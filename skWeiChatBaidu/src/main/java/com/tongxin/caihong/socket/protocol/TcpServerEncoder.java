/**
 *
 */
package com.tongxin.caihong.socket.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 版本: [1.0]
 * 功能说明:
 *
 * @author : WChao 创建时间: 2017年8月21日 下午4:00:31
 */
public class TcpServerEncoder {

    public static ByteBuffer encode(TcpPacket tcpPacket) {
        int bodyLen = tcpPacket.getBytes() == null ? 0 : tcpPacket.getBytes().length;
        int enBodyLen = bodyLen;
//        if (tcpPacket.getCommand() != Command.COMMAND_AUTH_REQ
//                && EMConnectionManager.MY_CODE != 0) {
//            enBodyLen = EncodeHelper.encodeBodyLen(bodyLen, EMConnectionManager.MY_CODE);
//        }
//        encrypt(tcpPacket, enBodyLen);

        boolean isCompress = true;
        boolean is4ByteLength = true;
        boolean isEncrypt = true;
        boolean isHasSynSeq = tcpPacket.getSynSeq() > 0;
        //协议版本号
        byte version = Protocol.VERSION;

        //协议标志位mask
        byte maskByte = ImPacket.encodeEncrypt(version, isEncrypt);
        maskByte = ImPacket.encodeCompress(maskByte, isCompress);
        maskByte = ImPacket.encodeHasSynSeq(maskByte, isHasSynSeq);
        maskByte = ImPacket.encode4ByteLength(maskByte, is4ByteLength);
        short cmdByte = tcpPacket.getCommand();
        byte[] body = tcpPacket.getBytes();
        //消息类型;
		/*if(tcpPacket.getCommand() != null) {
			cmdByte = (byte) (cmdByte | tcpPacket.getCommand().getNumber());
		}*/

        tcpPacket.setVersion(version);
        tcpPacket.setMask(maskByte);

        //byteBuffer的总长度是 = 1byte协议版本号+1byte消息标志位+4byte同步序列号(如果是同步发送则都4byte同步序列号,否则无4byte序列号)+2byte命令码+4byte消息的长度+消息体
        int allLen = 1 + 1;
        if (isHasSynSeq) {
            allLen += 4;
        }
        allLen += 2 + 4 + bodyLen;
        ByteBuffer buffer = ByteBuffer.allocate(allLen);
        //设置字节序
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        buffer.order(byteOrder);
        buffer.put(tcpPacket.getVersion());

        buffer.put(tcpPacket.getMask());
        //同步发送设置4byte，同步序列号;
        if (isHasSynSeq) {
            buffer.putInt(tcpPacket.getSynSeq());
        }

        buffer.putShort(cmdByte);
        buffer.putInt(bodyLen);
        buffer.put(body);
        return buffer;
    }

//    private static void encrypt(TcpPacket packet, int enBodyLen) {
//        short command = packet.getCommand();
//        byte[] bytes = packet.getBytes();
//        packet.setCommand(EncodeHelper.encodeCommand(command, enBodyLen));
//        packet.setBytes(EncodeHelper.encodeBody(packet.getCommand(), bytes));
//    }
}
