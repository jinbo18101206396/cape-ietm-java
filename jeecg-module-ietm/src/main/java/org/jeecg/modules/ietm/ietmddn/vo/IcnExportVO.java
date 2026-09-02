package org.jeecg.modules.ietm.ietmddn.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * @Description: 导出ICN请求参数（导出实体功能）
 * @Author: jeecg-boot
 * @Date: 2026-09-02
 */
@Data
@ApiModel(value = "导出ICN请求VO", description = "导出实体（ICN）功能的请求参数")
public class IcnExportVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "选中的ICN ID列表", required = true)
    @NotEmpty(message = "ICN列表不能为空")
    private List<String> icnIds;

    @ApiModelProperty(value = "型号")
    private String modelic;

    @ApiModelProperty(value = "密级", required = true)
    @NotEmpty(message = "密级不能为空")
    private String security;

    @ApiModelProperty(value = "商业密级")
    private String commercialSecurity;

    @ApiModelProperty(value = "警告")
    private String caveat;

    @ApiModelProperty(value = "导出单位", required = true)
    @NotEmpty(message = "导出单位不能为空")
    private String sender;

    @ApiModelProperty(value = "接收单位")
    private String receiver;

    @ApiModelProperty(value = "日期 yyyy-MM-dd", required = true)
    @NotEmpty(message = "日期不能为空")
    private String issueDate;
}
