package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 编辑DM属性请求VO（仅允许修改技术名称和信息名称）
 *
 * @Author: jeecg-boot
 * @Date: 2026-07-23
 */
@Data
@ApiModel(value = "DmEditPropVO", description = "编辑DM属性请求对象")
public class DmEditPropVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "技术名称不能为空")
    @Size(max = 500, message = "技术名称最大500字符")
    @ApiModelProperty(value = "技术名称", required = true)
    private String techName;

    @NotBlank(message = "信息名称不能为空")
    @Size(max = 500, message = "信息名称最大500字符")
    @ApiModelProperty(value = "信息名称", required = true)
    private String infoName;

    @ApiModelProperty(value = "版本类型（S1000D标准：new/changed/revised/status/deleted）", required = false)
    private String issueType;
}
