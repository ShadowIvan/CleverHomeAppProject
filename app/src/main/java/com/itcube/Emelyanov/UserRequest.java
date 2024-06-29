package com.itcube.Emelyanov;

public class UserRequest {
    private String userName;
    private String userPassword;
    private String userId; // Новое поле для user_id

    // Конструктор для инициализации полей
    public UserRequest(String userName, String userPassword, String userId) {
        this.userName = userName;
        this.userPassword = userPassword;
        this.userId = userId;
    }

    // Геттеры и сеттеры для userName
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    // Геттеры и сеттеры для userPassword
    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    // Геттеры и сеттеры для userId
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

