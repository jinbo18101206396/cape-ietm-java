package org.jeecg.modules.ietm.common.util;

import lombok.extern.slf4j.Slf4j;

/**
 * MIME类型检测工具类
 *
 * 功能：基于文件头魔数（Magic Number）检测文件的真实MIME类型
 *
 * 支持格式：
 * - 视频：MP4, WebM, AVI, MOV, FLV, MKV
 * - 音频：MP3, WAV, OGG, FLAC, AAC, M4A
 * - 图片：JPEG, PNG, GIF, BMP, TIFF, SVG
 *
 * 安全特性：
 * - 不依赖文件扩展名（防止伪装）
 * - 读取文件头前12字节进行匹配
 * - 失败时降级为 application/octet-stream
 *
 * @author claude
 * @date 2026-09-18
 */
@Slf4j
public class MimeTypeDetector {

    /**
     * 文件头读取长度（字节）
     */
    private static final int HEADER_LENGTH = 12;

    /**
     * 基于字节数组检测MIME类型
     *
     * @param content 文件内容（至少前12字节）
     * @return MIME类型字符串
     */
    public static String detect(byte[] content) {
        if (content == null || content.length < 4) {
            log.warn("文件内容为空或太短，无法检测MIME类型");
            return "application/octet-stream";
        }

        // === 视频格式检测 ===

        // MP4: 魔数 "ftyp" 位于字节4-7 (0x66 0x74 0x79 0x70)
        if (content.length >= 12 &&
            content[4] == 0x66 && content[5] == 0x74 &&
            content[6] == 0x79 && content[7] == 0x70) {
            return "video/mp4";
        }

        // WebM: 魔数 0x1A 0x45 0xDF 0xA3
        if (content.length >= 4 &&
            content[0] == 0x1A && content[1] == 0x45 &&
            content[2] == (byte) 0xDF && content[3] == (byte) 0xA3) {
            return "video/webm";
        }

        // AVI: RIFF 文件头 + "AVI " 标识
        // 魔数: 0x52 0x49 0x46 0x46 ... 0x41 0x56 0x49 0x20
        if (content.length >= 12 &&
            content[0] == 0x52 && content[1] == 0x49 && content[2] == 0x46 && content[3] == 0x46 &&
            content[8] == 0x41 && content[9] == 0x56 && content[10] == 0x49 && content[11] == 0x20) {
            return "video/x-msvideo";
        }

        // MOV: QuickTime 容器，魔数 "moov" 或 "mdat" 或 "ftyp"
        if (content.length >= 8) {
            // 检查字节4-7
            String signature = new String(new byte[]{content[4], content[5], content[6], content[7]});
            if ("moov".equals(signature) || "mdat".equals(signature) || "wide".equals(signature)) {
                return "video/quicktime";
            }
        }

        // FLV: 魔数 0x46 0x4C 0x56 ("FLV")
        if (content.length >= 3 &&
            content[0] == 0x46 && content[1] == 0x4C && content[2] == 0x56) {
            return "video/x-flv";
        }

        // MKV: 魔数与WebM相同（都是Matroska容器）
        // 已在WebM中处理

        // === 音频格式检测 ===

        // MP3: 魔数 0xFF 0xFB 或 0xFF 0xF3 或 0xFF 0xF2
        // 或者 ID3 标签: 0x49 0x44 0x33 ("ID3")
        if (content.length >= 3) {
            if ((content[0] == (byte) 0xFF &&
                 (content[1] == (byte) 0xFB || content[1] == (byte) 0xF3 || content[1] == (byte) 0xF2)) ||
                (content[0] == 0x49 && content[1] == 0x44 && content[2] == 0x33)) {
                return "audio/mpeg";
            }
        }

        // WAV: RIFF 文件头 + "WAVE" 标识
        // 魔数: 0x52 0x49 0x46 0x46 ... 0x57 0x41 0x56 0x45
        if (content.length >= 12 &&
            content[0] == 0x52 && content[1] == 0x49 && content[2] == 0x46 && content[3] == 0x46 &&
            content[8] == 0x57 && content[9] == 0x41 && content[10] == 0x56 && content[11] == 0x45) {
            return "audio/wav";
        }

        // OGG: 魔数 "OggS" (0x4F 0x67 0x67 0x53)
        if (content.length >= 4 &&
            content[0] == 0x4F && content[1] == 0x67 &&
            content[2] == 0x67 && content[3] == 0x53) {
            return "audio/ogg";
        }

        // FLAC: 魔数 "fLaC" (0x66 0x4C 0x61 0x43)
        if (content.length >= 4 &&
            content[0] == 0x66 && content[1] == 0x4C &&
            content[2] == 0x61 && content[3] == 0x43) {
            return "audio/flac";
        }

        // AAC: ADTS 帧头 0xFF 0xF1 或 0xFF 0xF9
        if (content.length >= 2 &&
            content[0] == (byte) 0xFF &&
            (content[1] == (byte) 0xF1 || content[1] == (byte) 0xF9)) {
            return "audio/aac";
        }

        // M4A: 与MP4相同的容器格式，但通常用于音频
        // 可以通过 ftyp 后的品牌代码区分，但简化处理
        // 已在MP4中处理，返回 video/mp4（浏览器会自动处理）

        // === 图片格式检测 ===

        // JPEG: 魔数 0xFF 0xD8 0xFF
        if (content.length >= 3 &&
            content[0] == (byte) 0xFF && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF) {
            return "image/jpeg";
        }

        // PNG: 魔数 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
        if (content.length >= 8 &&
            content[0] == (byte) 0x89 && content[1] == 0x50 && content[2] == 0x4E && content[3] == 0x47 &&
            content[4] == 0x0D && content[5] == 0x0A && content[6] == 0x1A && content[7] == 0x0A) {
            return "image/png";
        }

        // GIF: 魔数 "GIF87a" 或 "GIF89a"
        if (content.length >= 6 &&
            content[0] == 0x47 && content[1] == 0x49 && content[2] == 0x46 &&
            content[3] == 0x38 && (content[4] == 0x37 || content[4] == 0x39) && content[5] == 0x61) {
            return "image/gif";
        }

        // BMP: 魔数 "BM" (0x42 0x4D)
        if (content.length >= 2 &&
            content[0] == 0x42 && content[1] == 0x4D) {
            return "image/bmp";
        }

        // TIFF: 魔数 0x49 0x49 0x2A 0x00 (小端) 或 0x4D 0x4D 0x00 0x2A (大端)
        if (content.length >= 4 &&
            ((content[0] == 0x49 && content[1] == 0x49 && content[2] == 0x2A && content[3] == 0x00) ||
             (content[0] == 0x4D && content[1] == 0x4D && content[2] == 0x00 && content[3] == 0x2A))) {
            return "image/tiff";
        }

        // SVG: XML 文本文件，魔数 "<svg" 或 "<?xml"
        if (content.length >= 5) {
            String header = new String(content, 0, Math.min(content.length, 100));
            if (header.contains("<svg") || (header.startsWith("<?xml") && header.contains("<svg"))) {
                return "image/svg+xml";
            }
        }

        // 未识别的格式
        log.debug("无法识别的文件格式，文件头: {}", bytesToHex(content));
        return "application/octet-stream";
    }


    /**
     * 字节数组转16进制字符串（用于日志调试）
     *
     * @param bytes 字节数组
     * @return 16进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 12); i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        return sb.toString().trim();
    }

    /**
     * 检查MIME类型是否为视频
     *
     * @param mimeType MIME类型字符串
     * @return true=视频，false=非视频
     */
    public static boolean isVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * 检查MIME类型是否为音频
     *
     * @param mimeType MIME类型字符串
     * @return true=音频，false=非音频
     */
    public static boolean isAudio(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/");
    }

    /**
     * 检查MIME类型是否为图片
     *
     * @param mimeType MIME类型字符串
     * @return true=图片，false=非图片
     */
    public static boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * 检查MIME类型是否为多媒体（视频或音频）
     *
     * @param mimeType MIME类型字符串
     * @return true=多媒体，false=非多媒体
     */
    public static boolean isMultimedia(String mimeType) {
        return isVideo(mimeType) || isAudio(mimeType);
    }
}
