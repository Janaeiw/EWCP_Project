package com.wecorp.common.utils;

import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 企微原生 HTTP 工具类
 * 封装 SDK 未覆盖的接口（如 upload_attachment）
 */
@Component
@RequiredArgsConstructor
public class WxMediaUtils {

    private static final String WX_API_BASE = "https://qyapi.weixin.qq.com";

    private final WxCpService wxCpService;

    /**
     * 上传临时素材
     * POST /cgi-bin/media/upload?access_token=TOKEN&type=TYPE
     *
     * @param file      本地文件
     * @param filename  文件名
     * @param mediaType 媒体类型：image / voice / video / file
     * @return media_id
     */
    public String uploadMedia(File file, String filename, String mediaType)
            throws IOException, WxErrorException {
        String url = WX_API_BASE + "/cgi-bin/media/upload"
                + "?access_token=" + wxCpService.getAccessToken()
                + "&type=" + mediaType;
        return extractMediaId(httpPostMultipart(url, file, filename));
    }

    /**
     * 上传附件资源（朋友圈/商品图册）
     * POST /cgi-bin/media/upload_attachment?access_token=TOKEN&media_type=TYPE&attachment_type=N
     *
     * @param file           本地文件
     * @param filename       文件名
     * @param mediaType      媒体类型：image / video / file
     * @param attachmentType 附件类型：1=朋友圈，2=商品图册
     * @return media_id
     */
    public String uploadAttachment(File file, String filename, String mediaType, int attachmentType)
            throws IOException, WxErrorException {
        String url = WX_API_BASE + "/cgi-bin/media/upload_attachment"
                + "?access_token=" + wxCpService.getAccessToken()
                + "&media_type=" + mediaType
                + "&attachment_type=" + attachmentType;
        return extractMediaId(httpPostMultipart(url, file, filename));
    }

    // ========== 静态工具方法 ==========

    /**
     * 发送 multipart/form-data POST 请求
     * Content-Disposition 包含 filename 和 filelength（企微要求）
     */
    public static String httpPostMultipart(String urlStr, File file, String filename) throws IOException {
        String boundary = "----WxBoundary" + System.currentTimeMillis();
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(60_000);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = conn.getOutputStream()) {
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"media\";filename=\"" + filename
                    + "\"; filelength=" + file.length() + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            try (InputStream in = new FileInputStream(file)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * 从企微 JSON 响应中提取 media_id，失败时解析 errmsg 抛出可读异常
     */
    public static String extractMediaId(String responseBody) throws IOException {
        if (responseBody != null && responseBody.contains("\"media_id\"")) {
            int start = responseBody.indexOf("\"media_id\":\"") + 12;
            int end = responseBody.indexOf("\"", start);
            return responseBody.substring(start, end);
        }
        // 解析企微错误信息
        String msg = "上传失败";
        if (responseBody != null && responseBody.contains("\"errmsg\"")) {
            int start = responseBody.indexOf("\"errmsg\":\"") + 10;
            int end = responseBody.indexOf("\"", start);
            if (start > 9 && end > start) {
                msg = responseBody.substring(start, end);
            }
        }
        throw new IOException(msg);
    }
}
