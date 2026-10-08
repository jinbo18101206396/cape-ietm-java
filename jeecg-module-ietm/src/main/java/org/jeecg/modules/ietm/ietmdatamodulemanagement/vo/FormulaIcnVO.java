package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 公式ICN请求VO
 * 包含公式转换为ICN所需的11个字段
 *
 * @author IETM Team
 * @date 2026-09-23
 */
@Data
@ApiModel(value = "FormulaIcnVO", description = "公式转ICN请求对象")
public class FormulaIcnVO {

    @ApiModelProperty(value = "ICN编码", required = true, example = "ICN-12345-67890-ABC-00001-A-001-01")
    @NotBlank(message = "ICN编码不能为空")
    private String icn;

    @ApiModelProperty(value = "SNS编码（5段）", required = true, example = "12345-67890-ABCDE-12345-67890")
    @NotBlank(message = "SNS编码不能为空")
    private String sns;

    @ApiModelProperty(value = "RPC编码", required = true)
    @NotBlank(message = "RPC编码不能为空")
    private String rpc;

    @ApiModelProperty(value = "Originator编码", required = true)
    @NotBlank(message = "Originator编码不能为空")
    private String originator;

    @ApiModelProperty(value = "UniqueID（5位）", required = true)
    @NotBlank(message = "UniqueID不能为空")
    private String uniqueid;

    @ApiModelProperty(value = "变体代码", example = "A")
    private String variantcode;

    @ApiModelProperty(value = "安全分类", example = "02")
    private String securityclassification;

    @ApiModelProperty(value = "发行号", example = "001")
    private String issueno;

    @ApiModelProperty(value = "密级", example = "1")
    private String secretLevel;

    @ApiModelProperty(value = "构型节点ID", required = true)
    @NotBlank(message = "构型节点ID不能为空")
    private String cmnodeid;

    @ApiModelProperty(value = "文件名", required = true, example = "公式00001.png")
    @NotBlank(message = "文件名不能为空")
    private String filename;

    @ApiModelProperty(value = "文件内容（Base64或HTML）", required = true)
    @NotBlank(message = "文件内容不能为空")
    private String filecontent;
}
