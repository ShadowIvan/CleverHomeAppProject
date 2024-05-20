package com.itcube.Emelyanov;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class LightSwitchWidget extends AppWidgetProvider {

    private static final String TAG = "LightSwitchWidget";
    private static final String ACTION_SWITCH_LIGHT1 = "com.itcube.Emelyanov.ACTION_SWITCH_LIGHT1";
    private static final String ACTION_SWITCH_LIGHT2 = "com.itcube.Emelyanov.ACTION_SWITCH_LIGHT2";
    private static final String ACTION_SWITCH_LIGHT3 = "com.itcube.Emelyanov.ACTION_SWITCH_LIGHT3";
    private static final int PORT = 52000;
    private static final String ARDUINO_IP = "192.168.1.216";
    private static final int ROOM_ID = 2;

    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    private Context context;

    private boolean light1On = false;
    private boolean light2On = false;
    private boolean light3On = false;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        this.context = context.getApplicationContext(); // сохраняем applicationContext

        try {
            udpSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName(ARDUINO_IP);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации UDP-сокета", e);
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        updateWidgetViews(views);

        sendQuestionToArduino();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_SWITCH_LIGHT1.equals(intent.getAction())) {
            toggleLight1();
        } else if (ACTION_SWITCH_LIGHT2.equals(intent.getAction())) {
            toggleLight2();
        } else if (ACTION_SWITCH_LIGHT3.equals(intent.getAction())) {
            toggleLight3();
        }
    }

    private void toggleLight1() {
        light1On = !light1On;
        updateSwitchState(R.id.Light1w, light1On);
        sendCommandToArduino();
    }

    private void toggleLight2() {
        light2On = !light2On;
        updateSwitchState(R.id.Light2w, light2On);
        sendCommandToArduino();
    }

    private void toggleLight3() {
        light3On = !light3On;
        updateSwitchState(R.id.Light3w, light3On);
        sendCommandToArduino();
    }

    private void updateSwitchState(int viewId, boolean isOn) {
        if (context == null) {
            Log.e(TAG, "Context равен null в updateSwitchState. Виджет возможно не был правильно инициализирован.");
            return;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        int imageResId = isOn ? R.drawable.ic_light_on : R.drawable.ic_light_off;
        views.setImageViewResource(viewId, imageResId);

        updateWidgetViews(views);
    }

    private void sendCommandToArduino() {
        new Thread(() -> {
            try {
                if (serverAddress == null) {
                    serverAddress = InetAddress.getByName(ARDUINO_IP);
                }

                String binaryLightsState = "";
                binaryLightsState += (light3On ? "1" : "0");
                binaryLightsState += (light2On ? "1" : "0");
                binaryLightsState += (light1On ? "1" : "0");

                int decimalValue = Integer.parseInt(binaryLightsState, 2);

                String message = "!L" + ROOM_ID + ":" + decimalValue + ";";
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, PORT);

                if (udpSocket == null) {
                    udpSocket = new DatagramSocket();
                }

                udpSocket.send(packet);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка отправки команды на Arduino", e);
            }
        }).start();
    }

    private void sendQuestionToArduino() {
        new Thread(() -> {
            try {
                if (serverAddress == null) {
                    serverAddress = InetAddress.getByName(ARDUINO_IP);
                }

                String message = "?L" + ROOM_ID;
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, PORT);

                if (udpSocket == null) {
                    udpSocket = new DatagramSocket();
                }

                udpSocket.send(packet);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Неизвестный хост: " + e.getMessage(), e);
                showToast("Ошибка подключения к Arduino: Неизвестный хост");
            } catch (SocketException e) {
                Log.e(TAG, "Ошибка сокета: " + e.getMessage(), e);
                showToast("Ошибка подключения к Arduino: Ошибка сокета");
            } catch (IOException e) {
                Log.e(TAG, "Ошибка ввода-вывода: " + e.getMessage(), e);
                showToast("Ошибка подключения к Arduino: Ошибка ввода-вывода");
            }
        }).start();
    }

    private void updateWidgetViews(RemoteViews views) {
        setupSwitchAction(views, R.id.Light1w, ACTION_SWITCH_LIGHT1);
        setupSwitchAction(views, R.id.Light2w, ACTION_SWITCH_LIGHT2);
        setupSwitchAction(views, R.id.Light3w, ACTION_SWITCH_LIGHT3);

        pushWidgetUpdate(views);
    }

    private void setupSwitchAction(RemoteViews views, int viewId, String action) {
        Intent intent = new Intent(this.context, LightSwitchWidget.class);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(viewId, pendingIntent);
    }

    private void pushWidgetUpdate(RemoteViews views) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.context);
        ComponentName componentName = new ComponentName(this.context, LightSwitchWidget.class);
        appWidgetManager.updateAppWidget(componentName, views);
    }

    private void showToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        } else {
            Log.e(TAG, "Context равен null при попытке показа Toast");
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (udpSocket != null) {
            udpSocket.close();
        }
    }
}
