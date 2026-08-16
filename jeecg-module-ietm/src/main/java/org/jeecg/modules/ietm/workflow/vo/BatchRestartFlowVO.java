package org.jeecg.modules.ietm.workflow.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * @Description: 批量重新启动流程请求VO
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
@Data
@ApiModel(value = "BatchRestartFlowVO", description = "批量重新启动流程请求参数")
public class BatchRestartFlowVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 批量操作批次ID */
    @ApiModelProperty(value = "批量操作批次ID", required = true)
    @NotBlank(message = "批次ID不能为空")
    private String batchId;

    /** 重启原因 */
    @ApiModelProperty(value = "重启原因", required = true)
    @NotBlank(message = "重启原因不能为空")
    private String reason;

    /** DM重启数据列表 */
    @ApiModelProperty(value = "DM重启数据列表", required = true)
    @NotEmpty(message = "DM数据列表不能为空")
    @Valid
    private List<BatchRestartDataVO> dataList;

    /** 流程节点配置列表 */
    @ApiModelProperty(value = "流程节点配置列表", required = true)
    @NotEmpty(message = "流程节点配置不能为空")
    @Valid
    private List<BatchStartFlowDtlVO> nodes;

    /** 紧急级别：1=一般,2=紧急,3=特急 */
    @ApiModelProperty(value = "紧急级别", required = true)
    @NotBlank(message = "紧急级别不能为空")
    private String ifurgent;

    /** 阶段名称（可选） */
    @ApiModelProperty(value = "阶段名称")
    private String stagenames;
}
