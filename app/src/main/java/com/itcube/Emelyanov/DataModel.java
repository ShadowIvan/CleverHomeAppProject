package com.itcube.Emelyanov;


import java.util.List;

public class DataModel {

    private int ID;
    private String name;
    private int room;
    private String info;

    public DataModel(int ID, String name, int room, String info) {
        this.ID = ID;
        this.name = name;
        this.room = room;
        this.info = info;
    }

    public int getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public int getRoom() {
        return room;
    }

    public String getInfo() {
        return info;
    }


}
