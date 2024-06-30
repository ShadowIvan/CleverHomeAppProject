package com.itcube.Emelyanov;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChangeRoomIdActivity extends AppCompatActivity {

    private TextView TextRoomId;
    private Button saveButton, BbuttonRoom, NbuttonRoom;;

    private int roomId = 2;
    /**
     * инциализация
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_room_layout);
        TextRoomId = findViewById(R.id.text_roomid);
        saveButton = findViewById(R.id.save_button);
        BbuttonRoom = findViewById(R.id.back_button);
        NbuttonRoom = findViewById(R.id.next_button);
        SharedPreferences prefs = this.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
        roomId = Integer.parseInt(prefs.getString("room_id", "2"));
        SetTExtRoomId();

        saveButton.setOnClickListener(v -> {
            saveRoomId(String.valueOf(roomId));
        });

        BbuttonRoom.setOnClickListener(v -> {
            if (roomId > 0) {
                roomId--;
                SetTExtRoomId();
            } else {
                Toast.makeText(ChangeRoomIdActivity.this, "Room ID cannot be less than 0", Toast.LENGTH_SHORT).show();
            }
        });

        NbuttonRoom.setOnClickListener(v -> {
            if (roomId < 6) {
                roomId++;
                SetTExtRoomId();
            } else {
                Toast.makeText(ChangeRoomIdActivity.this, "Room ID cannot be more than 6", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * выставление текста
     */
    void SetTExtRoomId() {
        TextRoomId.setText("Room: " + roomId);
    }
    /**
     * сохранение id комнаты для виджета
     */
    private void saveRoomId(String roomId) {
        SharedPreferences prefs = getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("room_id", roomId);
        editor.apply();

        Intent intent = new Intent(this, LightSwitchWidget.class);
        intent.setAction(LightSwitchWidget.ACTION_SEND_COMMAND_TO_ARDUINO_Q);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }

        // Оповещение виджетов об изменениях
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, LightSwitchWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        // Обновление всех виджетов
        Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(updateIntent);

        finish();
    }
}
