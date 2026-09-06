package org.jeecg.modules.ietm.ietmimport.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * DDN信息VO
 *
 * @author IETM Team
 * @date 2026-09-03
 */
@Data
@ApiModel(value = "DDN信息", description = "数据交换凭证信息")
public class DdnInfoVO {

    @ApiModelProperty(value = "型号", required = true, example = "ZB1")
    private String modelic;

    @ApiModelProperty(value = "密级", required = true, example = "01")
    private String security;

    @ApiModelProperty(value = "商业密级", example = "01")
    private String commercialSecurity;

    @ApiModelProperty(value = "警告", example = "01")
    private String caveat;

    @ApiModelProperty(value = "发送单位", required = true, example = "30101")
    private String sender;

    @ApiModelProperty(value = "接收单位", example = "00000")
    private String receiver;

    @ApiModelProperty(value = "日期", required = true, example = "2026-09-03")
    private String issueDate;

    @ApiModelProperty(value = "年份", required = true, example = "2026")
    private Integer year;
}
