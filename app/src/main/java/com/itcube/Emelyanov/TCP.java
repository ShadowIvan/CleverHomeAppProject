package com.itcube.Emelyanov;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TCP extends AppCompatActivity {

    private static int PORT_DEFAULT = 52000;
    private DatagramSocket udpSocket;
    private InetAddress ipAddress;
    private int roomId = 2;
    private RecyclerView recyclerView;
    private List<DataModel> dataModelList = new ArrayList<>();
    private RoomAdapter roomAdapter;
    public TextView roomIdTextView;
    public Switch L1;
    public Switch L2;
    public Switch L3;
    private Button backButton, nextButton, addButton, logoutButton;
    private TextInputLayout newNameSetupLayout;
    private EditText ipAddressEditText, portEditText;
    private SharedPreferences prefs;
    public RetrofitInterface retrofitInterface;
    public UserRequest user_data;

    private static final String ACTION_LIGHT_CLICK = "com.itcube.Emelyanov.LIGHT_CLICK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("LightSwitchWidgetPrefs", MODE_PRIVATE);
        findViews();
        setupUI(this);
        loadSavedData();
        initRetrofit();
        attemptAutoLogin();
        initializeConnection();  // Ensure connection is initialized after loading saved data
        sendCommandToArduinoQ(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendCommandToArduinoQ(this);
    }


    private void findViews() {
        L1 = findViewById(R.id.Light1);
        L2 = findViewById(R.id.Light2);
        L3 = findViewById(R.id.Light3);
        backButton = findViewById(R.id.back_button);
        nextButton = findViewById(R.id.next_button);
        roomIdTextView = findViewById(R.id.text_roomid);
        portEditText = findViewById(R.id.port);
        ipAddressEditText = findViewById(R.id.ip_address);
        addButton = findViewById(R.id.AddSetup);
        newNameSetupLayout = findViewById(R.id.NameNewSetup);
        recyclerView = findViewById(R.id.recyclerView);
        logoutButton = findViewById(R.id.logout_button);
    }

    private void setupUI(Context context) {
        roomIdTextView.setText("Комната №" + roomId);

        L1.setOnClickListener((buttonView) -> {
            sendCommandToArduino();
            updateThumbTint(L1, L1.isChecked());
        });

        L2.setOnClickListener((buttonView) -> {
            sendCommandToArduino();
            updateThumbTint(L2, L2.isChecked());
        });

        L3.setOnClickListener((buttonView) -> {
            sendCommandToArduino();
            updateThumbTint(L3, L3.isChecked());
        });

        backButton.setOnClickListener(v -> {
            if (roomId > 0) {
                roomId--;
                sendCommandToArduinoQ(context);
                roomIdTextView.setText("Комната №" + roomId);
            } else {
                Toast.makeText(TCP.this, "Room ID cannot be less than 0", Toast.LENGTH_SHORT).show();
            }
        });

        nextButton.setOnClickListener(v -> {
            if (roomId < 6) {
                roomId++;
                sendCommandToArduinoQ(context);
                roomIdTextView.setText("Комната №" + roomId);
            } else {
                Toast.makeText(TCP.this, "Room ID cannot be more than 6", Toast.LENGTH_SHORT).show();
            }
        });

        addButton.setOnClickListener(v -> {
            String newName = newNameSetupLayout.getEditText().getText().toString().trim();
            if (newName.length() >= 3 && newName.length() <= 30) {
                addItemToServer(newName, roomId,
                        (L1.isChecked() ? "1" : "0") + (L2.isChecked() ? "1" : "0") + (L3.isChecked() ? "1" : "0"));
                newNameSetupLayout.getEditText().setText("");
                fetchDataFromServer(user_data.getUserId());
            } else {
                newNameSetupLayout.setError("Name must be between 3 and 30 characters");
            }
        });

        ipAddressEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});
        /*ipAddressEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveIPAddress();
            }
        });*/

        portEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
      /*  portEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                savePort();
            }
        });*/

        logoutButton.setOnClickListener( v ->
        {
            prefs.edit().remove("password").apply();
            showLoginDialog();
        });

        portEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int portValue = Integer.parseInt(s.toString());
                    if (portValue < 0 || portValue > 65535) {
                        portEditText.setError("Invalid port number");
                    }else {
                        savePort();
                    sendCommandToArduinoQ(context);
                    }
                } catch (NumberFormatException e) {
                    portEditText.setError("Invalid port number");
                }
            }
        });

        ipAddressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String ip = s.toString();
                if (!ip.matches("^\\d{1,3}(\\.\\d{1,3}){0,3}$")) {
                    ipAddressEditText.setError("Invalid IP address");
                }else {
                    saveIPAddress();
                sendCommandToArduinoQ(context);
                }
            }
        });
    }

    private void attemptAutoLogin() {
        String username = prefs.getString("username", null);
        String password = prefs.getString("password", null);

        if (username != null && password != null) {
            authenticateUser(username, password, null);
        } else {
            showLoginDialog();
        }
    }

    public void updateThumbTint(Switch _switch, boolean isChecked) {
        int thumbColor = isChecked ? Color.parseColor("#FF8E00") : Color.parseColor("#FFFFFF");
        _switch.setThumbTintList(ColorStateList.valueOf(thumbColor));
    }

    public void sendCommandToArduino() {
        new Thread(() -> {
            try {
                synchronized (this) {
                    if (udpSocket == null || ipAddress == null) {
                        runOnUiThread(() -> Toast.makeText(TCP.this, "UDP socket is not initialized", Toast.LENGTH_LONG).show());
                        return;
                    }

                    String binaryLightsState = (L1.isChecked() ? "1" : "0") + (L2.isChecked() ? "1" : "0") + (L3.isChecked() ? "1" : "0");
                    int decimalValue = Integer.parseInt(binaryLightsState, 2);
                    String message = "!L" + roomId + ":" + decimalValue + ";";
                    byte[] data = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, PORT_DEFAULT);
                    udpSocket.send(packet);
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(TCP.this, "Error sending command to Arduino: " + e.getMessage(), Toast.LENGTH_LONG).show());
                Log.e("UDP", "Error sending command to Arduino", e);
            }
        }).start();
    }

    private void saveIPAddress() {
        String ip = ipAddressEditText.getText().toString();
        if (ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("IP_ADDRESS", ip);
            editor.apply();
            Toast.makeText(this, "IP address saved", Toast.LENGTH_SHORT).show();
            initializeConnection();
        } else {
            ipAddressEditText.setError("Invalid IP address");
        }
    }

    private void savePort() {
        String portStr = portEditText.getText().toString();
        try {
            int portValue = Integer.parseInt(portStr);
            if (portValue >= 0 && portValue <= 65535) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("PORT", portValue);
                editor.apply();
                Toast.makeText(this, "Port saved", Toast.LENGTH_SHORT).show();
                initializeConnection();
            } else {
                portEditText.setError("Invalid port number");
            }
        } catch (NumberFormatException e) {
            portEditText.setError("Invalid port number");
        }
    }

    private void initializeConnection() {
        try {
            synchronized (this) {
                if (udpSocket != null && !udpSocket.isClosed()) {
                    udpSocket.close();
                }
                udpSocket = new DatagramSocket();
                String ip = ipAddressEditText.getText().toString();
                ipAddress = InetAddress.getByName(ip);
                int port = Integer.parseInt(portEditText.getText().toString());
                if (port > 0 && port <= 65535) {
                    PORT_DEFAULT = port;
                }
            }
        } catch (NumberFormatException | IOException e) {
            Toast.makeText(this, "Connection error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("UDP", "Error initializing connection", e);
        }
    }


    public void fetchDataFromServer(String id_user) {
        Call<List<DataModel>> call = retrofitInterface.fetchData(Integer.parseInt(id_user));
        call.enqueue(new Callback<List<DataModel>>() {
            @Override
            public void onResponse(Call<List<DataModel>> call, Response<List<DataModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    dataModelList.clear();
                    dataModelList.addAll(response.body());

                    if (roomAdapter == null) {
                        // Initialize the adapter and set it to RecyclerView
                        roomAdapter = new RoomAdapter(dataModelList, TCP.this, TCP.this);
                        recyclerView.setAdapter(roomAdapter);
                        recyclerView.setLayoutManager(new LinearLayoutManager(TCP.this));
                    } else {
                        // Notify the existing adapter that data has changed
                        roomAdapter.notifyDataSetChanged();
                    }
                } else {
                    Toast.makeText(TCP.this, "Data not found", Toast.LENGTH_SHORT).show();
                    Log.e("FETCH_DATA", "Response failed or no data found");
                }
            }

            @Override
            public void onFailure(Call<List<DataModel>> call, Throwable t) {
                Toast.makeText(TCP.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                Log.e("FETCH_DATA", "Request failed", t);
            }
        });
    }


    private void addItemToServer(String name, int roomId, String setup) {
        DataModel newItem = new DataModel(-1, name, roomId, setup);
        Call<Void> call = retrofitInterface.createData(newItem, Integer.parseInt(user_data.getUserId()));
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.e("ADD_ITEM", response.message());
                    Toast.makeText(TCP.this, "Data added successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TCP.this, "Failed to add data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TCP.this, "Request failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void sendCommandToArduinoQ(Context context) {
        if (isRunning) {
            Log.w("TCP", "Существует активный поток обработки данных.");
            return;
        }
        isRunning = true;
        executorService.submit(() -> {
            DatagramSocket socket = null;
            try {
                SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
                String ipAddress = prefs.getString("IP_ADDRESS", "192.168.1.216");
                int port = prefs.getInt("PORT", 52000);

                String message = "?L" + roomId + ";";
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(ipAddress), port);

                socket = new DatagramSocket();
                socket.send(packet);

                // Запуск прослушивания после отправки команды
                ListeningForResponses(context, socket);
            } catch (NumberFormatException e) {
                Log.e("TCP", "Invalid port number format", e);
            } catch (UnknownHostException e) {
                Log.e("TCP", "Unknown host exception", e);
            } catch (Exception e) {
                Log.e("TCP", "Failed to send command to Arduino Q", e);
            }finally {
                isRunning = false;
            }
        });
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_auth, null);
        builder.setView(dialogView);

        EditText usernameEditText = dialogView.findViewById(R.id.username);
        EditText passwordEditText = dialogView.findViewById(R.id.password);
        Button loginButton = dialogView.findViewById(R.id.login_button);
        Button registerButton = dialogView.findViewById(R.id.register_button);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            if (validateLoginInput(username, password)) {
                authenticateUser(username, password, dialog);
            }
        });

        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            if (validateRegisterInput(username, password)) {
                registerUser(username, password, dialog);
            }
        });
        dialog.show();
    }

    private boolean validateLoginInput(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username and Password cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validateRegisterInput(String username, String password) {
        if (username.length() < 3 || password.length() < 6) {
            Toast.makeText(this, "Username must be at least 3 characters and Password at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean isRunning = false;

    private void ListeningForResponses(Context context, DatagramSocket receiveSocket) {
        new Thread(() -> {
            SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                receiveSocket.setSoTimeout(1000);
                //while (!Thread.currentThread().isInterrupted()) {
                    try {
                        receiveSocket.receive(packet);
                        String receivedData = new String(packet.getData(), 0, packet.getLength());
                        Log.d("TCP", "Получены данные: " + receivedData);

                        // Обновите SharedPreferences и UI на главном потоке
                        runOnUiThread(() -> {
                            prefs.edit().putString("ConnectingArduino", "on").apply();
                            SetIpStatus(true);
                        });

                        processReceivedData(context, receivedData);

                    } catch (SocketTimeoutException e) {
                        Log.e("TCP", "Тайм-аут: данные не получены в течение периода ожидания.", e);
                        runOnUiThread(() -> {
                            prefs.edit().putString("ConnectingArduino", "off").apply();
                            SetIpStatus(false);
                            Toast.makeText(context, "Тайм-аут: данные от Arduino не получены", Toast.LENGTH_LONG).show();
                        });

                    } catch (IOException e) {
                        Log.e("TCP", "Ошибка ввода/вывода во время получения данных", e);
                        runOnUiThread(() -> {
                            prefs.edit().putString("ConnectingArduino", "off").apply();
                            SetIpStatus(false);
                            Toast.makeText(context, "Ошибка ввода/вывода во время получения данных", Toast.LENGTH_LONG).show();
                        });

                    } catch (Exception e) {
                        Log.e("TCP", "Неожиданная ошибка", e);
                        runOnUiThread(() -> {
                            prefs.edit().putString("ConnectingArduino", "off").apply();
                            SetIpStatus(false);
                            Toast.makeText(context, "Произошла неожиданная ошибка", Toast.LENGTH_LONG).show();
                        });
                    }
               //}

            } catch (SocketException e) {
                prefs.edit().putString("ConnectingArduino", "off").apply();
                SetIpStatus(false);
                throw new RuntimeException(e);
            } finally {
                if (receiveSocket != null && !receiveSocket.isClosed()) {
                    receiveSocket.close();
                }
            }
        }).start();
    }










    private void processReceivedData(Context context, String data) {
        // Example string: ">L:2;5;0;0;0;0;0;0;"
        SharedPreferences prefs = context.getSharedPreferences("LightSwitchWidgetPrefs", Context.MODE_PRIVATE);
        try {
            // Process data
            String trimmedData = data.substring(1); // Remove ">"
            String[] parts = trimmedData.split(";");

            if (parts.length < 2) {
                Log.e("TCP", "Invalid data format");
                return;
            }

            // Room number and light status
            String roomNumber = parts[0].split(":")[1];
            int receivedRoomNumber = Integer.parseInt(roomNumber);
            int lightStatusDecimal = Integer.parseInt(parts[1]);

            // Binary representation
            int lastThreeBits = lightStatusDecimal & 0b111;
            String lightStatusBinary = String.format("%03d", Integer.parseInt(Integer.toBinaryString(lastThreeBits)));


            if (receivedRoomNumber == roomId) {
                boolean _L1 = lightStatusBinary.charAt(0) == '1';
                boolean _L2 = lightStatusBinary.charAt(1) == '1';
                boolean _L3 = lightStatusBinary.charAt(2) == '1';

                // Update UI on main thread
                runOnUiThread(() -> {
                    updateThumbTint(L1, _L1);
                    L1.setChecked(_L1);
                    updateThumbTint(L2, _L2);
                    L2.setChecked(_L2);
                    updateThumbTint(L3, _L3);
                    L3.setChecked(_L3);
                });
            }
        } catch (Exception e) {
            Log.e("TCP", "Error processing received data", e);
        }
    }
    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://gimnazevent.ru/Hakaton/Sumsung/") // Replace with your base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);
    }
    private void loadSavedData() {
        String savedIP = prefs.getString("IP_ADDRESS", "192.168.1.216");
        int savedPort = prefs.getInt("PORT", 52000);
        ipAddressEditText.setText(savedIP);
        portEditText.setText(String.valueOf(savedPort));
    }
    public void SetIpStatus(boolean status){
        if(status){
            ipAddressEditText.setTextColor(Color.WHITE);
            portEditText.setTextColor(Color.WHITE);
        }else {
            ipAddressEditText.setTextColor(Color.RED);
            portEditText.setTextColor(Color.RED);
        }
    }
    private void authenticateUser(String username, String password, AlertDialog dialog) {
        // Создание объекта запроса для аутентификации
        UserRequest request = new UserRequest(username, password, null);

        // Создание вызова API
        Call<UserResponse> call = retrofitInterface.authenticateUser(request);
        // Асинхронный запрос
        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                // Проверка успешности ответа
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse userResponse = response.body();

                    if (userResponse.isSuccess()) {
                        // Обработка успешной аутентификации
                        String userId = userResponse.getMessage();
                        if(dialog != null){ dialog.dismiss(); }
                        user_data = new UserRequest(username, password, userId);
                        prefs.edit().putString("username", username).apply();
                        prefs.edit().putString("password", password).apply();
                        fetchDataFromServer(userId);
                        Toast.makeText(TCP.this, "Successful " + userId, Toast.LENGTH_SHORT).show();
                    } else {
                        // Обработка неудачной аутентификации
                        Toast.makeText(TCP.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Обработка ошибки HTTP
                    Toast.makeText(TCP.this, "Error occurred: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                // Обработка ошибки запроса
                Toast.makeText(TCP.this, "Request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void registerUser(String username, String password, AlertDialog dialog) {
        // Создание объекта запроса для регистрации
        UserRequest userRequest = new UserRequest(username, password, null);
        Log.e("TCP", "Sending registration request: " + username + " " + password);

        // Создание вызова API
        Call<UserResponse> call = retrofitInterface.registerUser(userRequest);

        // Асинхронный запрос
        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                // Проверка успешности ответа
                if (response.isSuccessful()) {
                    UserResponse userResponse = response.body();

                    if (userResponse != null && userResponse.isSuccess()) {
                        // Обработка успешной регистрации
                        Log.e("TCP", "Registration successful: " + response.code() + " - " + response.message());
                        user_data = new UserRequest(username, password, response.message());
                        prefs.edit().putString("username", username).apply();
                        prefs.edit().putString("password", password).apply();
                        dataModelList.clear();
                       // fetchDataFromServer(response.message());
                        dialog.dismiss(); // Закрытие диалога
                        Toast.makeText(TCP.this, "Registration Successful. User ID: " + userResponse.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        // Обработка неудачной регистрации
                        Log.e("TCP", "Registration failed: " + (userResponse != null ? userResponse.getMessage() : "Unknown error"));
                        Toast.makeText(TCP.this, "Registration Failed: " + (userResponse != null ? userResponse.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Обработка ошибки HTTP
                    Log.e("TCP", "Registration error: HTTP " + response.code() + " - " + response.message());
                    Toast.makeText(TCP.this, "Error occurred: HTTP " + response.code() + " - " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e("TCP", "Registration request failed", t);
                Toast.makeText(TCP.this, "Request failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
