package org.jeecg.modules.ietm.ietmdatamodulemanagement.constants;

/**
 * IETM数据模块管理常量
 * <p>
 * 集中定义引用类型、备注模板等常量，避免魔法字符串
 * </p>
 *
 * @author IETM Team
 * @since 2026-08-31
 */
public class IetmDataModuleConstants {

    // ==================== 引用类型常量 ====================

    /**
     * ICN引用类型：ICN到DM的引用
     */
    public static final String REF_TYPE_ICN_TO_DM = "ICN_TO_DM";

    /**
     * DM引用类型：DM到DM的引用
     */
    public static final String REF_TYPE_DM_TO_DM = "DM_TO_DM";

    // ==================== ICN标签类型 ====================

    /**
     * ICN标签类型：graphic
     */
    public static final String ICN_TAG_GRAPHIC = "graphic";

    /**
     * ICN标签类型：multimedia
     */
    public static final String ICN_TAG_MULTIMEDIA = "multimedia";

    /**
     * ICN标签类型：symbol
     */
    public static final String ICN_TAG_SYMBOL = "symbol";

    // ==================== ICN引用备注模板 ====================

    /**
     * ICN引用备注：DM保存时自动创建
     */
    public static final String ICN_REF_REMARK_SAVE = "DM保存时自动创建";

    /**
     * ICN引用备注：计算引用时自动创建
     */
    public static final String ICN_REF_REMARK_CALCULATE = "计算引用时自动创建";

    // ==================== DM引用标签类型 ====================

    /**
     * DM引用标签类型：dmRef
     */
    public static final String DM_REF_TAG = "dmRef";

    // ==================== 数据库字段值 ====================

    /**
     * 逻辑删除标记：未删除
     */
    public static final String ISDELETED_NO = "0";

    /**
     * 逻辑删除标记：已删除
     */
    public static final String ISDELETED_YES = "1";

    // ==================== 私有构造函数 ====================

    /**
     * 私有构造函数，防止实例化
     */
    private IetmDataModuleConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}
