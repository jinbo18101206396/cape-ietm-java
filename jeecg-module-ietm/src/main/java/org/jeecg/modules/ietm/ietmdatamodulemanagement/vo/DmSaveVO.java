package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * DM 内容保存请求体（对标 legacy savedm/{id}，需求 §15）
 */
@Data
@ApiModel(value = "DmSaveVO", description = "DM内容保存请求")
public class DmSaveVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "DM的XML正文(英文标签)", required = true)
    @NotBlank(message = "DM内容不能为空")
    private String content;

    @ApiModelProperty(value = "乐观锁版本号(前端加载时携带的version)", required = true)
    @NotNull(message = "缺少版本号，无法保存，请重新加载")
    private Integer version;
}
