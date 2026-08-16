package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * DMC查重请求VO（优化版）
 * 包含生成完整DMC编码所需的全部字段
 *
 * @Author: jeecg-boot
 * @Date: 2026-07-24
 * @Version: 2.0
 */
@Data
@ApiModel(value = "DMC查重请求VO", description = "DMC唯一性检查参数（完整版）")
public class DmcUniqueCheckVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "制作单位代码", example = "COMPANY")
    private String originator;

    @ApiModelProperty(value = "SNS编码", required = true, example = "SYS01-SUB02-COM03")
    @NotBlank(message = "SNS不能为空")
    private String sns;

    @ApiModelProperty(value = "信息码", required = true, example = "D001")
    @NotBlank(message = "信息码不能为空")
    private String infoCode;

    @ApiModelProperty(value = "信息码变体", example = "A", notes = "默认值：A")
    private String infoCodeVariant;

    @ApiModelProperty(value = "位置码", example = "A", notes = "默认值：A")
    private String ietmLocationCode;

    @ApiModelProperty(value = "学习码（DMC第6段，000-999）", example = "001")
    private String learnCode;

    @ApiModelProperty(value = "学习事件码（DMC第7段，A-Z）", example = "A")
    private String learnEventCode;

    @ApiModelProperty(value = "变更年代码（DMC第8段，年份后2位）", example = "26")
    private String yearOfChange;

    @ApiModelProperty(value = "顺序码（DMC第9段，001-999）", example = "00001")
    private String seqNo;

    @ApiModelProperty(value = "版本号", example = "000", notes = "默认值：000")
    private String issueNo;

    @ApiModelProperty(value = "修订号", example = "01", notes = "默认值：01")
    private String inWork;

    @ApiModelProperty(value = "语言代码", example = "zh", notes = "默认值：zh")
    private String languageIsoCode;

    @ApiModelProperty(value = "国家代码", example = "CN", notes = "默认值：CN")
    private String countryIsoCode;

    @ApiModelProperty(value = "排除的ID（编辑时用，传入当前记录ID）", example = "1234567890")
    private String excludeId;
}
