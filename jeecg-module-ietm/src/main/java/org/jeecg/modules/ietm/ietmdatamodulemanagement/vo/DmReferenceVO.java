package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 数据模块引用关系VO
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 */
@Data
@ApiModel(value = "DmReferenceVO", description = "数据模块引用关系对象")
public class DmReferenceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "DM ID")
    private String dmId;

    @ApiModelProperty(value = "DMC编码")
    private String dmcCode;

    @ApiModelProperty(value = "技术名称")
    private String techName;

    @ApiModelProperty(value = "信息名称")
    private String infoName;

    @ApiModelProperty(value = "引用类型（dmRef、infoEntityRef等）")
    private String refType;

    @ApiModelProperty(value = "引用深度（用于树形结构展示）")
    private Integer refDepth;

    @ApiModelProperty(value = "是否循环引用")
    private Boolean isCircular;

    @ApiModelProperty(value = "父节点ID")
    private String parentDmId;

    @ApiModelProperty(value = "子引用列表（递归结构）")
    private List<DmReferenceVO> children;
}
