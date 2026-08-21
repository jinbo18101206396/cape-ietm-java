package org.jeecg.modules.ietm.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 工作流节点明细
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
@Data
@TableName("wf_instance_dtl")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "wf_instance_dtl对象", description = "工作流节点明细")
public class WfInstanceDtl implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键ID")
    private String id;

    /** 所属工作流实例ID */
    @ApiModelProperty(value = "所属工作流实例ID", required = true)
    @TableField("instid_")
    private String instanceid;

    /** 节点顺序号（0=创建节点，启动时ifexec自动置Y） */
    @ApiModelProperty(value = "节点顺序号")
    @TableField("seqno_")
    private Integer seqno;

    /** 节点名称 */
    @ApiModelProperty(value = "节点名称")
    @TableField("nodename_")
    private String nodename;

    /** 节点类型：0=创建节点/所有人必完成，其他值见字典 */
    @ApiModelProperty(value = "节点类型")
    @TableField("nodetype_")
    private String nodetype;

    /** 处理人ID串（用户直接ID；部门dpt_前缀；角色rol_前缀；群组grp_前缀；岗位pst_前缀） */
    @ApiModelProperty(value = "处理人ID串")
    @TableField("userid_")
    private String userid;

    /** 处理人名称串，逗号分隔 */
    @ApiModelProperty(value = "处理人名称串")
    @TableField("useridname_")
    private String useridname;

    /** 所属阶段索引（分阶段模板时使用） */
    @ApiModelProperty(value = "所属阶段索引")
    @TableField("stagename_")
    private String stagename;

    /** 执行状态：N=未执行,Y=已执行,J=跳过,R=退回 */
    @ApiModelProperty(value = "执行状态")
    @TableField("ifexec_")
    private String ifexec;

    /** 可跳转节点ID串（空=不限制，-1=不可跳转） */
    @ApiModelProperty(value = "可跳转节点ID串")
    @TableField("ifgetback_")
    private String ifgetback;

    /** 跳转次数（退回次数累计） */
    @ApiModelProperty(value = "跳转次数")
    @TableField("ifjump_")
    private String ifjump;

    /** 是否可无意见通过：Y=可以,N=不可以 */
    @ApiModelProperty(value = "是否可无意见通过")
    @TableField("ifnoopinion_")
    private String ifnoopinion;

    /** 创建人 */
    @ApiModelProperty(value = "创建人")
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新人 */
    @ApiModelProperty(value = "更新人")
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
