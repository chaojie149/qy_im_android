package com.tongxin.caihong.bean.redpacket;

import java.util.List;

public class RoomNotReceivedRedPacket {
    private int count;
    private double total;
    private List<RedPacket> data;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<RedPacket> getData() {
        return data;
    }

    public void setData(List<RedPacket> data) {
        this.data = data;
    }
}

