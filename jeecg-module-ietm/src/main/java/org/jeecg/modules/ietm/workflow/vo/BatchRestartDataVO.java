package org.jeecg.modules.ietm.workflow.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @Description: 批量重启单条DM数据VO
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
@Data
@ApiModel(value = "BatchRestartDataVO", description = "重启单条DM数据")
public class BatchRestartDataVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** DM ID */
    @ApiModelProperty(value = "DM ID", required = true)
    @NotBlank(message = "DM ID不能为空")
    private String dmId;

    /** 旧工作流实例ID（需要终止的） */
    @ApiModelProperty(value = "旧工作流实例ID", required = true)
    @NotBlank(message = "旧工作流实例ID不能为空")
    private String oldInstanceId;
}
