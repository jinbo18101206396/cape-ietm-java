package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 数据模块校验结果VO
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 */
@Data
@ApiModel(value = "DmValidationVO", description = "数据模块校验结果对象")
public class DmValidationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "校验结果（true-通过，false-失败）")
    private Boolean valid;

    @ApiModelProperty(value = "错误数量")
    private Integer errorCount;

    @ApiModelProperty(value = "警告数量")
    private Integer warningCount;

    @ApiModelProperty(value = "提示数量")
    private Integer infoCount;

    @ApiModelProperty(value = "错误列表")
    private List<ValidationError> errors;

    @ApiModelProperty(value = "校验时间")
    private String validateTime;

    @ApiModelProperty(value = "校验规则版本")
    private String ruleVersion;

    /**
     * 校验错误详情
     */
    @Data
    @ApiModel(value = "ValidationError", description = "校验错误详情")
    public static class ValidationError implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "错误级别（ERROR、WARNING、INFO）")
        private String level;

        @ApiModelProperty(value = "错误代码")
        private String code;

        @ApiModelProperty(value = "错误信息")
        private String message;

        @ApiModelProperty(value = "错误位置（XML行号）")
        private Integer lineNumber;

        @ApiModelProperty(value = "错误位置（XML列号）")
        private Integer columnNumber;

        @ApiModelProperty(value = "建议修复方案")
        private String suggestion;
    }
}
