package org.jeecg.modules.ietm.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.aspect.annotation.Dict;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 工作流执行记录
 * @Author: IETM Team
 * @Date: 2026-08-20
 * @Version: V1.0
 */
@Data
@TableName("wf_execute")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "wf_execute对象", description = "工作流执行记录")
public class WfExecute implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键ID")
    private String id;

    /** 明细ID */
    @ApiModelProperty(value = "明细ID", required = true)
    @NotBlank(message = "明细ID不能为空")
    @TableField("instdtlid_")
    private String instdtlid;

    /** 处理结果 */
    @ApiModelProperty(value = "处理结果：1=通过,2=不同意,3=跳转,4=追加意见,5=拿回,9=终止")
    @Dict(dicCode = "wf_ifpass")
    @TableField("ifpass_")
    private String ifpass;

    /** 跳转标记 */
    @ApiModelProperty(value = "跳转标记（第几次退回）")
    @TableField("ifjump_")
    private String ifjump;

    /** 处理意见 */
    @ApiModelProperty(value = "处理意见")
    @TableField("opinion_")
    private String opinion;

    /** 附件名称 */
    @ApiModelProperty(value = "附件名称")
    @TableField("filename_")
    private String filename;

    /** 附件内容 */
    @ApiModelProperty(value = "附件内容")
    @TableField("filecontent_")
    private byte[] filecontent;

    /** 创建人（处理人） */
    @ApiModelProperty(value = "处理人")
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /** 创建人姓名（处理人姓名，transient，对齐旧系统 CREATED_NAME） */
    @ApiModelProperty(value = "处理人姓名")
    @TableField(exist = false)
    private String createName;

    /** 节点顺序号（用于排序，transient） */
    @ApiModelProperty(value = "节点顺序号")
    @TableField(exist = false)
    private Integer seqno;

    /** 节点名称（用于历史记录匹配，transient） */
    @ApiModelProperty(value = "节点名称")
    @TableField(exist = false)
    private String nodename;

    /** 节点类型（用于历史记录匹配，transient） */
    @ApiModelProperty(value = "节点类型")
    @TableField(exist = false)
    private String nodetype;

    /** 创建时间（处理时间） */
    @ApiModelProperty(value = "处理时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /** 最后更新人 */
    @ApiModelProperty(value = "最后更新人")
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 最后更新时间 */
    @ApiModelProperty(value = "最后更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 版本号（乐观锁） */
    @ApiModelProperty(value = "版本号")
    @Version
    @TableField("version_")
    private Integer version;

    /** 删除标识 */
    @ApiModelProperty(value = "删除标识：0=未删除,1=已删除")
    @TableLogic(value = "0", delval = "1")
    @TableField("del_flag")
    private String delFlag;

    /** 扩展字段01 */
    @ApiModelProperty(value = "扩展字段01")
    @TableField("attribute_01")
    private String attribute01;

    /** 扩展字段02 */
    @ApiModelProperty(value = "扩展字段02")
    @TableField("attribute_02")
    private String attribute02;

    /** 扩展字段03 */
    @ApiModelProperty(value = "扩展字段03")
    @TableField("attribute_03")
    private String attribute03;

    /** 扩展字段04 */
    @ApiModelProperty(value = "扩展字段04")
    @TableField("attribute_04")
    private String attribute04;

    /** 扩展字段05 */
    @ApiModelProperty(value = "扩展字段05")
    @TableField("attribute_05")
    private String attribute05;

    /** 扩展字段06 */
    @ApiModelProperty(value = "扩展字段06")
    @TableField("attribute_06")
    private String attribute06;

    /** 扩展字段07 */
    @ApiModelProperty(value = "扩展字段07")
    @TableField("attribute_07")
    private String attribute07;

    /** 扩展字段08 */
    @ApiModelProperty(value = "扩展字段08")
    @TableField("attribute_08")
    private String attribute08;

    /** 扩展字段09 */
    @ApiModelProperty(value = "扩展字段09")
    @TableField("attribute_09")
    private String attribute09;

    /** 扩展字段10 */
    @ApiModelProperty(value = "扩展字段10")
    @TableField("attribute_10")
    private String attribute10;
}
