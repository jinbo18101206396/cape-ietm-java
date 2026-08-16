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
 * @Description: 工作流模板节点
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
@Data
@TableName("wf_template_dtl")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "wf_template_dtl对象", description = "工作流模板节点")
public class WfTemplateDtl implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键ID")
    private String id;

    /** 所属模板ID */
    @ApiModelProperty(value = "所属模板ID", required = true)
    @TableField("templateid_")
    private String templateid;

    /** 节点顺序号 */
    @ApiModelProperty(value = "节点顺序号")
    @TableField("seqno_")
    private Integer seqno;

    /** 节点名称 */
    @ApiModelProperty(value = "节点名称")
    @TableField("nodename_")
    private String nodename;

    /** 节点类型 */
    @ApiModelProperty(value = "节点类型")
    @TableField("nodetype_")
    private String nodetype;

    /** 默认处理人ID串 */
    @ApiModelProperty(value = "默认处理人ID串")
    @TableField("userid_")
    private String userid;

    /** 默认处理人名称 */
    @ApiModelProperty(value = "默认处理人名称")
    @TableField("useridname_")
    private String useridname;

    /** 所属阶段索引 */
    @ApiModelProperty(value = "所属阶段索引")
    @TableField("stagename_")
    private String stagename;

    /** 可跳转节点 */
    @ApiModelProperty(value = "可跳转节点")
    @TableField("ifgetback_")
    private String ifgetback;

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
}
