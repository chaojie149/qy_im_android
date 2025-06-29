package com.tongxin.caihong.socket.protocol;

@SuppressWarnings("unused")
public class TcpNotEnoughException extends TcpDecodeException {
    private final int readableLength;
    private final int bodyLen;

    public TcpNotEnoughException(String message, Throwable cause, int headerLength, int readableLength, int bodyLen) {
        super(message, headerLength);
        this.readableLength = readableLength;
        this.bodyLen = bodyLen;
    }

    public int getReadableLength() {
        return readableLength;
    }

    public Integer getBodyLen() {
        return bodyLen;
    }
}
