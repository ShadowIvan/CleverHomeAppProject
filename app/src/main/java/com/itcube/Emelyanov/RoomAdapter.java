package com.itcube.Emelyanov;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private List<DataModel> dataModelList;
    private Activity activity;
    private TCP tcpInstance;
    private RetrofitInterface retrofitInterface;

    public RoomAdapter(List<DataModel> dataModelList, Activity activity, TCP tcpInstance) {
        this.dataModelList = dataModelList;
        this.activity = activity;
        this.tcpInstance = tcpInstance;
        retrofitInterface = tcpInstance.retrofitInterface;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        DataModel dataModel = dataModelList.get(position);
        holder.roomName.setText(dataModel.getName());
        holder.roomId.setText(String.valueOf("Комната №" + dataModel.getRoom()));

        holder.editButton.setOnClickListener(v -> showEditDialog(dataModel, position));
        holder.deleteButton.setOnClickListener(v -> {
            DeleteData(position);
        });

        holder.textConteiner.setOnClickListener(v -> {
             SetRoomSetup(dataModel);
        });
    }

    void SetRoomSetup(DataModel dataModel){
        String info = dataModel.getInfo();

        boolean state1 = info.charAt(0) == '1';
        boolean state2 = info.charAt(1) == '1';
        boolean state3 = info.charAt(2) == '1';

        tcpInstance.L1.setChecked(state1);
        tcpInstance.L2.setChecked(state2);
        tcpInstance.L3.setChecked(state3);

        tcpInstance.updateThumbTint(tcpInstance.L1, state1);
        tcpInstance.updateThumbTint(tcpInstance.L2, state2);
        tcpInstance.updateThumbTint(tcpInstance.L3, state3);

        tcpInstance.sendCommandToArduino();
    }

    public void UpdateRecycle() {
        dataModelList.clear();
        notifyDataSetChanged(); // добавьте это, чтобы обновить RecyclerView
        tcpInstance.fetchDataFromServer(tcpInstance.user_data.getUserId());  // вызов метода fetchDataFromServer
    }

    public void DeleteData(int poz){
        Call<Void> call = retrofitInterface.deleteData(dataModelList.get(poz).getID());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Успешное удаление данных
                    Toast.makeText(tcpInstance, "Data successfully deleted", Toast.LENGTH_SHORT).show();
                    Log.d("DeleteData", "Data successfully deleted");
                    UpdateRecycle();
                    // Дополнительная логика для успешного удаления, например, обновление UI
                } else {
                    // Неуспешный запрос
                    Toast.makeText(tcpInstance, "Failed to delete data", Toast.LENGTH_SHORT).show();
                    Log.e("DeleteData", "Failed to delete data: " + response.message());
                    // Дополнительная логика для обработки ошибки, например, отображение сообщения пользователю
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Ошибка в процессе выполнения запроса
                Toast.makeText(tcpInstance, "Failed to delete data", Toast.LENGTH_SHORT).show();
                Log.e("DeleteData", "Error: " + t.getMessage());
                // Дополнительная логика для обработки ошибки, например, отображение сообщения пользователю
            }
        });
    }

    public void EditData(DataModel dataModel){
        Call<Void> call = retrofitInterface.editData(dataModel);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Успешное удаление данных
                    Toast.makeText(tcpInstance, "Data successfully edited", Toast.LENGTH_SHORT).show();
                    Log.d("EditData", "Data successfully edited " + response.message());
                    UpdateRecycle();
                    // Дополнительная логика для успешного удаления, например, обновление UI
                } else {
                    // Неуспешный запрос
                    Toast.makeText(tcpInstance, "Failed to edit data", Toast.LENGTH_SHORT).show();
                    Log.e("EditData", "Failed to edit data: " + response.message());
                    // Дополнительная логика для обработки ошибки, например, отображение сообщения пользователю
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Ошибка в процессе выполнения запроса
                Toast.makeText(tcpInstance, "Failed to edit data", Toast.LENGTH_SHORT).show();
                Log.e("EditData", "Error: " + t.getMessage());
                // Дополнительная логика для обработки ошибки, например, отображение сообщения пользователю
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataModelList.size();
    }

    public class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView roomName, roomId;
        Button editButton, deleteButton;
        LinearLayout textConteiner;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            roomName = itemView.findViewById(R.id.nameTextView);
            roomId = itemView.findViewById(R.id.roomIdTextView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            textConteiner = itemView.findViewById(R.id.textContainer);
        }
    }

    private void showEditDialog(DataModel dataModel, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomAlertDialog);

        View viewInflated = LayoutInflater.from(activity).inflate(R.layout.edit_room_dialog, null);
        final EditText inputName = viewInflated.findViewById(R.id.inputName);
        final EditText inputRoom = viewInflated.findViewById(R.id.inputRoom);
        final ImageView light1 = viewInflated.findViewById(R.id.Light1w);
        final ImageView light2 = viewInflated.findViewById(R.id.Light2w);
        final ImageView light3 = viewInflated.findViewById(R.id.Light3w);

        if (inputName == null || inputRoom == null || light1 == null || light2 == null || light3 == null) {
            Log.e("RoomAdapter", "One of the required views is null. Check your edit_room_dialog.xml layout.");
            return;
        }

        inputName.setText(dataModel.getName());
        inputRoom.setText(String.valueOf(dataModel.getRoom()));

        // Установка состояния светильников
        String lightInfo = dataModel.getInfo();
        if (lightInfo.length() >= 3) {
            updateLightState(light1, lightInfo.charAt(0));
            updateLightState(light2, lightInfo.charAt(1));
            updateLightState(light3, lightInfo.charAt(2));
        }

        // Установка обработчиков кликов для переключения состояния светильников
        light1.setOnClickListener(v -> toggleLightState(light1));
        light2.setOnClickListener(v -> toggleLightState(light2));
        light3.setOnClickListener(v -> toggleLightState(light3));

        builder.setView(viewInflated);

        builder.setPositiveButton("Сохранить", null);
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            Button applyButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            if (cancelButton != null) {
                cancelButton.setTypeface(null, Typeface.BOLD);
            }

            if (applyButton != null) {
                applyButton.setTypeface(null, Typeface.BOLD);
                applyButton.setOnClickListener(v -> {
                    String newName = inputName.getText().toString();
                    String roomStr = inputRoom.getText().toString();
                    int newRoom = -1;
                    try {
                        newRoom = Integer.parseInt(roomStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(activity, "Введите допустимый номер комнаты", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newRoom < 0 || newRoom > 6) {
                        Toast.makeText(activity, "Номер комнаты должен быть в диапазоне от 0 до 6", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newName.length() < 3 || newName.length() > 30) {
                        Toast.makeText(activity, "Название комнаты должно быть больше 3 и меньше 30 символов", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newInfo = getLightState(light1) + (getLightState(light2)) + getLightState(light3);
                    Toast.makeText(activity, newInfo, Toast.LENGTH_SHORT).show();
                    Log.e("EditData", newInfo);
                    dataModel.setName(newName);
                    dataModel.setRoom(newRoom);
                    dataModel.setInfo(newInfo);
                    //notifyItemChanged(position);
                    EditData(dataModel);
                    dialog.dismiss();
                });
            }
        });

        dialog.show();
    }

    private void updateLightState(ImageView light, char state) {
        if (state == '1') {
            light.setImageResource(R.drawable.ic_light_on);
        } else {
            light.setImageResource(R.drawable.ic_light_off);
        }
    }

    private void toggleLightState(ImageView light) {
        if (light.getDrawable().getConstantState() == activity.getDrawable(R.drawable.ic_light_off).getConstantState()) {
            light.setImageResource(R.drawable.ic_light_on);
        } else {
            light.setImageResource(R.drawable.ic_light_off);
        }
    }

    private String getLightState(ImageView light) {
        if (light.getDrawable().getConstantState() == activity.getDrawable(R.drawable.ic_light_off).getConstantState()) {
            return "0";
        } else {
            return "1";
        }
    }
}
