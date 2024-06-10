package com.itcube.Emelyanov;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class TCP extends AppCompatActivity {
    private static final int PORT = 52000;
    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    private String _ipAddress = "192.168.1.65";
    private int roomId = 2;
    private LinearLayout toggleButtonContainer;
    private LinearLayout buttonContainer;
    private List<ToggleButton> lights = new ArrayList<>();
    private TextView RoomId_text;
    private Switch L1, L2, L3;
    private boolean OutputUser = true;
    private Button BbuttonRoom, NbuttonRoom, ADDsetup;
    private RetrofitInterface retrofitInterface;
    private TextInputLayout NewNameSetup;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UIfinder();
        SetTExtRoomId();
        UIsetActivity();
        OnStartConnect();
        sendIP();
        QuestionRoom();
        readArduinoMessage();
        initRetrofit();
        fetchDataFromServer("fetch");
    }

    private void updateThumbTint(Switch aSwitch, boolean isChecked) {
        int thumbColor = isChecked ? Color.parseColor("#FF8E00") : Color.parseColor("#FFFFFF");
        aSwitch.setThumbTintList(ColorStateList.valueOf(thumbColor));
    }

    void UIsetActivity() {
        L1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sendCommandToArduino(L1, isChecked);
            updateThumbTint(L1, isChecked);
        });

        L2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sendCommandToArduino(L2, isChecked);
            updateThumbTint(L2, isChecked);
        });

        L3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sendCommandToArduino(L3, isChecked);
            updateThumbTint(L3, isChecked);
        });

        BbuttonRoom.setOnClickListener(v -> {
            if (roomId > 0) {
                roomId--;
                SetTExtRoomId();
                OutputUser = true;
                QuestionRoom();
            } else {
                Toast.makeText(TCP.this, "Room ID cannot be less than 0", Toast.LENGTH_SHORT).show();
            }
        });

        NbuttonRoom.setOnClickListener(v -> {
            if (roomId < 6) {
                roomId++;
                SetTExtRoomId();
                OutputUser = true;
                QuestionRoom();
            } else {
                Toast.makeText(TCP.this, "Room ID cannot be more than 6", Toast.LENGTH_SHORT).show();
            }
        });

        ADDsetup.setOnClickListener(v -> {
            if (NewNameSetup.getEditText() != null && !NewNameSetup.getEditText().getText().toString().isEmpty()) {
                addItemToServer(NewNameSetup.getEditText().getText().toString(), roomId,
                        (L1.isChecked() ? "1" : "0") + (L2.isChecked() ? "1" : "0") + (L3.isChecked() ? "1" : "0"));
                NewNameSetup.getEditText().setText("");
                buttonContainer.removeAllViews();
                fetchDataFromServer("fetch");
            } else {
                Toast.makeText(TCP.this, "Please enter a valid name.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendCommandToArduino(Switch lightSwitch, boolean isChecked) {
        new Thread(() -> {
            try {
                String binaryLightsState = (L1.isChecked() ? "1" : "0") + (L2.isChecked() ? "1" : "0") + (L3.isChecked() ? "1" : "0");
                int decimalValue = Integer.parseInt(binaryLightsState, 2);
                String message = "!L" + roomId + ":" + decimalValue + ";";
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, PORT);
                udpSocket.send(packet);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(TCP.this, "Error sending command to Arduino: " + e.getMessage(), Toast.LENGTH_LONG).show());
                Log.e("UDP", "Error sending command to Arduino", e);
            }
        }).start();
    }

    void OnStartConnect() {
        try {
            serverAddress = InetAddress.getByName(_ipAddress);
        } catch (UnknownHostException e) {
            Toast.makeText(this, "Error resolving host: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("UDP", "Error resolving host", e);
        }
        try {
            udpSocket = new DatagramSocket(); // Создаем сокет без привязки к порту
            udpSocket.setBroadcast(true);
        } catch (IOException e) {
            Toast.makeText(this, "Error creating socket: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("UDP", "Error creating socket", e);
        }
    }

    void UIfinder() {
        RoomId_text = findViewById(R.id.text_roomid);
        BbuttonRoom = findViewById(R.id.back_button);
        NbuttonRoom = findViewById(R.id.next_button);
        buttonContainer = findViewById(R.id.buttonContainer);  // Добавляем контейнер для кнопок
        L1 = findViewById(R.id.Light1);
        L2 = findViewById(R.id.Light2);
        L3 = findViewById(R.id.Light3);
        ADDsetup = findViewById(R.id.AddSetup);
        NewNameSetup = findViewById(R.id.NameNewSetup);
    }

    void SetTExtRoomId() {
        RoomId_text.setText("Room: " + roomId);
    }

    private void sendIP() {
        new Thread(() -> {
            try {
                String ipAddress = "192.168.1.255"; // Измените на нужный IP-адрес
                InetAddress address = InetAddress.getByName(ipAddress);
                byte[] data = "?IP".getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, address, PORT);
                udpSocket.send(packet);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(TCP.this, "Error sending IP: " + e.getMessage(), Toast.LENGTH_LONG).show());
                Log.e("UDP", "Error sending IP", e);
            }
        }).start();
    }



    private void QuestionRoom() {
        new Thread(() -> {
            try {
                InetAddress serverAddress = InetAddress.getByName(_ipAddress);
                String message = "?L" + roomId;
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, PORT);
                udpSocket.send(packet);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(TCP.this, "Error connecting to Arduino: " + e.getMessage(), Toast.LENGTH_LONG).show());
                Log.e("UDP", "Error connecting to Arduino", e);
            }
        }).start();
    }

    private void readArduinoMessage() {
        new Thread(() -> {
            try {
                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    String command = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (command.startsWith(">L:")) {
                        if (OutputUser) {
                            String[] parts = command.split(":")[1].split(";");
                            if (parts.length >= 2) {
                                int receivedRoomId = Integer.parseInt(parts[0]);
                                if (receivedRoomId == roomId) {
                                    int lightsState = Integer.parseInt(parts[1]);
                                    runOnUiThread(() -> updateLightsState(lightsState));
                                }
                            }
                            OutputUser = false;
                        }
                    }
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(TCP.this, "Error reading Arduino message: " + e.getMessage(), Toast.LENGTH_LONG).show());
                Log.e("UDP", "Error reading Arduino message", e);
            }
        }).start();
    }

    private void updateLightsState(int lightsState) {
        boolean light1On = (lightsState & 0b100) != 0;
        boolean light2On = (lightsState & 0b10) != 0;
        boolean light3On = (lightsState & 0b1) != 0;

        runOnUiThread(() -> {
            L1.setChecked(light1On);
            L2.setChecked(light2On);
            L3.setChecked(light3On);
        });
    }

    private void createInfoButton(int ID, String name, int roomNumber, String info) {
        // Create a horizontal LinearLayout to hold each button and delete button together
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER); // Center contents horizontally

        // Create the main button
        Button button = new Button(this);
        button.setText(name + " - Room:" + roomNumber);
        button.setTextColor(Color.WHITE);

        // Create a ColorStateList for the background tint of button (blue color)
        int[][] buttonStates = new int[][] {
                new int[] { android.R.attr.state_enabled }, // enabled
                new int[] { -android.R.attr.state_enabled }, // disabled
        };
        int[] buttonColors = new int[] {
                Color.parseColor("#24587A"), // tint color for enabled state
                Color.parseColor("#4CAF50"), // tint color for disabled state (example)
        };
        ColorStateList buttonColorStateList = new ColorStateList(buttonStates, buttonColors);

        // Apply the ColorStateList to set the background tint for button
        button.setBackgroundTintList(buttonColorStateList);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1); // Make button occupy equal width with deleteButton
        button.setLayoutParams(buttonParams);

        button.setOnClickListener(v -> {
            roomId = roomNumber;
            SetTExtRoomId();
            updateLightsState(Integer.parseInt(info));
            Toast.makeText(this, "Success", Toast.LENGTH_LONG).show();
        });

        // Create the delete button
        Button deleteButton = new Button(this);
        deleteButton.setText("Delete");
        deleteButton.setTextColor(Color.WHITE);

        // Create a ColorStateList for the background tint of deleteButton (red color)
        int[][] deleteButtonStates = new int[][] {
                new int[] { android.R.attr.state_enabled }, // enabled
                new int[] { -android.R.attr.state_enabled }, // disabled
        };
        int[] deleteButtonColors = new int[] {
                Color.parseColor("#b5351c"), // tint color for enabled state
                Color.parseColor("#FFCDD2"), // tint color for disabled state (example)
        };
        ColorStateList deleteButtonColorStateList = new ColorStateList(deleteButtonStates, deleteButtonColors);

        // Apply the ColorStateList to set the background tint for deleteButton
        deleteButton.setBackgroundTintList(deleteButtonColorStateList);

        LinearLayout.LayoutParams deleteButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0); // Make deleteButton occupy zero width initially
        deleteButton.setLayoutParams(deleteButtonParams);

        deleteButton.setOnClickListener(v -> {
            // Remove the button and its associated views from the container
            buttonContainer.removeView(layout);
            deleteItemFromServer(ID);
            // Optionally add additional logic here, e.g., deleting data associated with the button
            // (if any data structures are holding roomNumber, info, etc.)
        });

        // Measure the delete button's width based on its text content
        deleteButton.measure(0, 0);
        int deleteButtonWidth = deleteButton.getMeasuredWidth();

        // Update delete button's layout params to match button's width
        deleteButtonParams.width = deleteButtonWidth;
        deleteButton.setLayoutParams(deleteButtonParams);

        // Add both buttons to the layout
        layout.addView(button);
        layout.addView(deleteButton);

        // Set layout parameters for the parent layout
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layout.setLayoutParams(params);

        // Add the layout to the buttonContainer (assuming buttonContainer is a LinearLayout)
        buttonContainer.addView(layout);
    }







    // Method to create a new table via HTTP request


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (udpSocket != null) {
            udpSocket.close();
        }
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://gimnazevent.ru/Hakaton/") // Replace with your base URL
                .addConverterFactory(GsonConverterFactory.create()) // Use Gson converter
                .build();

        retrofitInterface = retrofit.create(RetrofitInterface.class);
    }


    private void deleteItemFromServer(int ID) {
        // Make an HTTP request to delete the item from the server
        Call<Void> call = retrofitInterface.deleteData("delete",ID);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TCP.this, "Item deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TCP.this, "Failed to delete item: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("Retrofit", "Failed to delete item: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TCP.this, "Failed to delete item: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("Retrofit", "Failed to delete item", t);
            }
        });
    }

    private void addItemToServer(String name, int room, String info) {
        // Make an HTTP request to add the new item to the server
        Call<Void> call = retrofitInterface.CreateData( "insert", name, room, info);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TCP.this, "Item added successfully", Toast.LENGTH_SHORT).show();

                    // Optionally, fetch updated data from the server after adding the item
                    fetchDataFromServer("fetch");
                } else {
                    Toast.makeText(TCP.this, "Failed to add item: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("Retrofit", "Failed to add item: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TCP.this, "Failed to add item: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("Retrofit", "Failed to add item", t);
            }
        });
    }


    private void fetchDataFromServer(String operation) {
        Call<RoomsResponse> call = retrofitInterface.fetchData(operation);
        call.enqueue(new Callback<RoomsResponse>() {
            @Override
            public void onResponse(Call<RoomsResponse> call, Response<RoomsResponse> response) {
                if (response.isSuccessful()) {
                    RoomsResponse roomsResponse = response.body();
                    if (roomsResponse != null) {
                        List<DataModel> data = roomsResponse.getSetupRooms();
                        if (data != null) {
                            // Обработка данных
                            buttonContainer.removeAllViews(); // Очистка существующих кнопок
                            for (DataModel item : data) {
                                createInfoButton(item.getID(), item.getName(), item.getRoom(), item.getInfo());
                            }
                        } else {
                            Toast.makeText(TCP.this, "Пустой массив SetupRooms", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(TCP.this, "Пустой тело ответа", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(TCP.this, "Не удалось получить данные: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("Retrofit", "Ошибка ответа: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<RoomsResponse> call, Throwable t) {
                Toast.makeText(TCP.this, "Ошибка получения данных: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("Retrofit", "Ошибка получения данных", t);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // QuestionRoom();
    }

    @Override
    protected void onResume() {
        super.onResume();
        OutputUser = true;
        QuestionRoom();
    }


}
