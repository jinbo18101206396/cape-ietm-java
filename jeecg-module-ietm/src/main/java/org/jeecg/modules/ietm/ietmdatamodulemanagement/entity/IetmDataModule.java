package org.jeecg.modules.ietm.ietmdatamodulemanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Date;

/**
 * @Description: IETM数据模块管理
 * @Author: jeecg-boot
 * @Date: 2026-07-22
 * @Version: V3.0
 */
@Data
@TableName("ietm_data_module")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ietm_data_module对象", description = "IETM数据模块管理")
public class IetmDataModule implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键ID")
    private String id;

    /** 项目ID */
    @Excel(name = "项目ID", width = 15)
    @ApiModelProperty(value = "项目ID", required = true)
    @NotBlank(message = "项目ID不能为空")
    @TableField("project_id")
    private String projectId;

    /** 项目名称 */
    @Excel(name = "项目名称", width = 20)
    @ApiModelProperty(value = "项目名称")
    @TableField("project_name")
    private String projectName;

    /** DMC第1段：模式代码（默认J，不落库，仅用于拼接DMC编码）*/
    @Excel(name = "Schema", width = 15)
    @ApiModelProperty(value = "DMC第1段-模式代码（默认J）")
    @TableField(exist = false)
    private String schema;

    /** DMC第2段：SNS编号 */
    @Excel(name = "SNS", width = 20)
    @ApiModelProperty(value = "DMC第2段-系统编号码", required = true)
    @NotBlank(message = "SNS编号不能为空")
    @TableField("sns")
    private String sns;

    /** DMC第3段：信息代码 */
    @Excel(name = "信息码", width = 15)
    @ApiModelProperty(value = "DMC第3段-信息代码", required = true)
    @NotBlank(message = "信息码不能为空")
    @Size(min = 3, max = 3, message = "信息码长度必须为3位")
    @TableField("info_code")
    private String infoCode;

    /** DMC第4段：信息代码变体（A-Z）*/
    @Excel(name = "信息码变体", width = 10)
    @ApiModelProperty(value = "DMC第4段-信息代码变体（A-Z大写字母）")
    @Pattern(regexp = "^[A-Z]?$", message = "信息码变体必须是A-Z的大写字母或为空")
    @TableField("info_code_variant")
    private String infoCodeVariant;

    /** DMC第5段：IETM位置码 */
    @Excel(name = "IETM位置码", width = 15, dicCode = "dm_location_code")
    @Dict(dicCode = "dm_location_code")
    @ApiModelProperty(value = "DMC第5段-IETM位置码（A/B/C/D/T）")
    @Pattern(regexp = "^[ABCDT]?$", message = "位置码只能是A/B/C/D/T")
    @TableField("ietm_location_code")
    private String ietmLocationCode;

    /** DMC第6段：学习码（000-999）*/
    @Excel(name = "学习码", width = 15)
    @ApiModelProperty(value = "DMC第6段-学习码（000-999）")
    @Pattern(regexp = "^([0-9]{3})?$", message = "学习码必须是000-999的3位数字")
    @TableField("learn_code")
    private String learnCode;

    /** DMC第7段：学习事件码（A-Z）*/
    @Excel(name = "学习事件码", width = 15)
    @ApiModelProperty(value = "DMC第7段-学习事件码（A-Z）")
    @Pattern(regexp = "^[A-Z]?$", message = "学习事件码必须是A-Z的大写字母")
    @TableField("learn_code_event_code")
    private String learnCodeEventCode;

    /** DMC第8段：变更年代码 */
    @Excel(name = "变更年代码", width = 10)
    @ApiModelProperty(value = "DMC第8段-变更年代码（年份后2位，如26=2026年）")
    @TableField("year_of_change")
    private String yearOfChange;

    /** DMC第9段：顺序码（001-999）*/
    @Excel(name = "顺序码", width = 15)
    @ApiModelProperty(value = "DMC第9段-顺序码（001-999）")
    @TableField("seq_no")
    private String seqNo;

    /** DMC第10段：语言代码（ISO 639-1）*/
    @Excel(name = "语言ISO代码", width = 10, dicCode = "language")
    @Dict(dicCode = "language")
    @ApiModelProperty(value = "DMC第10段-语言代码（ISO 639-1，如ZH）")
    @TableField("language_iso_code")
    private String languageIsoCode;

    /** DMC第11段：国家代码（ISO 3166-1）*/
    @Excel(name = "国家ISO代码", width = 10, dicCode = "country")
    @Dict(dicCode = "country")
    @ApiModelProperty(value = "DMC第11段-国家代码（ISO 3166-1，如CN）")
    @TableField("country_iso_code")
    private String countryIsoCode;

    /** 发行方代码 */
    @Excel(name = "发行方代码", width = 15)
    @ApiModelProperty(value = "发行方代码（责任单位代码）", required = true)
    @NotBlank(message = "发行方代码不能为空")
    @Size(max = 50, message = "发行方代码长度不能超过50")
    @TableField("originator")
    private String originator;

    /** 发行方名称 */
    @Excel(name = "发行方名称", width = 20)
    @ApiModelProperty(value = "发行方名称")
    @TableField("originator_name")
    private String originatorName;

    /** 责任伙伴公司码 */
    @Excel(name = "责任伙伴公司码", width = 15, dicCode = "dm_rpc_type")
    @Dict(dicCode = "dm_rpc_type")
    @ApiModelProperty(value = "责任伙伴公司码")
    @TableField("rpc")
    private String rpc;

    /** 责任伙伴公司名称 */
    @Excel(name = "责任伙伴公司名称", width = 20)
    @ApiModelProperty(value = "责任伙伴公司名称")
    @TableField("rpc_name")
    private String rpcName;

    /** 发行编号（001-999）*/
    @Excel(name = "发行编号", width = 10)
    @ApiModelProperty(value = "发行编号（001-999）", required = true, example = "001")
    @NotBlank(message = "发行编号不能为空")
    @Pattern(regexp = "^(0[0-9]{2}|[1-9][0-9]{2})$", message = "发行编号必须为001-999的3位数字")
    @TableField("issue_no")
    private String issueNo;

    /** 在编版本号（00-99）*/
    @Excel(name = "在编版本号", width = 10)
    @ApiModelProperty(value = "在编版本号（00-99）", required = true, example = "00")
    @NotBlank(message = "在编版本号不能为空")
    @Pattern(regexp = "^[0-9]{2}$", message = "在编版本号必须为00-99的2位数字")
    @TableField("in_work")
    private String inWork;

    /** 版本类型（0=草稿 1=已发布）*/
    @Excel(name = "版本类型", width = 10, dicCode = "dm_version_type")
    @Dict(dicCode = "dm_version_type")
    @ApiModelProperty(value = "版本类型（0=草稿 1=已发布）")
    @TableField("version_type")
    private String versionType;

    /** 技术名称 */
    @Excel(name = "技术名称", width = 30)
    @ApiModelProperty(value = "技术名称")
    @TableField("tech_name")
    private String techName;

    /** 信息名称 */
    @Excel(name = "信息名称", width = 30)
    @ApiModelProperty(value = "信息名称")
    @TableField("info_name")
    private String infoName;

    /** 技术名称（英文）*/
    @Excel(name = "技术名称(英文)", width = 30)
    @ApiModelProperty(value = "技术名称（英文）")
    @TableField("tech_name_en")
    private String techNameEn;

    /** 信息名称（英文）*/
    @Excel(name = "信息名称(英文)", width = 30)
    @ApiModelProperty(value = "信息名称（英文）")
    @TableField("info_name_en")
    private String infoNameEn;

    /** 构型节点ID */
    @Excel(name = "构型节点ID", width = 15)
    @ApiModelProperty(value = "构型节点ID", required = true)
    @NotBlank(message = "构型节点不能为空")
    @TableField("cm_node_id")
    private String cmNodeId;

    /** 构型节点名称 */
    @Excel(name = "构型节点名称", width = 20)
    @ApiModelProperty(value = "构型节点名称")
    @TableField("cm_node_name")
    private String cmNodeName;

    /** 构型节点路径 */
    @Excel(name = "构型节点路径", width = 50)
    @ApiModelProperty(value = "构型节点路径（格式：/项目/子系统/组件）")
    @TableField("cm_node_path")
    private String cmNodePath;

    /** 是否最新版本（1=是 0=否）*/
    @Excel(name = "是否最新版本", width = 10)
    @ApiModelProperty(value = "是否最新版本（1=是 0=历史版本）")
    @TableField("is_latest")
    private String isLatest;

    /** 状态（1=正常 0=已删除）*/
    @Excel(name = "状态", width = 10, dicCode = "dm_status")
    @Dict(dicCode = "dm_status")
    @ApiModelProperty(value = "状态（1=正常 0=已删除）")
    @TableField("status")
    private String status;

    /** 签出用户 */
    @Excel(name = "签出用户", width = 15)
    @ApiModelProperty(value = "签出用户（非空表示已签出，其他用户不可编辑）")
    @TableField("checkout_user")
    private String checkoutUser;

    /** 签出时间 */
    @Excel(name = "签出时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "签出时间")
    @TableField("checkout_time")
    private Date checkoutTime;

    /** 签入时间 */
    @Excel(name = "签入时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "签入时间")
    @TableField("checkin_time")
    private Date checkinTime;

    /** 发布日期 */
    @Excel(name = "发布日期", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "发布日期")
    @TableField("publish_date")
    private Date publishDate;

    /** 工作流实例ID */
    @Excel(name = "工作流实例ID", width = 20)
    @ApiModelProperty(value = "工作流实例ID（关联Activiti）")
    @TableField("workflow_instance_id")
    private String workflowInstanceId;

    /** 工作流状态 */
    @Excel(name = "工作流状态", width = 10, dicCode = "workflow_status")
    @Dict(dicCode = "workflow_status")
    @ApiModelProperty(value = "工作流状态")
    @TableField("workflow_status")
    private String workflowStatus;

    /** 当前流程节点 */
    @Excel(name = "当前流程节点", width = 15)
    @ApiModelProperty(value = "当前流程节点名称")
    @TableField("workflow_step")
    private String workflowStep;

    /** 当前处理人 */
    @Excel(name = "当前处理人", width = 15)
    @ApiModelProperty(value = "当前流程处理人用户名")
    @TableField("workflow_handler")
    private String workflowHandler;

    /** DM内容（XML格式）*/
    @ApiModelProperty(value = "DM内容（XML格式大文本）")
    @TableField("dm_content")
    private String dmContent;

    /** DM类型 */
    @Excel(name = "DM类型", width = 15, dicCode = "dm_type")
    @Dict(dicCode = "dm_type")
    @ApiModelProperty(value = "DM类型（描述性/过程性/故障性等）")
    @TableField("dm_type")
    private String dmType;

    /** 密级（0-5）*/
    @Excel(name = "密级", width = 10, dicCode = "security")
    @Dict(dicCode = "security")
    @ApiModelProperty(value = "密级（0=公开 1=内部 2=秘密 3=机密 4=绝密 5=核心绝密）")
    @TableField("security")
    private String security;

    /** 出引用数量 */
    @Excel(name = "出引用数量", width = 10)
    @ApiModelProperty(value = "出引用数量（本DM引用其他DM的数量）")
    @TableField("ref_count")
    private Integer refCount;

    /** 入引用数量 */
    @Excel(name = "入引用数量", width = 10)
    @ApiModelProperty(value = "入引用数量（被其他DM引用的数量）")
    @TableField("refed_count")
    private Integer refedCount;

    /** 备注 */
    @Excel(name = "备注", width = 30)
    @ApiModelProperty(value = "备注")
    @TableField("remark")
    private String remark;

    /** 修改原因 */
    @Excel(name = "修改原因", width = 30)
    @ApiModelProperty(value = "修改原因（版本升级时填写）")
    @TableField("reason")
    private String reason;

    /** 创建人 */
    @ApiModelProperty(value = "创建人")
    @TableField("create_by")
    private String createBy;

    /** 创建时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private Date createTime;

    /** 更新人 */
    @ApiModelProperty(value = "更新人")
    @TableField("update_by")
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private Date updateTime;

    /** 所属部门编码 */
    @ApiModelProperty(value = "所属部门编码（数据权限控制）")
    @TableField("sys_org_code")
    private String sysOrgCode;

    /** 信息码组合（SNS+信息码+变体拼接）*/
    @ApiModelProperty(value = "信息码组合（SNS+info_code+info_code_variant拼接）")
    @TableField("info_code_part")
    private String infoCodePart;

    /** DMC完整编码（11段拼接）*/
    @Excel(name = "DMC完整编码", width = 50)
    @ApiModelProperty(value = "DMC完整编码（11段拼接）")
    @TableField("dmc_code")
    private String dmcCode;

    /** 签发日期 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "正式签发日期")
    @TableField("issue_date")
    private Date issueDate;

    /** 密级名称 */
    @ApiModelProperty(value = "密级名称（如：秘密、机密）")
    @TableField("security_classification")
    private String securityClassification;

    /** 语言名称 */
    @ApiModelProperty(value = "语言名称（如：中文、英文）")
    @TableField("language")
    private String language;
}
