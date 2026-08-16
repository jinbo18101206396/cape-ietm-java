package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;

/**
 * 校验结果项（对标 legacy validdm 返回的 [{lineno,info}]，需求 §17.5 CONFIRMED）
 */
@Data
@ApiModel(value = "DmValidateItemVO", description = "校验错误项")
public class DmValidateItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("错误所在行号(相对正文,前端加 linenoOffset 定位)")
    private Integer lineno;

    @ApiModelProperty("错误描述")
    private String info;

    public DmValidateItemVO() {}

    public DmValidateItemVO(Integer lineno, String info) {
        this.lineno = lineno;
        this.info = info;
    }
}
