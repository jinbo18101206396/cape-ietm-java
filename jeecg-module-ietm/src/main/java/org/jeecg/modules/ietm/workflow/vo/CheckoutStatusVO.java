package org.jeecg.modules.ietm.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 签出状态VO
 * @Author: IETM Team
 * @Date: 2026-08-28
 * @Version: V1.0
 */
@Data
@ApiModel(value = "签出状态VO", description = "DM/PM的签出状态")
public class CheckoutStatusVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "签出用户（null表示已签入）")
    private String checkoutUser;

    @ApiModelProperty(value = "签出时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkoutTime;
}
