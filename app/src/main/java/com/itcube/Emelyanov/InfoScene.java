package com.itcube.Emelyanov;

public class InfoScene {
    private String serverIp;
    private int serverPort;
    private int roomId;

    public InfoScene(String serverIp, int serverPort, int roomId) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.roomId = roomId;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }


    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getIdRoom() {
        return roomId;
    }
}
