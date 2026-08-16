package org.jeecg.modules.ietm.common;

/**
 * IETM模块通用常量定义
 * 用于替换代码中的魔法字符串和硬编码值
 */
public class IetmConstants {

    /**
     * 逻辑删除标记
     */
    public static final String DELETED_FLAG_NO = "0";
    public static final String DELETED_FLAG_YES = "1";

    /**
     * ICN文件类型
     */
    public static final String FILE_TYPE_ENTITY = "实体文件";
    public static final String FILE_TYPE_RELATED = "相关文件";

    /**
     * ICN引用类型
     */
    public static final String REFERENCE_TYPE_ICN_TO_ICN = "ICN_TO_ICN";
    public static final String REFERENCE_TYPE_ICN_TO_DM = "ICN_TO_DM";

    /**
     * 数据表名称
     */
    public static final String TABLE_IETM_DATA_MODULE = "ietm_data_module";

    /**
     * 默认XSD模式文件
     */
    public static final String DEFAULT_XSD = "descript.xsd";

    private IetmConstants() {
        // 工具类不应被实例化
    }
}
