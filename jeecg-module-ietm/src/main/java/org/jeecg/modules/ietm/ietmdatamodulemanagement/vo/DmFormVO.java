package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * @Description: 数据模块表单提交VO
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 */
@Data
@ApiModel(value = "DmFormVO", description = "数据模块表单提交对象")
public class DmFormVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID（新增时为空，编辑时必填）")
    private String id;

    @ApiModelProperty(value = "项目ID", required = true)
    @NotBlank(message = "项目ID不能为空")
    private String projectId;

    @ApiModelProperty(value = "构型节点ID", required = true)
    @NotBlank(message = "构型节点ID不能为空")
    private String cmNodeId;

    @ApiModelProperty(value = "构型节点路径")
    private String cmNodePath;

    @ApiModelProperty(value = "SNS编号", required = true)
    @NotBlank(message = "SNS编号不能为空")
    private String sns;

    @ApiModelProperty(value = "信息代码", required = true)
    @NotBlank(message = "信息代码不能为空")
    private String infoCode;

    @ApiModelProperty(value = "信息代码变体")
    @Pattern(regexp = "^[A-Z]?$", message = "信息代码变体必须为大写字母")
    private String infoCodeVariant;

    @ApiModelProperty(value = "IETM位置码")
    private String ietmLocationCode;

    @ApiModelProperty(value = "学习码")
    @Pattern(regexp = "^(\\d{3})?$", message = "学习码必须为3位数字或为空")
    private String learnCode;

    @ApiModelProperty(value = "学习码事件码")
    @Pattern(regexp = "^[A-Z]?$", message = "学习码事件码必须为大写字母")
    private String learnEventCode;

    @ApiModelProperty(value = "变更年份")
    @Pattern(regexp = "^(\\d{2})?$", message = "变更年份必须为2位数字或为空")
    private String yearOfChange;

    @ApiModelProperty(value = "序列号")
    @Pattern(regexp = "^(\\d{3})?$", message = "序列号必须为3位数字或为空")
    private String seqNo;

    @ApiModelProperty(value = "发行方代码", required = true)
    @NotBlank(message = "发行方代码不能为空")
    @Size(max = 50, message = "发行方代码长度不能超过50")
    private String originator;

    @ApiModelProperty(value = "发行方名称")
    private String originatorName;

    @ApiModelProperty(value = "责任伙伴公司码")
    private String rpc;

    @ApiModelProperty(value = "责任单位名称")
    private String rpcName;

    @ApiModelProperty(value = "技术名称", required = true)
    @NotBlank(message = "技术名称不能为空")
    @Size(max = 500, message = "技术名称长度不能超过500")
    private String techName;

    @ApiModelProperty(value = "信息名称")
    @Size(max = 500, message = "信息名称长度不能超过500")
    private String infoName;

    @ApiModelProperty(value = "技术名称(英文)")
    @Size(max = 500, message = "技术名称(英文)长度不能超过500")
    private String techNameEn;

    @ApiModelProperty(value = "信息名称(英文)")
    @Size(max = 500, message = "信息名称(英文)长度不能超过500")
    private String infoNameEn;

    @ApiModelProperty(value = "Schema")
    private String schema;

    @ApiModelProperty(value = "语言ISO代码", required = true)
    @NotBlank(message = "语言ISO代码不能为空")
    @Pattern(regexp = "^[a-z]{2,3}$", message = "语言ISO代码必须为2-3位小写字母")
    private String languageIsoCode;

    @ApiModelProperty(value = "国家ISO代码", required = true)
    @NotBlank(message = "国家ISO代码不能为空")
    @Pattern(regexp = "^[A-Z]{2}$", message = "国家ISO代码必须为2位大写字母")
    private String countryIsoCode;

    @ApiModelProperty(value = "DM类型")
    private String dmType;

    @ApiModelProperty(value = "密级")
    private String security;

    @ApiModelProperty(value = "DM内容（XML格式）")
    private String dmContent;

    @ApiModelProperty(value = "备注")
    @Size(max = 1000, message = "备注长度不能超过1000")
    private String remark;

    @ApiModelProperty(value = "修改原因")
    @Size(max = 1000, message = "修改原因长度不能超过1000")
    private String reason;
}
