package org.jeecg.modules.ietm.workflow.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: 待办项VO
 * @Author: IETM Team
 * @Date: 2026-08-28
 * @Version: V1.0
 */
@Data
@ApiModel(value = "待办项VO", description = "单个待办事项的展示数据")
public class TodoItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "流程实例ID")
    private String id;

    @ApiModelProperty(value = "业务表单ID（DM或PM的ID）")
    private String formId;

    @ApiModelProperty(value = "完整标题", example = "【DMC-AAAA-00-00-00-00A-040A-A】装备维护手册")
    private String title;

    @ApiModelProperty(value = "流程类型（用于前端排序）")
    private String type;

    @ApiModelProperty(value = "业务页面URL")
    private String url;

    @ApiModelProperty(value = "流程状态：0=草稿,1=流转中,2=已结束,9=强制终止")
    private String status;

    @ApiModelProperty(value = "紧急标识：0=否,1=是")
    private String ifUrgent;

    @ApiModelProperty(value = "创建人姓名")
    private String createdName;

    @ApiModelProperty(value = "创建日期字符串", example = "2026-08-28")
    private String creationDate;

    @ApiModelProperty(value = "节点实例ID（dtlid）")
    private String nodeId;

    @ApiModelProperty(value = "节点名称", example = "审批")
    private String nodeName;

    @ApiModelProperty(value = "签出状态：CHECKED_IN/CHECKED_OUT_BY_ME/CHECKED_OUT_BY_OTHER")
    private String checkoutStatus;

    @ApiModelProperty(value = "签出用户（如果被他人签出）")
    private String checkoutUser;
}
