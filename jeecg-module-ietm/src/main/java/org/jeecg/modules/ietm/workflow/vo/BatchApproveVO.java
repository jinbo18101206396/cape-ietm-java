package org.jeecg.modules.ietm.workflow.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * @Description: 批量审批请求VO
 * @Author: IETM Team
 * @Date: 2026-08-28
 * @Version: V1.0
 */
@Data
@ApiModel(value = "批量审批请求VO", description = "批量审批的参数")
public class BatchApproveVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "节点ID列表（dtlid）", required = true)
    @NotEmpty(message = "节点ID列表不能为空")
    private List<String> nodeIds;

    @ApiModelProperty(value = "是否通过：true=通过, false=不同意", required = true)
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @ApiModelProperty(value = "审批意见", required = true)
    @NotEmpty(message = "审批意见不能为空")
    @Size(max = 1000, message = "审批意见最多1000字符")
    private String opinion;
}
