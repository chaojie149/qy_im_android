package com.tongxin.caihong.socket.protocol;

@SuppressWarnings("unused")
public class TcpDecodeException extends Exception {
    private Integer headerLength = null;

    public TcpDecodeException(String message, Throwable cause, int headerLength) {
        super(message, cause);
    }

    public TcpDecodeException(String message, int headerLength) {
        super(message);
        this.headerLength = headerLength;
    }

    public int getHeaderLength() {
        return headerLength;
    }
}
