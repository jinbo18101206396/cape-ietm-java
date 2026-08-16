package org.jeecg.modules.ietm.ietmdatamodulemanagement.constants;

/**
 * 数据模块管理常量类
 * 用于替代代码中的魔法值，提高可读性和可维护性
 */
public class DmConstants {

    /**
     * 数据状态
     */
    public static final String STATUS_NORMAL = "1";
    public static final String STATUS_DELETED = "0";

    /**
     * 是否最新版本
     */
    public static final String IS_LATEST_YES = "1";
    public static final String IS_LATEST_NO = "0";

    /**
     * DM状态
     */
    public static final String DM_STATUS_DRAFT = "draft";
    public static final String DM_STATUS_EDITING = "editing";
    public static final String DM_STATUS_REVIEWING = "reviewing";
    public static final String DM_STATUS_PUBLISHED = "published";
    public static final String DM_STATUS_ARCHIVED = "archived";

    /**
     * 工作流状态
     */
    public static final String WORKFLOW_STATUS_NOT_SUBMITTED = "0";
    public static final String WORKFLOW_STATUS_IN_PROGRESS = "1";
    public static final String WORKFLOW_STATUS_APPROVED = "2";
    public static final String WORKFLOW_STATUS_REJECTED = "3";

    /**
     * 版本类型
     */
    public static final String VERSION_TYPE_DRAFT = "0";
    public static final String VERSION_TYPE_RELEASED = "1";

    /**
     * 默认值
     */
    public static final String DEFAULT_LANGUAGE = "zh";  // ISO 639标准：小写语言代码
    public static final String DEFAULT_COUNTRY = "CN";
    public static final String DEFAULT_USER = "system";
    public static final String DEFAULT_SCHEMA = "00";

    /**
     * DMC段长度
     */
    public static final int DMC_MODEL_IDENT_LENGTH = 2;
    public static final int DMC_SYSTEM_DIFF_LENGTH = 3;
    public static final int DMC_SYSTEM_CODE_LENGTH = 1;
    public static final int DMC_SUB_SYSTEM_CODE_LENGTH = 1;
    public static final int DMC_SUB_SUB_SYSTEM_CODE_LENGTH = 2;
    public static final int DMC_ASSY_CODE_LENGTH = 2;
    public static final int DMC_DISASSY_CODE_LENGTH = 2;
    public static final int DMC_DISASSY_CODE_VARIANT_LENGTH = 1;
    public static final int DMC_INFO_CODE_LENGTH = 3;
    public static final int DMC_INFO_CODE_VARIANT_LENGTH = 1;
    public static final int DMC_ITEM_LOCATION_CODE_LENGTH = 1;

    /**
     * 引用类型
     */
    public static final String REF_TYPE_OUT = "out";
    public static final String REF_TYPE_IN = "in";

    /**
     * 操作类型
     */
    public static final String OP_TYPE_ADD = "add";
    public static final String OP_TYPE_EDIT = "edit";
    public static final String OP_TYPE_DELETE = "delete";
    public static final String OP_TYPE_CHECKOUT = "checkout";
    public static final String OP_TYPE_CHECKIN = "checkin";
    public static final String OP_TYPE_CANCEL_CHECKOUT = "cancelCheckout";
    public static final String OP_TYPE_PUBLISH = "publish";

    /**
     * 文件类型
     */
    public static final String FILE_TYPE_XML = "xml";
    public static final String FILE_TYPE_ZIP = "zip";
    public static final String FILE_TYPE_PDF = "pdf";

    /**
     * XML命名空间
     */
    public static final String S1000D_NAMESPACE = "http://www.s1000d.org";

    /**
     * DMC格式正则
     */
    public static final String DMC_PATTERN = "^[A-Z0-9]{2,4}-[A-Z0-9]{2,5}-[A-Z0-9]{1,2}-[A-Z0-9]{1,2}-[A-Z0-9]{2,4}-[A-Z0-9]{2,4}-[A-Z0-9]{1,3}$";

    private DmConstants() {
        // 工具类，禁止实例化
    }
}
