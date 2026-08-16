package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * DM复制请求VO
 * 用于复制新建DM功能
 *
 * @Author: jeecg-boot
 * @Date: 2026-07-23
 */
@Data
@ApiModel(value = "DM复制请求VO", description = "复制新建DM的请求参数")
public class DmCopyVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "源DM的ID", required = true, example = "1234567890")
    @NotBlank(message = "源DM的ID不能为空")
    private String sourceDmId;

    @ApiModelProperty(value = "目标构型节点ID", required = true, example = "0987654321")
    @NotBlank(message = "目标构型节点ID不能为空")
    private String targetCmNodeId;

    @ApiModelProperty(value = "目标构型节点名称", required = true, example = "DDN 动力系统")
    @NotBlank(message = "目标构型节点名称不能为空")
    private String targetCmNodeName;

    // ==================== 前端表单数据（可选，优先使用前端值） ====================

    @ApiModelProperty(value = "SNS编码（前端计算）", example = "DDN-A-A1")
    @Size(max = 20, message = "SNS编码长度不能超过20")
    private String sns;

    @ApiModelProperty(value = "技术名称（前端提取）", example = "动力系统")
    @Size(max = 500, message = "技术名称长度不能超过500")
    private String techName;

    @ApiModelProperty(value = "信息码（前端可修改）", example = "ZBBM02")
    @Size(max = 20, message = "信息码长度不能超过20")
    private String infoCode;

    @ApiModelProperty(value = "信息码变体（前端可修改）", example = "A")
    @Size(max = 1, message = "信息码变体长度不能超过1")
    private String infoCodeVariant;

    @ApiModelProperty(value = "位置码（前端可修改）", example = "A")
    @Size(max = 10, message = "位置码长度不能超过10")
    private String ietmLocationCode;

    @ApiModelProperty(value = "学习码（前端可修改）", example = "001")
    @Size(max = 3, message = "学习码长度不能超过3")
    private String learnCode;

    @ApiModelProperty(value = "学习事件码（前端可修改）", example = "A")
    @Size(max = 1, message = "学习事件码长度不能超过1")
    private String learnEventCode;

    @ApiModelProperty(value = "信息名称（前端可修改）", example = "基本信息")
    @Size(max = 500, message = "信息名称长度不能超过500")
    private String infoName;

    @ApiModelProperty(value = "DM类型（前端可修改）", example = "descript")
    @Size(max = 20, message = "DM类型长度不能超过20")
    private String dmType;

    @ApiModelProperty(value = "语言ISO代码（前端可修改）", example = "ZH")
    @Size(max = 2, message = "语言ISO代码长度不能超过2")
    private String languageIsoCode;

    @ApiModelProperty(value = "国家ISO代码（前端可修改）", example = "CN")
    @Size(max = 2, message = "国家ISO代码长度不能超过2")
    private String countryIsoCode;

    @ApiModelProperty(value = "创作单位代码（前端可修改）", example = "COMP001")
    @Size(max = 50, message = "创作单位代码长度不能超过50")
    private String originator;

    @ApiModelProperty(value = "创作单位名称（前端传递）", example = "某某航空公司")
    @Size(max = 200, message = "创作单位名称长度不能超过200")
    private String originatorName;

    @ApiModelProperty(value = "责任单位代码（前端可修改）", example = "COMP002")
    @Size(max = 50, message = "责任单位代码长度不能超过50")
    private String rpc;

    @ApiModelProperty(value = "责任单位名称（前端传递）", example = "某某维修公司")
    @Size(max = 200, message = "责任单位名称长度不能超过200")
    private String rpcName;

    @ApiModelProperty(value = "版本号（前端可修改）", example = "000")
    @Size(max = 3, message = "版本号长度不能超过3")
    private String issueNo;

    @ApiModelProperty(value = "修订号（前端可修改）", example = "01")
    @Size(max = 2, message = "修订号长度不能超过2")
    private String inWork;

    @ApiModelProperty(value = "密级（前端可修改）", example = "1")
    @Size(max = 20, message = "密级长度不能超过20")
    private String security;

    @ApiModelProperty(value = "发行类型（固定值new）", example = "new")
    @Size(max = 10, message = "发行类型长度不能超过10")
    private String issueType;

    @ApiModelProperty(value = "生产者（DME，前端可修改）", example = "PROD01")
    @Size(max = 50, message = "生产者长度不能超过50")
    private String enterprise;

    @ApiModelProperty(value = "扩展代码（DME，前端可修改）", example = "EXT001")
    @Size(max = 50, message = "扩展代码长度不能超过50")
    private String extraCode;
}
