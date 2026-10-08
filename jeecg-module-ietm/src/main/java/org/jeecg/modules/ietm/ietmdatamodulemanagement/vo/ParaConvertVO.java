package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Para转换请求VO
 *
 * @author IETM Team
 * @date 2026-09-23
 */
@Data
@ApiModel(value = "ParaConvertVO", description = "Para设计器转换请求对象")
public class ParaConvertVO {

    @ApiModelProperty(value = "XML字符串", required = true)
    @NotBlank(message = "XML内容不能为空")
    private String xml;

    @ApiModelProperty(value = "转换方向（para2html/html2para）")
    private String direction;

    @ApiModelProperty(value = "项目参数JSON字符串（html2para时需要，用于公式转ICN）")
    private String projectParameters;
}
