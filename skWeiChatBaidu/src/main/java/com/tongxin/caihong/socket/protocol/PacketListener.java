package com.tongxin.caihong.socket.protocol;

public interface PacketListener {
    void onAfterSent(Packet packet, boolean isSentSuccess) throws Exception;
}
