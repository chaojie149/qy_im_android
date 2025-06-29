/**
 *
 */
package com.tongxin.caihong.socket.protocol;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * 版本: [1.0]
 * 功能说明:
 *
 * @author : WChao 创建时间: 2017年8月21日 下午3:08:04
 */
public class TcpServerDecoder {

//	private static Logger logger = LoggerFactory.getLogger(TcpServerDecoder.class);

    @NonNull
    public static TcpPacket decode(ByteBuffer buffer) throws TcpDecodeException {
        //校验协议头
        int headerLength = isHeaderLength(buffer);
        //获取第一个字节协议版本号;
        byte version = buffer.get();
        if (version != Protocol.VERSION) {
            throw new TcpDecodeException("wrong version: " + version, headerLength);
        }
        //标志位
        byte maskByte = buffer.get();
        Integer synSeq = 0;
        //同步发送;
        if (ImPacket.decodeHasSynSeq(maskByte)) {
            synSeq = buffer.getInt();
        }
        //cmd命令码
        short cmdByte = buffer.getShort();
		/*if(Command.forNumber(cmdByte) == null){
			throw new AioDecodeException(ImStatus.C10014.getText());
		}*/
        int bodyLen = buffer.getInt();
        int enBodyLen = bodyLen;
//        if (EMConnectionManager.MY_CODE != 0) {
//            // 注意，此处需要将bodyLen进行加密
//            enBodyLen = EncodeHelper.encodeBodyLen(bodyLen, EMConnectionManager.MY_CODE);
//        }
        //数据不正确，则抛出AioDecodeException异常
        if (bodyLen < 0) {
            throw new TcpDecodeException("wrong bodyLen: " + bodyLen, headerLength);
        }
        int readableLength = buffer.limit() - buffer.position();
        int validateBodyLen = readableLength - bodyLen;
        // 不够消息体长度(剩下的buffer组不了消息体)
        if (validateBodyLen < 0) {
            throw new TcpDecodeException("wrong validateBodyLen: " + validateBodyLen, headerLength);
        }
        byte[] body = new byte[bodyLen];
        buffer.get(body, 0, bodyLen);

        //byteBuffer的总长度是 = 2byte协议版本号+1byte消息标志位+4byte同步序列号(如果是同步发送则多4byte同步序列号,否则无4byte序列号)+1byte命令码+4byte消息的长度+消息体的长度
        //byteBuffer的总长度是 = 2byte协议版本号+1byte命令码+4byte消息的长度+消息体的长度
        TcpPacket tcpPacket = new TcpPacket(cmdByte, body);
        tcpPacket.setVersion(version);

        tcpPacket.setMask(maskByte);
        //同步发送设置同步序列号
        if (synSeq > 0) {
            tcpPacket.setSynSeq(synSeq);
        }


        //decrypt(tcpPacket, enBodyLen);
        return tcpPacket;
    }

    /**
     * 判断是否符合协议头长度
     *
     * @param buffer
     * @return 检查通过后返回检查了的字节数，
     */
    private static int isHeaderLength(ByteBuffer buffer) throws TcpDecodeException {
        int readableLength = buffer.limit() - buffer.position();
        if (readableLength == 0) {
            throw new TcpNotEnoughException("wrong readableLength: " + readableLength, null, 0, readableLength, 0);
        }
        //协议头索引;
        int index = buffer.position();
        try {
            //获取第一个字节协议版本号;
            buffer.get(index);
            index++;
            //标志位

            byte maskByte = buffer.get(index);
            //同步发送;
            if (ImPacket.decodeHasSynSeq(maskByte)) {
                index += 4;
            }
            index++;

            //cmd命令码, 此处为Short两个字节, 原代码为Byte一个字节, 有误，
            buffer.getShort(index);
            index += 2;
            //消息体长度
            int bodyLen = buffer.getInt(index);
            index += 4;
            int leftLength = buffer.limit() - index;
            if (leftLength < bodyLen) {
                // 剩下的长度不够这个包的内容，
                throw new TcpNotEnoughException("wrong leftLength: " + leftLength, null, index - buffer.position(), readableLength, bodyLen);
            }
        } catch (TcpNotEnoughException e) {
            throw e;
        } catch (IndexOutOfBoundsException e) {
            throw new TcpNotEnoughException("check failed", e, index - buffer.position(), readableLength, 0);
        } catch (Exception e) {
            throw new TcpDecodeException("check failed", e, index - buffer.position());
        }
        return index - buffer.position();
    }

//    private static void decrypt(TcpPacket packet, int enBodyLen) {
//        short command = packet.getCommand();
//        byte[] bytes = packet.getBytes();
//        packet.setCommand(DecodeHelper.decodeCommand(command, enBodyLen));
//        packet.setBytes(DecodeHelper.decodeBody(command, bytes));
//    }
}
