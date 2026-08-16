package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: DM项目信息VO
 * @Author: jeecg-boot
 * @Date: 2026-07-22
 */
@Data
@ApiModel(value = "DM项目信息VO", description = "用于新增DM时返回项目相关信息")
public class DmProjectInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "项目ID")
    private String projectId;

    @ApiModelProperty(value = "项目密级")
    private String security;

    @ApiModelProperty(value = "SNS编码")
    private String sns;

    @ApiModelProperty(value = "编码规则")
    private String codeRule;

    @ApiModelProperty(value = "语言ISO代码")
    private String languageIsoCode;

    @ApiModelProperty(value = "国家ISO代码")
    private String countryIsoCode;

    @ApiModelProperty(value = "构型节点技术名称")
    private String techName;

    @ApiModelProperty(value = "创作单位编码")
    private String originator;

    @ApiModelProperty(value = "创作单位名称")
    private String originatorName;

    @ApiModelProperty(value = "责任单位编码")
    private String rpc;

    @ApiModelProperty(value = "责任单位名称")
    private String rpcName;
}
