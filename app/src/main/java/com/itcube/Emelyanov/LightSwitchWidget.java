package com.itcube.Emelyanov;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class LightSwitchWidget extends AppWidgetProvider {

    private static final String ACTION_LIGHT_CLICK = "com.itcube.Emelyanov.LIGHT_CLICK";
    private static final int PORT = 52000;
    private static InetAddress serverAddress;
    private static DatagramSocket udpSocket;

    static {
        try {
            serverAddress = InetAddress.getByName("192.168.1.65");
            udpSocket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            Intent intent1 = new Intent(context, LightSwitchWidget.class);
            intent1.setAction(ACTION_LIGHT_CLICK);
            intent1.putExtra("light_id", R.id.Light1w);
            intent1.setData(Uri.parse(intent1.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent pendingIntent1 = PendingIntent.getBroadcast(context, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.Light1w, pendingIntent1);

            Intent intent2 = new Intent(context, LightSwitchWidget.class);
            intent2.setAction(ACTION_LIGHT_CLICK);
            intent2.putExtra("light_id", R.id.Light2w);
            intent2.setData(Uri.parse(intent2.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context, 1, intent2, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.Light2w, pendingIntent2);

            Intent intent3 = new Intent(context, LightSwitchWidget.class);
            intent3.setAction(ACTION_LIGHT_CLICK);
            intent3.putExtra("light_id", R.id.Light3w);
            intent3.setData(Uri.parse(intent3.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent pendingIntent3 = PendingIntent.getBroadcast(context, 2, intent3, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.Light3w, pendingIntent3);

            updateViewStates(context, views);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_LIGHT_CLICK.equals(intent.getAction())) {
            int lightId = intent.getIntExtra("light_id", 0);
            if (lightId != 0) {
                SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
                boolean isOn = prefs.getBoolean("is_on_" + lightId, false);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_on_" + lightId, !isOn);
                editor.apply();

                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                updateViewStates(context, views);

                AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, LightSwitchWidget.class), views);

                sendCommandToArduino(context);
            }
        }
    }

    private void updateViewStates(Context context, RemoteViews views) {
        SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);

        boolean isLight1On = prefs.getBoolean("is_on_" + R.id.Light1w, false);
        views.setImageViewResource(R.id.Light1w, isLight1On ? R.drawable.ic_light_on : R.drawable.ic_light_off);

        boolean isLight2On = prefs.getBoolean("is_on_" + R.id.Light2w, false);
        views.setImageViewResource(R.id.Light2w, isLight2On ? R.drawable.ic_light_on : R.drawable.ic_light_off);

        boolean isLight3On = prefs.getBoolean("is_on_" + R.id.Light3w, false);
        views.setImageViewResource(R.id.Light3w, isLight3On ? R.drawable.ic_light_on : R.drawable.ic_light_off);
    }

    private void sendCommandToArduino(Context context) {
        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);

                boolean isLight1On = prefs.getBoolean("is_on_" + R.id.Light1w, false);
                boolean isLight2On = prefs.getBoolean("is_on_" + R.id.Light2w, false);
                boolean isLight3On = prefs.getBoolean("is_on_" + R.id.Light3w, false);

                String binaryLightsState = (isLight1On ? "1" : "0") +
                        (isLight2On ? "1" : "0") +
                        (isLight3On ? "1" : "0");

                int decimalValue = Integer.parseInt(binaryLightsState, 2);
                String message = "!L2:" + decimalValue + ";";
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, PORT);
                udpSocket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (udpSocket != null) {
            udpSocket.close();
        }
    }
}
