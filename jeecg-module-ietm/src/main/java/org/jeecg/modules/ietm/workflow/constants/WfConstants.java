package org.jeecg.modules.ietm.workflow.constants;

/**
 * @Description: 工作流常量定义
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
public class WfConstants {

    /** 流程状态 */
    public static final String STATUS_DRAFT = "0";        // 草稿
    public static final String STATUS_RUNNING = "1";      // 流转中
    public static final String STATUS_ENDED = "2";        // 已结束
    public static final String STATUS_TERMINATED = "9";   // 强制终止

    /** 节点执行状态 */
    public static final String EXEC_NO = "N";             // 未执行
    public static final String EXEC_YES = "Y";            // 已执行
    public static final String EXEC_SKIP = "J";           // 跳过
    public static final String EXEC_RETURN = "R";         // 退回

    /** 节点类型 */
    public static final String NODE_TYPE_CREATE = "0";    // 创建节点
    public static final String NODE_TYPE_REVIEW = "1";    // 审核节点
    public static final String NODE_TYPE_APPROVE = "2";   // 签批节点

    /** 紧急级别 */
    public static final String URGENT_NORMAL = "1";       // 一般
    public static final String URGENT_URGENT = "2";       // 紧急
    public static final String URGENT_VERY = "3";         // 特急

    /** seqno偏移量（重启流程时） */
    public static final int SEQNO_OFFSET = 100;

    /** 用户ID前缀 */
    public static final String PREFIX_DEPT = "dpt_";      // 部门
    public static final String PREFIX_ROLE = "rol_";      // 角色
    public static final String PREFIX_POST = "pst_";      // 岗位
    public static final String PREFIX_GROUP = "grp_";     // 用户组

    private WfConstants() {
        // 工具类不允许实例化
    }
}
