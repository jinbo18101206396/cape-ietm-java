package org.jeecg.modules.ietm.ietmddn.constants;

/**
 * DDN模块常量配置
 *
 * @author IETM Team
 * @date 2026-09-01
 */
public class DdnConstants {

    /**
     * DDN状态码
     */
    public static class Status {
        /** 生成中 */
        public static final String GENERATING = "0";
        /** 生成成功 */
        public static final String SUCCESS = "1";
        /** 生成失败 */
        public static final String FAILED = "-1";
    }

    /**
     * 文件大小限制配置
     * 修复P2-7：将硬编码改为可配置的常量
     */
    public static class FileSize {
        /** 单个文件最大大小：100MB */
        public static final long MAX_FILE_SIZE = 100 * 1024 * 1024L;
        /** ZIP包总大小限制：1GB */
        public static final long MAX_ZIP_SIZE = 1024 * 1024 * 1024L;
    }

    /**
     * DDN递归收集配置
     */
    public static class Collection {
        /** 递归收集DM的最大深度 */
        public static final int MAX_RECURSION_DEPTH = 10;
    }

    /**
     * 引用类型常量
     * 修复P2：提取硬编码字符串
     */
    public static class ReferenceType {
        /** ICN被DM引用 */
        public static final String ICN_TO_DM = "ICN_TO_DM";
        /** DM引用DM */
        public static final String DM_TO_DM = "DM_TO_DM";
    }

    /**
     * S1000D标准版本
     * 修复P2：提取硬编码字符串
     */
    public static class Standard {
        /** 默认标准版本 */
        public static final String DEFAULT_VERSION = "S1000D40";
        /** S1000D 4.0 */
        public static final String S1000D_40 = "S1000D40";
        /** S1000D 4.1 */
        public static final String S1000D_41 = "S1000D41";
        /** S1000D 4.2 */
        public static final String S1000D_42 = "S1000D42";
    }

    /**
     * S1000D默认属性值
     * 修复P2：提取硬编码字符串
     */
    public static class DefaultValues {
        /** 系统差异代码默认值 */
        public static final String SYSTEM_DIFF_CODE = "A";
        /** 系统代码默认值 */
        public static final String SYSTEM_CODE = "00";
        /** 子系统代码默认值 */
        public static final String SUB_SYSTEM_CODE = "0";
        /** 子子系统代码默认值 */
        public static final String SUB_SUB_SYSTEM_CODE = "0";
        /** 组件代码默认值 */
        public static final String ASSY_CODE = "00";
        /** 分解组件代码默认值 */
        public static final String DISASSY_CODE = "00";
        /** 分解组件变体代码默认值 */
        public static final String DISASSY_CODE_VARIANT = "A";
        /** 信息代码默认值 */
        public static final String INFO_CODE = "000";
        /** 信息代码变体默认值 */
        public static final String INFO_CODE_VARIANT = "A";
        /** 项目位置代码默认值 */
        public static final String ITEM_LOCATION_CODE = "A";
    }

    /**
     * 目录名称常量
     */
    public static class DirectoryNames {
        /** DM目录 */
        public static final String DM = "DM";
        /** ICN目录 */
        public static final String ICN = "ICN";
        /** 多媒体资源目录 */
        public static final String MM = "MM";
    }

    /**
     * 文件扩展名常量
     */
    public static class FileExtensions {
        /** XML文件扩展名 */
        public static final String XML = ".xml";
        /** 日志文件扩展名 */
        public static final String LOG = ".log";
        /** ZIP文件扩展名 */
        public static final String ZIP = ".zip";
    }
}
