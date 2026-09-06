package org.jeecg.modules.ietm.ietmimport.constants;

/**
 * 数据模块导入常量
 *
 * @author IETM Team
 * @date 2026-09-03
 */
public class DmImportConstants {

    /** 14种校验错误码（对应旧系统formateresult函数） */
    public static final String ERROR_UNKNOWN = "-10";           // 未知原因失败
    public static final String ERROR_DM_EXISTS = "-1";          // DM已存在
    public static final String ERROR_SNS_NOT_IN_CM = "-2";      // SNS不在构型中
    public static final String ERROR_DDN_FILE_NOT_EXISTS = "-3"; // DDN文件不存在
    public static final String ERROR_CODE_MISMATCH = "-4";      // 编码不一致
    public static final String ERROR_MODEL_MISMATCH = "-5";     // 型号不匹配
    public static final String ERROR_SECURITY_NOT_EXISTS = "-6"; // 密级值不存在
    public static final String ERROR_SECURITY_EXCEED = "-7";    // 密级超限
    public static final String ERROR_NO_FILE = "-99";           // ZIP包无文件
    public static final String ERROR_ICN_NAME_INVALID = "-11";  // ICN文件名不规范
    public static final String ERROR_ICN_SNS_NOT_IN_CM = "-12"; // ICN的SNS不在构型中
    public static final String ERROR_ICN_EXISTS = "-13";        // ICN已存在

    // 资源文件相关错误码
    public static final String ERROR_RESOURCE_EXISTS = "-14";   // 资源文件已存在
    public static final String SUCCESS = "1";                   // 可以导入

    /** 错误消息映射（与旧系统formateresult函数保持一致） */
    public static String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case ERROR_UNKNOWN:
                return "未知原因导入失败";
            case ERROR_DM_EXISTS:
                return "DM已存在";
            case ERROR_SNS_NOT_IN_CM:
                return "SNS不在构型中";
            case ERROR_DDN_FILE_NOT_EXISTS:
                return "DDN文件列表不存在";
            case ERROR_CODE_MISMATCH:
                return "文件名与DM内容编码不一致";
            case ERROR_MODEL_MISMATCH:
                return "型号不匹配";
            case ERROR_SECURITY_NOT_EXISTS:
                return "密级值不存在";
            case ERROR_SECURITY_EXCEED:
                return "密级超限";
            case ERROR_NO_FILE:
                return "ZIP包无文件";
            case ERROR_ICN_NAME_INVALID:
                return "ICN文件名不规范";
            case ERROR_ICN_SNS_NOT_IN_CM:
                return "ICN的SNS不在构型中";
            case ERROR_ICN_EXISTS:
                return "ICN已存在";
            case ERROR_RESOURCE_EXISTS:
                return "资源文件已存在";
            case SUCCESS:
                return "可以导入";
            default:
                return "未知错误";
        }
    }

    /** 支持的文件类型 */
    public static final String FILE_TYPE_XML = ".xml";
    public static final String FILE_TYPE_ZIP = ".zip";

    /** 文件大小限制（1024MB = 1GB） */
    public static final long MAX_FILE_SIZE = 1024L * 1024 * 1024;

    /** 缓冲区大小（8KB，参考DdnPackageBuilder） */
    public static final int BUFFER_SIZE = 8192;

    /** ZIP炸弹防护：最大压缩比（正常文件通常<10:1，超过100:1视为可疑） */
    public static final int MAX_COMPRESSION_RATIO = 100;

    /** 单个XML/ICN文件最大大小限制（50MB） */
    public static final long MAX_SINGLE_FILE_SIZE = 50L * 1024 * 1024;
}
