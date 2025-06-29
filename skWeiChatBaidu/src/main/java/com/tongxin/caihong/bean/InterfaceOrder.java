package com.tongxin.caihong.bean;

public class InterfaceOrder {
    private int friend;
    private int company;
    private int room;

    public void setFriend(int friend) {
        this.friend = friend;
    }

    public int getFriend() {
        return friend;
    }

    public void setCompany(int company) {
        this.company = company;
    }

    public int getCompany() {
        return company;
    }

    public void setRoom(int room) {
        this.room = room;
    }

    public int getRoom() {
        return room;
    }

    @Override
    public String toString() {
        return
                "InterfaceOrder{" +
                        "friend = '" + friend + '\'' +
                        ",company = '" + company + '\'' +
                        ",room = '" + room + '\'' +
                        "}";
    }

    public String getOrderString() {
        int[] buf = new int[]{1, 2, 3};
        buf[friend - 1] = 1;
        buf[company - 1] = 2;
        buf[room - 1] = 3;
        return "" + buf[0] + buf[1] + buf[2];
    }
}
