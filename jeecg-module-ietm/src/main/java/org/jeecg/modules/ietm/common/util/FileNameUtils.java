package org.jeecg.modules.ietm.common.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件名处理工具类
 * 统一全项目的文件名清理和提取规则
 *
 * @author IETM Team
 * @date 2026-09-02
 */
@Slf4j
public class FileNameUtils {

    /** 默认最大文件名长度 */
    private static final int DEFAULT_MAX_LENGTH = 200;

    /** Windows + ZIP不允许的字符 */
    private static final String UNSAFE_CHARS_PATTERN = "[\\\\/:*?\"<>|]";

    /**
     * 清理文件名，移除不安全字符
     * 保留Unicode字符（支持中文等）
     *
     * @param fileName 原始文件名
     * @return 安全的文件名
     */
    public static String sanitize(String fileName) {
        return sanitize(fileName, DEFAULT_MAX_LENGTH);
    }

    /**
     * 清理文件名，移除不安全字符
     *
     * @param fileName 原始文件名
     * @param maxLength 最大长度
     * @return 安全的文件名
     */
    public static String sanitize(String fileName, int maxLength) {
        if (fileName == null || fileName.isEmpty()) {
            return "unnamed";
        }

        // 黑名单过滤：移除文件系统不安全字符，保留Unicode
        String safe = fileName.replaceAll(UNSAFE_CHARS_PATTERN, "_");

        // 智能截断：保留扩展名
        if (safe.length() > maxLength) {
            int lastDot = safe.lastIndexOf('.');
            if (lastDot > 0 && safe.length() - lastDot < 10) {
                // 有扩展名且不太长，保留扩展名
                String ext = safe.substring(lastDot);
                String base = safe.substring(0, Math.min(maxLength - ext.length(), lastDot));
                safe = base + ext;
            } else {
                // 无扩展名或扩展名太长，直接截断
                safe = safe.substring(0, maxLength);
            }
        }

        // 防止特殊名称
        if (safe.isEmpty() || safe.equals(".") || safe.equals("..")) {
            return "unnamed";
        }

        return safe;
    }

    /**
     * 从系统生成的文件名中提取原始文件名
     * <p>
     * 系统在上传文件时会在文件名中添加ID后缀以避免重名：
     * <ul>
     *   <li>原始文件名: "金波.jpg"</li>
     *   <li>系统存储名: "金波_1786887767219_1788313777683.jpg"</li>
     *   <li>提取结果: "金波.jpg"</li>
     * </ul>
     * </p>
     *
     * @param systemFileName 系统生成的文件名（带ID后缀）
     * @return 原始文件名，如果不匹配格式则返回原值
     */
    public static String extractOriginalName(String systemFileName) {
        if (systemFileName == null || !systemFileName.contains("_")) {
            return systemFileName;
        }

        try {
            // 找到第一个下划线位置
            int firstUnderscore = systemFileName.indexOf('_');
            String baseName = systemFileName.substring(0, firstUnderscore);

            // 提取文件扩展名
            int lastDot = systemFileName.lastIndexOf('.');
            String fileExt = (lastDot > 0) ? systemFileName.substring(lastDot) : "";

            return baseName + fileExt;
        } catch (Exception e) {
            log.warn("提取原始文件名失败，返回原值: {}", systemFileName, e);
            return systemFileName;
        }
    }

    /**
     * 构建带前缀的文件名（避免重名冲突）
     * <p>
     * 用于DDN导出等场景，不同DM可能有同名资源文件，通过添加前缀避免冲突：
     * <ul>
     *   <li>prefix: "DMC-ABC-123"</li>
     *   <li>fileName: "图片.jpg"</li>
     *   <li>结果: "DMC-ABC-123_图片.jpg"</li>
     * </ul>
     * </p>
     *
     * @param prefix 前缀（如DMC编码、ICN编码）
     * @param fileName 原始文件名
     * @return prefix_fileName格式（两部分都经过安全清理）
     */
    public static String withPrefix(String prefix, String fileName) {
        if (prefix == null || prefix.isEmpty()) {
            return sanitize(fileName);
        }
        return sanitize(prefix) + "_" + sanitize(fileName);
    }

    /**
     * 提取文件扩展名（包含点号）
     *
     * @param fileName 文件名
     * @return 扩展名（如".jpg"），无扩展名返回空字符串
     */
    public static String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }
        return "";
    }

    /**
     * 提取文件基础名（不含扩展名）
     *
     * @param fileName 文件名
     * @return 基础名（如"文档.pdf" → "文档"）
     */
    public static String getBaseName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    /**
     * 验证文件名是否安全（不含非法字符）
     *
     * @param fileName 待验证的文件名
     * @return true=安全，false=包含非法字符
     */
    public static boolean isSafe(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        // 检查是否包含不安全字符
        return !fileName.matches(".*" + UNSAFE_CHARS_PATTERN + ".*");
    }
}
