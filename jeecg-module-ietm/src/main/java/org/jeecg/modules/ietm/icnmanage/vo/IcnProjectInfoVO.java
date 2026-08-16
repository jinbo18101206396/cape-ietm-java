package org.jeecg.modules.ietm.icnmanage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: ICN项目信息VO
 * @Author: jeecg-boot
 * @Date: 2026-07-19
 */
@Data
@ApiModel(value = "ICN项目信息VO", description = "用于新增ICN时返回项目相关信息")
public class IcnProjectInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "项目ID")
    private String projectId;

    @ApiModelProperty(value = "项目密级")
    private Integer security;

    @ApiModelProperty(value = "下一个唯一识别码")
    private String uniqueId;

    @ApiModelProperty(value = "SNS编码")
    private String sns;

    @ApiModelProperty(value = "编码规则")
    private String codeRule;
}
