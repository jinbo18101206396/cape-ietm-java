package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 数据模块列表展示VO
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 */
@Data
@ApiModel(value = "DmInfoVO", description = "数据模块列表展示对象")
public class DmInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private String id;

    @ApiModelProperty(value = "完整DMC编码")
    private String dmcCode;

    @ApiModelProperty(value = "项目ID")
    private String projectId;

    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @ApiModelProperty(value = "构型节点ID")
    private String cmNodeId;

    @ApiModelProperty(value = "构型节点名称")
    private String cmNodeName;

    @ApiModelProperty(value = "技术名称")
    private String techName;

    @ApiModelProperty(value = "信息名称")
    private String infoName;

    @ApiModelProperty(value = "版本号（如001-00）")
    private String versionInfo;

    @ApiModelProperty(value = "发行编号")
    private String issueNo;

    @ApiModelProperty(value = "在编编号")
    private String inWork;

    @ApiModelProperty(value = "是否最新版本")
    private String isLatest;

    @ApiModelProperty(value = "状态")
    private String status;

    @ApiModelProperty(value = "签出用户")
    private String checkoutUser;

    @ApiModelProperty(value = "签出时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkoutTime;

    @ApiModelProperty(value = "工作流状态")
    private String workflowStatus;

    @ApiModelProperty(value = "工作流实例ID")
    private String workflowInstanceId;

    @ApiModelProperty(value = "当前处理人")
    private String workflowHandler;

    @ApiModelProperty(value = "语言ISO代码")
    private String languageIsoCode;

    @ApiModelProperty(value = "国家ISO代码")
    private String countryIsoCode;

    @ApiModelProperty(value = "DM类型")
    private String dmType;

    @ApiModelProperty(value = "密级")
    private String security;

    @ApiModelProperty(value = "出引用数量")
    private Integer refCount;

    @ApiModelProperty(value = "入引用数量")
    private Integer refedCount;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @ApiModelProperty(value = "是否可编辑（前端计算）")
    private Boolean editable;

    @ApiModelProperty(value = "是否可签出（前端计算）")
    private Boolean canCheckOut;

    @ApiModelProperty(value = "是否可签入（前端计算）")
    private Boolean canCheckIn;
}
