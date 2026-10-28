package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * LevelledPara 验证请求 VO
 */
@Data
@ApiModel(value = "LevelledPara验证请求", description = "LevelledPara验证请求参数")
public class LevelledParaValidateVO {

    @ApiModelProperty(value = "levelledPara XML内容", required = true)
    @NotBlank(message = "XML内容不能为空")
    private String xml;

    @ApiModelProperty(value = "验证级别: basic/strict", example = "basic")
    private String level = "basic";
}
