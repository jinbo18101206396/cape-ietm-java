package org.jeecg.modules.ietm.ietmddn.entity;

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
 * @Description: DDN数据交换凭证
 * @Author: jeecg-boot
 * @Date: 2026-09-01
 * @Version: V1.0
 */
@Data
@TableName("ietm_ddn")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ietm_ddn对象", description = "DDN数据交换凭证")
public class IetmDdn implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键ID")
    private String id;

    @ApiModelProperty(value = "所属项目ID")
    @TableField("project_id")
    private String projectId;

    @ApiModelProperty(value = "DDN完整编码")
    @TableField("ddn_code")
    private String ddnCode;

    @ApiModelProperty(value = "型号代码")
    @TableField("model_ident_code")
    private String modelIdentCode;

    @ApiModelProperty(value = "导出单位代码")
    @TableField("sender_ident")
    private String senderIdent;

    @ApiModelProperty(value = "接收单位代码")
    @TableField("receiver_ident")
    private String receiverIdent;

    @ApiModelProperty(value = "发布年份")
    @TableField("year_of_data_issue")
    private String yearOfDataIssue;

    @ApiModelProperty(value = "序列号")
    @TableField("seq_number")
    private String seqNumber;

    @ApiModelProperty(value = "密级")
    @TableField("security")
    private String security;

    @ApiModelProperty(value = "商业密级")
    @TableField("commercial_security")
    private String commercialSecurity;

    @ApiModelProperty(value = "警告")
    @TableField("caveat")
    private String caveat;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "发布日期")
    @TableField("issue_date")
    private Date issueDate;

    @ApiModelProperty(value = "包含的DM ID列表（逗号分隔）")
    @TableField("dm_ids")
    private String dmIds;

    @ApiModelProperty(value = "是否含引用ICN")
    @TableField("include_ref_icn")
    private String includeRefIcn;

    @ApiModelProperty(value = "是否含引用DM")
    @TableField("include_ref_dm")
    private String includeRefDm;

    @ApiModelProperty(value = "是否含DM资源")
    @TableField("include_dm_resource")
    private String includeDmResource;

    @ApiModelProperty(value = "DDN文件相对路径")
    @TableField("ddn_file_path")
    private String ddnFilePath;

    @ApiModelProperty(value = "导出DM数量")
    @TableField("dm_count")
    private Integer dmCount;

    @ApiModelProperty(value = "导出ICN数量")
    @TableField("icn_count")
    private Integer icnCount;

    @ApiModelProperty(value = "状态（1正常0删除）")
    @TableField("status")
    private String status;

    @ApiModelProperty(value = "创建人")
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty(value = "更新人")
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @ApiModelProperty(value = "所属部门")
    @TableField("sys_org_code")
    private String sysOrgCode;
}
