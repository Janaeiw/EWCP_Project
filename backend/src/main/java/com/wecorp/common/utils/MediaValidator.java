package com.wecorp.common.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

/**
 * 企微素材校验工具
 */
public class MediaValidator {

    private static final int MAX_SIZE = 10 * 1024 * 1024; // 10MB

    // ========== 上传通用校验 ==========

    /**
     * 上传图片校验：≤10MB，JPG/PNG
     */
    public static void validateImage(byte[] data, String filename) {
        if (data.length > MAX_SIZE) {
            throw new IllegalArgumentException("图片大小超过10MB限制");
        }
        String lower = filename != null ? filename.toLowerCase() : "";
        if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png")) {
            throw new IllegalArgumentException("仅支持JPG、PNG格式的图片");
        }
    }

    /**
     * 上传视频校验：≤10MB，MP4
     */
    public static void validateVideo(byte[] data, String filename) {
        if (data.length > MAX_SIZE) {
            throw new IllegalArgumentException("视频大小超过10MB限制");
        }
        String lower = filename != null ? filename.toLowerCase() : "";
        if (!lower.endsWith(".mp4")) {
            throw new IllegalArgumentException("仅支持MP4格式的视频");
        }
        if (data.length < 8 || !"ftyp".equals(new String(data, 4, 4))) {
            throw new IllegalArgumentException("视频文件格式不正确，请上传MP4格式");
        }
    }

    // ========== 朋友圈特有限制 ==========

    /**
     * 朋友圈图片校验：长边≤10800px，短边≤1080px
     */
    public static void validateMomentImage(byte[] data) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) {
                throw new IllegalArgumentException("无法读取图片，请检查格式是否正确");
            }
            int w = img.getWidth();
            int h = img.getHeight();
            int longEdge = Math.max(w, h);
            int shortEdge = Math.min(w, h);
            if (longEdge > 10800) {
                throw new IllegalArgumentException("图片长边超过10800像素限制（当前" + longEdge + "px）");
            }
            if (shortEdge > 1080) {
                throw new IllegalArgumentException("图片短边超过1080像素限制（当前" + shortEdge + "px）");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("图片格式校验失败: " + e.getMessage());
        }
    }

    /**
     * 朋友圈视频校验：时长≤30s
     */
    public static void validateMomentVideo(byte[] data) {
        try {
            double duration = getMp4Duration(data);
            if (duration > 30) {
                throw new IllegalArgumentException("视频时长超过30秒限制（当前" + String.format("%.1f", duration) + "秒）");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            // 时长解析失败不阻断，由企微服务端兜底校验
        }
    }

    // ========== MP4 时长解析 ==========

    private static double getMp4Duration(byte[] data) throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(data);
        while (buf.remaining() >= 8) {
            int pos = buf.position();
            long size = Integer.toUnsignedLong(buf.getInt());
            int type = buf.getInt();
            if (size < 8) break;
            if (type == 0x6D6F6F76) { // "moov"
                return parseMoov(buf, pos + (int) size);
            }
            buf.position(pos + (int) size);
        }
        throw new Exception("未找到moov atom");
    }

    private static double parseMoov(ByteBuffer buf, int moovEnd) throws Exception {
        while (buf.position() < moovEnd - 8) {
            int pos = buf.position();
            long size = Integer.toUnsignedLong(buf.getInt());
            int type = buf.getInt();
            if (size < 8) break;
            if (type == 0x6D766864) { // "mvhd"
                return parseMvhd(buf);
            }
            buf.position(pos + (int) size);
        }
        throw new Exception("未找到mvhd atom");
    }

    private static double parseMvhd(ByteBuffer buf) {
        int version = buf.get() & 0xFF;
        buf.position(buf.position() + 3); // flags
        if (version == 0) {
            buf.position(buf.position() + 8);
            int timescale = buf.getInt();
            int duration = buf.getInt();
            return (double) duration / timescale;
        } else {
            buf.position(buf.position() + 16);
            int timescale = buf.getInt();
            long duration = buf.getLong();
            return (double) duration / timescale;
        }
    }
}
