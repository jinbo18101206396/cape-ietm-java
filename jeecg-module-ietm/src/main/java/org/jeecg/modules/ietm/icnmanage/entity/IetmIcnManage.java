package org.jeecg.modules.ietm.icnmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Description: 项目管理-项目实体管理
 * @Author: jeecg-boot
 * @Date: 2026-07-19
 * @Version: V2.0
 */
@Data
@TableName("ietm_icn_manage")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ietm_icn_manage对象", description = "项目管理-项目实体管理")
public class IetmIcnManage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键*/
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;

    /**创建人*/
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /**创建日期*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private Date createTime;

    /**更新人*/
    @ApiModelProperty(value = "更新人")
    private String updateBy;

    /**更新日期*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private Date updateTime;

    /**构型节点ID*/
    @Excel(name = "构型节点ID", width = 15)
    @ApiModelProperty(value = "构型节点ID")
    private String cmnodeId;

    /**SNS编码*/
    @Excel(name = "SNS", width = 20)
    @ApiModelProperty(value = "SNS系统编号")
    private String sns;

    /**责任伙伴公司码*/
    @Excel(name = "责任伙伴公司码", width = 15)
    @ApiModelProperty(value = "责任伙伴公司码")
    private String rpc;

    /**责任单位名称*/
    @Excel(name = "责任单位名称", width = 20)
    @ApiModelProperty(value = "责任单位名称")
    private String rpcName;

    /**创作者编码*/
    @Excel(name = "创作者编码", width = 15)
    @ApiModelProperty(value = "创作者编码")
    private String originator;

    /**创作单位名称*/
    @Excel(name = "创作单位名称", width = 20)
    @ApiModelProperty(value = "创作单位名称")
    private String originatorName;

    /**唯一识别码*/
    @Excel(name = "唯一识别码", width = 10)
    @ApiModelProperty(value = "唯一识别码（5位数字）")
    private String uniqueId;

    /**变量码*/
    @Excel(name = "变量码", width = 10)
    @ApiModelProperty(value = "变量码（A-Z）")
    private String variantCode;

    /**版本号*/
    @Excel(name = "版本号", width = 10)
    @ApiModelProperty(value = "版本号")
    private String issueNo;

    /**安全等级*/
    @Excel(name = "安全等级", width = 15)
    @ApiModelProperty(value = "安全等级")
    private String securityClassification;

    /**密级*/
    @Excel(name = "密级", width = 15, dicCode = "security")
    @Dict(dicCode = "security")
    @ApiModelProperty(value = "密级")
    private Integer security;

    /**分类*/
    @Excel(name = "分类", width = 15)
    @ApiModelProperty(value = "ICN分类")
    private String icnType;

    /**是否发布*/
    @Excel(name = "是否发布", width = 10)
    @ApiModelProperty(value = "是否发布")
    private String ispublished;

    /**是否删除*/
    @Excel(name = "是否删除", width = 10)
    @ApiModelProperty(value = "是否删除")
    @TableLogic
    private String isdeleted;

    /**版本*/
    @Excel(name = "版本", width = 10)
    @ApiModelProperty(value = "版本")
    private String version;

    /**组织*/
    @Excel(name = "组织", width = 15)
    @ApiModelProperty(value = "组织")
    private String orgIdentity;

    /**ICN完整编码*/
    @Excel(name = "ICN编码", width = 30)
    @ApiModelProperty(value = "ICN完整编码")
    private String icn;

    /**实体附件信息（不持久化）*/
    @TableField(exist = false)
    private IetmAttachment ietmAttachment;

    /**相关附件信息（不持久化）*/
    @TableField(exist = false)
    private IetmAttachment relatedIetmAttachment;

    /**附件列表（不持久化）*/
    @TableField(exist = false)
    private List<IetmAttachment> attachmentList;

    /**批量新增数量（不持久化，仅用于前端传参）*/
    @TableField(exist = false)
    private Integer count;
}
