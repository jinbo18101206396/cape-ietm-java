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
 * @Description: 工作流模板
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
@Data
@TableName("wf_template")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "wf_template对象", description = "工作流模板")
public class WfTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键ID")
    private String id;

    /** 模板名称 */
    @ApiModelProperty(value = "模板名称", required = true)
    @TableField("tmplname_")
    private String tmplname;

    /** 状态：0=草稿,1=已发布 */
    @ApiModelProperty(value = "状态：0=草稿,1=已发布")
    @TableField("status_")
    private String status;

    /** 阶段名称，逗号分隔（有值表示分阶段模板） */
    @ApiModelProperty(value = "阶段名称")
    @TableField("stagenames_")
    private String stagenames;

    /** 模板类型标识（如DM、CM等） */
    @ApiModelProperty(value = "模板类型")
    @TableField("tmpltype_")
    private String tmpltype;

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
