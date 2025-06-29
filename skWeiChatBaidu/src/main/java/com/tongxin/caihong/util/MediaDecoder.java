package com.tongxin.caihong.util;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * https://www.programmersought.com/article/10681659011/
 */
public class MediaDecoder {
    private static final String TAG = "MediaDecoder";

    public static void decode(String encodeFile, OutputStream fosDecoder) throws IOException {
        decode(encodeFile, fosDecoder, 16000);
    }

    /**
     * 转换成pcm的采样率，
     */
    public static void decode(String encodeFile, OutputStream fosDecoder, int targetSampleRate) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(encodeFile);
        MediaCodec decoder = null;
        int sampleRate = 0;

        // Select the first audio track we find.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; ++i) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            if (mime.startsWith("audio/")) {
                extractor.selectTrack(i);
                decoder = MediaCodec.createDecoderByType(mime);
                decoder.configure(format, null, null, 0);
                break;
            }
        }

        if (decoder == null) {
            throw new IllegalArgumentException("No decoder for file format");
        }

        decoder.start();

        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
        ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean isEOS = false;
        long startMs = System.currentTimeMillis();

        while (!Thread.interrupted()) {
            if (!isEOS) {
                int inIndex = decoder.dequeueInputBuffer(10000);
                if (inIndex >= 0) {
                    ByteBuffer buffer = inputBuffers[inIndex];
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        // We shouldn't stop the playback at this point, just pass the EOS
                        // flag to decoder, we will get it again from the
                        // dequeueOutputBuffer
                        Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                    } else {
                        decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }

            int outIndex = decoder.dequeueOutputBuffer(info, 10000);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                    outputBuffers = decoder.getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                    break;
                default:
                    ByteBuffer buffer = outputBuffers[outIndex];
                    // 采样率转换，
                    // 这里默认是，两个字节一个音频帧，
                    double rate = 1d * targetSampleRate / sampleRate;
                    int remain = buffer.remaining();
                    int count = (int) (remain / 2 * rate);
                    byte[] data = new byte[count * 2];
                    int current = 0;
                    int[] lastSample = new int[2];
                    for (int i = 0; i < count; i++) {
                        int target = (int) (i / rate);
                        while (target >= current) {
                            lastSample[0] = buffer.get();
                            lastSample[1] = buffer.get();
                            current++;
                        }
                        data[i * 2] = (byte) lastSample[0];
                        data[i * 2 + 1] = (byte) lastSample[1];
                    }
                    fosDecoder.write(data);
                    decoder.releaseOutputBuffer(outIndex, true);
                    break;
            }

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }

        decoder.stop();
        decoder.release();
        extractor.release();
    }
}
