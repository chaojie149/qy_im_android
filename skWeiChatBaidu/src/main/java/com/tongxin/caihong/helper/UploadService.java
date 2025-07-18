package com.tongxin.caihong.helper;

import android.text.TextUtils;

import com.tongxin.caihong.MyApplication;
import com.tongxin.caihong.ui.base.CoreManager;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 生活圈、视界文件上传类
 */
public class UploadService {
    /* 表单的一些固定字段 */
    private static final String END = "\r\n";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final String BOUNDARY = "------WebKitFormBoundarywL3jvc3wm30NCvQt"; // 数据分隔线

    private static final String getFileType(String fileName) {
        if (fileName.endsWith(".png") || fileName.endsWith(".PNG")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".JPEG") || fileName.endsWith(".JPG")) {
            return "image/jpg";
        } else if (fileName.endsWith(".bmp") || fileName.endsWith(".BMP")) {
            return "image/bmp";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * @param filePathList
     * @return 返回json
     */
    public String uploadFile(List<String> filePathList) {
        if (filePathList == null || filePathList.size() <= 0) {
            return null;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(MyApplication.getContext()).accessToken);
        params.put("userId", CoreManager.requireSelf(MyApplication.getContext()).getUserId());
        params.put("validTime", "-1");// 文件有效期
        String result = "";// 返回信息

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        InputStream is = null;
        try {
            System.setProperty("http.keepAlive", "false");
            URL url = new URL(CoreManager.requireConfig(MyApplication.getContext()).UPLOAD_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);   // 允许输入
            // conn.setDoOutput(true);//允许输出
            conn.setUseCaches(false); //不使用Cache
            conn.setConnectTimeout(50000);// 50秒钟连接超时
            conn.setReadTimeout(50000);   // 50秒钟读数据超时
            conn.setChunkedStreamingMode(0);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", MULTIPART_FORM_DATA + "; boundary=" + BOUNDARY);

            StringBuilder sb = new StringBuilder();
            // 上传的表单参数部分，格式请参考文章
            for (Entry<String, String> entry : params.entrySet()) {// 构建表单字段内容
                sb.append("--");
                sb.append(BOUNDARY);
                sb.append(END);
                sb.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
                sb.append(entry.getValue());
                sb.append(END);
            }

            dos = new DataOutputStream(conn.getOutputStream());
            dos.write(sb.toString().getBytes());

            for (int i = 0; i < filePathList.size(); i++) {
                String filePath = filePathList.get(i);
                if (TextUtils.isEmpty(filePath)) {
                    continue;
                }
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);// 获得图片或文件名称
                String fileType = getFileType(fileName);
                StringBuilder sbsb = new StringBuilder();
                sbsb.append("--");
                sbsb.append(BOUNDARY);
                sbsb.append(END);
                sbsb.append("Content-Disposition: form-data; name=\"" + "files" + "\"; filename=\"" + fileName + "\"" + "\r\n" + "Content-Type: "
                        + fileType + "\r\n\r\n");
                dos.write(sbsb.toString().getBytes());
                File file = new File(filePath);
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[(int) Math.min(file.length(), 4L * 1024)]; // 4k
                int count = 0;
                while ((count = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, count);
                }
                dos.writeBytes(END);
                fis.close();

                dos.flush();
            }
            dos.writeBytes("--" + BOUNDARY + "--" + END);

            // 获取服务器响应
            is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            int ch;
            StringBuffer b = new StringBuffer();
            while ((ch = isr.read()) != -1) {
                b.append((char) ch);
            }
            result = b.toString();
        } catch (ConnectTimeoutException e) {
            e.printStackTrace();
        } catch (EOFException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            try {
                if (dos != null) {
                    dos.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
