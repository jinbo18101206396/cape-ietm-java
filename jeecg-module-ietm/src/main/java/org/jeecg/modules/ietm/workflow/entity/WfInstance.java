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
 * @Description: 工作流实例
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
@Data
@TableName("wf_instance")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "wf_instance对象", description = "工作流实例")
public class WfInstance implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键ID")
    private String id;

    /** 业务表单ID（关联ietm_data_module.id） */
    @ApiModelProperty(value = "业务表单ID", required = true)
    @TableField("formid_")
    private String formid;

    /** 流程标题（通常为空串，展示依赖titleparam_） */
    @ApiModelProperty(value = "流程标题")
    @TableField("title_")
    private String title;

    /** 待办标题参数，格式【DMC1】,【DMC2】 */
    @ApiModelProperty(value = "待办标题参数")
    @TableField("titleparam_")
    private String titleparam;

    /** 业务表单URL */
    @ApiModelProperty(value = "业务表单URL")
    @TableField("url_")
    private String url;

    /** 流程状态：0=草稿,1=流转中,2=已结束,9=强制终止 */
    @ApiModelProperty(value = "流程状态：0=草稿,1=流转中,2=已结束,9=强制终止")
    @TableField("status_")
    private String status;

    /** 紧急级别：1=一般,2=紧急,3=特急 */
    @ApiModelProperty(value = "紧急级别：1=一般,2=紧急,3=特急")
    @TableField("ifurgent_")
    private String ifurgent;

    /** 阶段名称，逗号分隔（分阶段模板时有值） */
    @ApiModelProperty(value = "阶段名称")
    @TableField("stagenames_")
    private String stagenames;

    /** 批量操作批次ID */
    @ApiModelProperty(value = "批量操作批次ID")
    @TableField("batch_id_")
    private String batchId;

    /** 重启原因 */
    @ApiModelProperty(value = "重启原因")
    @TableField("reason_")
    private String reason;

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
