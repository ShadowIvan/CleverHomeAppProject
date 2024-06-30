package com.itcube.Emelyanov;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class LightSwitchWidget extends AppWidgetProvider {

    public static final String ACTION_SEND_COMMAND_TO_ARDUINO_Q = "com.itcube.Emelyanov.SEND_COMMAND_TO_ARDUINO_Q";
    private static final String ACTION_LIGHT_CLICK = "com.itcube.Emelyanov.LIGHT_CLICK";
    private static final String ACTION_UPDATE_WIDGET = "com.itcube.Emelyanov.UPDATE_WIDGET";
    private static final String ACTION_OPEN_ACTIVITY = "com.itcube.Emelyanov.OPEN_ACTIVITY";

    private static InetAddress serverAddress;
    private final Handler handler = new Handler();
    /**
     * создание переодичной получении данных
     */
    private Runnable createPeriodicTask(Context context) {
        return new Runnable() {
            @Override
            public void run() {
                sendCommandToArduinoQ(context, LightSwitchWidget.this);
                SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
                int backgroundResId = "on".equals(prefs.getString("ConnectingArduino", "off")) ?
                        R.drawable.background_widget : R.drawable.background_widget_off;
                updateWidgetBackground(context, backgroundResId);
                //handler.postDelayed(this, 8000); // 8 секунд интервал
            }
        };
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            setupClickIntents(context, views);
            updateViewStates(context, views);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        setupPeriodicUpdate(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
        Log.d("LightSwitchWidget", "Action received: " + action);

        try {
            if (ACTION_LIGHT_CLICK.equals(action)) {
                int lightId = intent.getIntExtra("light_id", 0);
                Log.d("LightSwitchWidget", "Light ID clicked: " + lightId);
                if (lightId != 0) {
                    toggleLightState(context, lightId);
                }
            } else if (ACTION_OPEN_ACTIVITY.equals(action)) {
                Log.d("LightSwitchWidget", "Open activity action received");
                Intent openActivityIntent = new Intent(context, ChangeRoomIdActivity.class);
                openActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(openActivityIntent);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_on_" + R.id.Light1w, false);
                editor.putBoolean("is_on_" + R.id.Light2w, false);
                editor.putBoolean("is_on_" + R.id.Light3w, false);
                editor.apply();
                sendCommandToArduinoQ(context, this); // Отправляем команду
            } else if (ACTION_UPDATE_WIDGET.equals(action)) {
                updateAllWidgets(context);
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(createPeriodicTask(context), 8000);
            } else if (ACTION_SEND_COMMAND_TO_ARDUINO_Q.equals(action)) {
                sendCommandToArduinoQ(context, this); // Отправляем команду
            }
        } catch (Exception e) {
            Log.e("LightSwitchWidget", "Error handling broadcast action", e);
        }
    }

    private void setupClickIntents(Context context, RemoteViews views) {
        setupLightClickIntent(context, views, R.id.Light1w);
        setupLightClickIntent(context, views, R.id.Light2w);
        setupLightClickIntent(context, views, R.id.Light3w);
        setupOpenActivityIntent(context, views);
    }

    private void setupLightClickIntent(Context context, RemoteViews views, int lightId) {
        Intent intent = new Intent(context, LightSwitchWidget.class);
        intent.setAction(ACTION_LIGHT_CLICK);
        intent.putExtra("light_id", lightId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, lightId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(lightId, pendingIntent);
    }

    private void setupOpenActivityIntent(Context context, RemoteViews views) {
        Intent openActivityIntent = new Intent(context, ChangeRoomIdActivity.class);
        openActivityIntent.setAction(ACTION_OPEN_ACTIVITY);
        PendingIntent openActivityPendingIntent = PendingIntent.getActivity(context, 3, openActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.text_roomid, openActivityPendingIntent);
    }

    private void updateViewStates(Context context, RemoteViews views) {
        SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
        String roomId = prefs.getString("room_id", "2");
        views.setTextViewText(R.id.text_roomid, "Room: " + roomId);
        updateLightState(context, views, R.id.Light1w);
        updateLightState(context, views, R.id.Light2w);
        updateLightState(context, views, R.id.Light3w);
    }

    private static void updateLightState(Context context, RemoteViews views, int lightId) {
        SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
        boolean isOn = prefs.getBoolean("is_on_" + lightId, false);
        views.setImageViewResource(lightId, isOn ? R.drawable.ic_light_on : R.drawable.ic_light_off);
    }

    private void toggleLightState(Context context, int lightId) {
        SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
        boolean isOn = prefs.getBoolean("is_on_" + lightId, false);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_on_" + lightId, !isOn);
        editor.apply();
        updateAllWidgets(context);
        sendCommandToArduino(context);
    }
    /**
     * Обновить объекты
     */
    private void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, LightSwitchWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            updateViewStates(context, views);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    /**
     * Отправка команды для ардуино
     */
    private void sendCommandToArduino(Context context) {
        new Thread(() -> {
            DatagramSocket socket = null;
            try {
                SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
                String ipAddress = prefs.getString("IP_ADDRESS", "192.168.1.216");
                int port = prefs.getInt("PORT", 52000);

                serverAddress = InetAddress.getByName(ipAddress);

                boolean isLight1On = prefs.getBoolean("is_on_" + R.id.Light1w, false);
                boolean isLight2On = prefs.getBoolean("is_on_" + R.id.Light2w, false);
                boolean isLight3On = prefs.getBoolean("is_on_" + R.id.Light3w, false);

                String binaryLightsState = (isLight1On ? "1" : "0") +
                        (isLight2On ? "1" : "0") +
                        (isLight3On ? "1" : "0");

                int decimalValue = Integer.parseInt(binaryLightsState, 2);
                String message = "!L" + prefs.getString("room_id", "2") + ":" + decimalValue + ";";
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, port);

                socket = new DatagramSocket();
                socket.send(packet);

                // Запуск прослушивания после отправки команды
                // ListeningForResponses(context);
            } catch (NumberFormatException e) {
                Log.e("LightSwitchWidget", "Invalid port number format", e);
                showToastOnMainThread(context, "Invalid port number");
            } catch (UnknownHostException e) {
                Log.e("LightSwitchWidget", "Unknown host exception", e);
                showToastOnMainThread(context, "Unknown host");
            } catch (Exception e) {
                Log.e("LightSwitchWidget", "Failed to send command to Arduino", e);
                showToastOnMainThread(context, "Failed to send command to Arduino");
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }).start();
    }
    /**
     * Запрос на ардуино
     */
    public static void sendCommandToArduinoQ(Context context, LightSwitchWidget widget) {
        new Thread(() -> {
            DatagramSocket socket = null;
            try {
                SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
                String ipAddress = prefs.getString("IP_ADDRESS", "192.168.1.216");
                int port = prefs.getInt("PORT", 52000);

                String message = "?L" + prefs.getString("room_id", "2") + ";";
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(ipAddress), port);

                socket = new DatagramSocket();
                socket.send(packet);

                // Запуск прослушивания после отправки команды
                ListeningForResponses(context, socket);
            } catch (NumberFormatException e) {
                Log.e("LightSwitchWidget", "Invalid port number format", e);
                showToastOnMainThread(context, "Invalid port number");
            } catch (UnknownHostException e) {
                Log.e("LightSwitchWidget", "Unknown host exception", e);
                showToastOnMainThread(context, "Unknown host");
            } catch (Exception e) {
                Log.e("LightSwitchWidget", "Failed to send command to Arduino Q", e);
                showToastOnMainThread(context, "Failed to send command to Arduino Q");
            }
        }).start();
    }
    /**
     * Выставление переодичности
     */
    private void setupPeriodicUpdate(Context context) {
        Intent intent = new Intent(context, LightSwitchWidget.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 8000, pendingIntent);
        }
    }
    /**
     * Прослушка информации с ардуино
     */
    private static void ListeningForResponses(Context context, DatagramSocket receiveSocket) {
        new Thread(() -> {
            SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
            try {
                // Set timeout for receiving data
                receiveSocket.setSoTimeout(1000); // 3 seconds timeout

                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    // Attempt to receive data
                    receiveSocket.receive(packet);
                    String receivedData = new String(packet.getData(), 0, packet.getLength());
                    Log.d("LightSwitchWidget", "Received data: " + receivedData);
                    prefs.edit().putString("ConnectingArduino", "on").apply();
                    // Process received data
                    processReceivedData(context, receivedData);

                } catch (java.net.SocketException e) {
                    // Handle the case when no data is received within the timeout period
                    prefs.edit().putString("ConnectingArduino", "off").apply();
                    Log.e("LightSwitchWidget", "No data received within the timeout period (3 seconds).", e);

                } catch (Exception e) {
                    // Handle other exceptions
                    prefs.edit().putString("ConnectingArduino", "off").apply();
                    Log.e("LightSwitchWidget", "Error initializing or receiving data", e);
                }

            } catch (SocketException e) {
                // Handle socket exceptions
                prefs.edit().putString("ConnectingArduino", "off").apply();
                Log.e("LightSwitchWidget", "Socket error occurred", e);

            } finally {
                // Ensure the socket is closed properly
                if (receiveSocket != null && !receiveSocket.isClosed()) {
                    receiveSocket.close();
                }
            }
        }).start();
    }
    /**
     * Обработка информации от ардуино
     */
    private static void processReceivedData(Context context, String data) {
        // Пример строки: ">L:2;5;0;0;0;0;0;0;"
        SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
        try {
            // Убираем начальный символ и разделяем строку
            String trimmedData = data.substring(1); // Убираем ">"
            String[] parts = trimmedData.split(";");

            if (parts.length < 2) {
                Log.e("LightSwitchWidget", "Invalid data format");
                return;
            }

            // Номер комнаты
            String roomNumber = parts[0].split(":")[1];
            int receivedRoomNumber = Integer.parseInt(roomNumber);

            // Статус светильников
            int lightStatusDecimal = Integer.parseInt(parts[1]);

            // Преобразование в 3-битное бинарное представление
            // Получаем последние 3 бита, используя побитовую операцию AND
            int lastThreeBits = lightStatusDecimal & 0b111; // 0b111 это 7 в двоичной системе
            String lightStatusBinary = String.format("%03d", Integer.parseInt(Integer.toBinaryString(lastThreeBits)));

            // Обновляем виджет, если номер комнаты совпадает
            int widgetRoomNumber = Integer.parseInt(prefs.getString("room_id", "2"));

            if (receivedRoomNumber == widgetRoomNumber) {
                updateWidgetWithLightStatus(context, lightStatusBinary);
            }
        } catch (Exception e) {
            Log.e("LightSwitchWidget", "Error processing received data", e);
        }
    }


    /**
     * Обновление виджета света
     */
    private static void updateWidgetWithLightStatus(Context context, String lightStatusBinary) {
        // Преобразуем бинарную строку в статус для каждого светильника
        boolean[] lightStates = new boolean[8];
        for (int i = 0; i < lightStatusBinary.length(); i++) {
            lightStates[i] = lightStatusBinary.charAt(i) == '1';
        }

        // Обновляем виджет
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, LightSwitchWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // Обновляем изображение для каждого светильника
            views.setImageViewResource(R.id.Light1w, lightStates[0] ? R.drawable.ic_light_on : R.drawable.ic_light_off);
            views.setImageViewResource(R.id.Light2w, lightStates[1] ? R.drawable.ic_light_on : R.drawable.ic_light_off);
            views.setImageViewResource(R.id.Light3w, lightStates[2] ? R.drawable.ic_light_on : R.drawable.ic_light_off);
            // Если есть больше светильников, добавьте их аналогичным образом

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    /**
     * Показать toast
     */
    private static void showToastOnMainThread(Context context, String message) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * Обновление фона по статусу подключения к ардуино
     */
    private void updateWidgetBackground(Context context, int backgroundResId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, LightSwitchWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setInt(R.id.widget, "setBackgroundResource", backgroundResId);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
