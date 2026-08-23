package org.jeecg.modules.ietm.ietmdatamodulemanagement.constant;

/**
 * 数据模块常量定义
 *
 * @author Kiro AI
 * @date 2026-08-23
 */
public class DmConstants {

    // ==================== 流程节点 ====================

    /** 流程节点：DM编写 */
    public static final String WF_NODE_DM_WRITE = "DM编写";

    /** 流程节点：审核 */
    public static final String WF_NODE_REVIEW = "审核";

    /** 流程节点：审批 */
    public static final String WF_NODE_APPROVE = "审批";


    // ==================== 数据模块状态 ====================

    /** 状态：已删除 */
    public static final String STATUS_DELETED = "0";

    /** 状态：正常 */
    public static final String STATUS_NORMAL = "1";

    /** 状态：已发布 */
    public static final String STATUS_PUBLISHED = "2";


    // ==================== 是否最新版本 ====================

    /** 不是最新版本 */
    public static final String IS_LATEST_NO = "0";

    /** 是最新版本 */
    public static final String IS_LATEST_YES = "1";


    // ==================== 版本类型 ====================

    /** 版本类型：草稿/在编 */
    public static final String VERSION_TYPE_DRAFT = "0";

    /** 版本类型：已发布 */
    public static final String VERSION_TYPE_PUBLISHED = "1";


    // ==================== 工作流状态 ====================

    /** 工作流状态：未启动或已结束 */
    public static final String WF_STATUS_ENDED = "0";

    /** 工作流状态：流转中 */
    public static final String WF_STATUS_IN_PROGRESS = "1";

    /** 工作流状态：已撤销 */
    public static final String WF_STATUS_REVOKED = "2";


    // ==================== 版本号上限 ====================

    /** 在编版本号（inWork）上限 */
    public static final int MAX_INWORK = 99;

    /** 发行编号（issueNo）上限 */
    public static final int MAX_ISSUENO = 999;


    // ==================== 私有构造函数 ====================

    private DmConstants() {
        // 工具类，禁止实例化
    }
}
