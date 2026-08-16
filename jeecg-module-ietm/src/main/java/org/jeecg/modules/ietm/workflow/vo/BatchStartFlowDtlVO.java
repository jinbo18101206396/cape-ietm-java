package org.jeecg.modules.ietm.workflow.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Description: 批量启动流程节点配置VO
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
@Data
@ApiModel(value = "BatchStartFlowDtlVO", description = "流程节点配置")
public class BatchStartFlowDtlVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 节点顺序号（前端维护，创建节点seqno=0） */
    @ApiModelProperty(value = "节点顺序号", required = true)
    @NotNull(message = "节点顺序号不能为空")
    private Integer seqno;

    /** 节点名称 */
    @ApiModelProperty(value = "节点名称", required = true)
    @NotBlank(message = "节点名称不能为空")
    private String nodename;

    /** 节点类型：0=创建节点/所有人必完成，其他值见字典 */
    @ApiModelProperty(value = "节点类型", required = true)
    @NotBlank(message = "节点类型不能为空")
    private String nodetype;

    /** 处理人ID串（逗号分隔，支持前缀：dpt_/rol_/pst_/grp_） */
    @ApiModelProperty(value = "处理人ID串", required = true)
    @NotBlank(message = "处理人ID不能为空")
    private String userid;

    /** 处理人名称串（逗号分隔） */
    @ApiModelProperty(value = "处理人名称串", required = true)
    @NotBlank(message = "处理人名称不能为空")
    private String useridname;

    /** 所属阶段索引（分阶段模板时使用） */
    @ApiModelProperty(value = "所属阶段索引")
    private String stagename;

    /** 可跳转节点ID串（空=不限制，-1=不可跳转） */
    @ApiModelProperty(value = "可跳转节点ID串")
    private String ifgetback;
}
