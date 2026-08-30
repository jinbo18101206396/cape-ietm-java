package org.jeecg.modules.ietm.workflow.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 批量审批结果VO
 * @Author: IETM Team
 * @Date: 2026-08-28
 * @Version: V1.0
 */
@Data
@ApiModel(value = "批量审批结果VO", description = "批量审批的执行结果")
public class BatchApproveResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "成功数量")
    private Integer successCount = 0;

    @ApiModelProperty(value = "失败数量")
    private Integer failedCount = 0;

    @ApiModelProperty(value = "错误列表")
    private List<ApprovalError> errors = new ArrayList<>();

    /**
     * 审批错误项
     */
    @Data
    @ApiModel(value = "审批错误项")
    public static class ApprovalError implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "节点ID")
        private String nodeId;

        @ApiModelProperty(value = "错误信息")
        private String errorMessage;

        public ApprovalError(String nodeId, String errorMessage) {
            this.nodeId = nodeId;
            this.errorMessage = errorMessage;
        }
    }
}
