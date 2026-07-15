package org.jeecg.modules.ietm.icnmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
import java.io.UnsupportedEncodingException;

/**
 * @Description: 项目管理-项目实体管理
 * @Author: jeecg-boot
 * @Date: 2026-02-27
 * @Version: V1.0
 */
@Data
@TableName("ietm_icn_manage")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ietm_icn_manage对象", description = "项目管理-项目实体管理")
public class IetmIcnManage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private java.lang.String id;
    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
    /**
     * 创建日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
    /**
     * 更新人
     */
    @ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
    /**
     * 更新日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;
    /**
     * 构型节点ID
     */
    @Excel(name = "构型节点ID", width = 15)
    @ApiModelProperty(value = "构型节点ID")
    private java.lang.String cmnodeId;
    /**
     * SNS
     */
    @Excel(name = "SNS", width = 15)
    @ApiModelProperty(value = "SNS")
    private java.lang.String sns;
    /**
     * 责任伙伴公司码
     */
    @Excel(name = "责任伙伴公司码", width = 15)
    @ApiModelProperty(value = "责任伙伴公司码")
    private java.lang.String rpc;
    /**
     * 责任单位名称
     */
    @Excel(name = "责任单位名称", width = 15)
    @ApiModelProperty(value = "责任单位名称")
    private java.lang.String rpcName;
    /**
     * 创建者编码
     */
    @Excel(name = "创建者编码", width = 15)
    @ApiModelProperty(value = "创建者编码")
    private java.lang.String originator;
    /**
     * 创作单位名称
     */
    @Excel(name = "创作单位名称", width = 15)
    @ApiModelProperty(value = "创作单位名称")
    private java.lang.String originatorName;
    /**
     * 唯一识别码
     */
    @Excel(name = "唯一识别码", width = 15)
    @ApiModelProperty(value = "唯一识别码")
    private java.lang.String uniqueId;
    /**
     * 变量码
     */
    @Excel(name = "变量码", width = 15)
    @ApiModelProperty(value = "变量码")
    private java.lang.String variantCode;
    /**
     * 版本号
     */
    @Excel(name = "版本号", width = 15)
    @ApiModelProperty(value = "版本号")
    private java.lang.String issueNo;
    /**
     * 安全等级
     */
    @Excel(name = "安全等级", width = 15)
    @ApiModelProperty(value = "安全等级")
    private java.lang.String securityClassification;
    /**
     * 分类
     */
    @Excel(name = "分类", width = 15)
    @ApiModelProperty(value = "分类")
    private java.lang.String icnType;
    /**
     * 是否发布
     */
    @Excel(name = "是否发布", width = 15)
    @ApiModelProperty(value = "是否发布")
    private java.lang.String ispublished;
    /**
     * 是否删除
     */
    @Excel(name = "是否删除", width = 15)
    @ApiModelProperty(value = "是否删除")
    private java.lang.String isdeleted;

    /**
     * 版本
     */
    @Excel(name = "版本", width = 15)
    @ApiModelProperty(value = "版本")
    private java.lang.String version;
    /**
     * 组织
     */
    @Excel(name = "组织", width = 15)
    @ApiModelProperty(value = "组织")
    private java.lang.String orgIdentity;

    /**密级*/
    @Excel(name = "密级", width = 15, dicCode = "security")
    @Dict(dicCode = "security")
    @ApiModelProperty(value = "密级")
    private java.lang.Integer security;

    /**
     * 附件信息
     */
    @TableField(exist = false)
    private IetmAttachment ietmAttachment;

    /**
     *   关联文件信息
     */
    @TableField(exist = false)
    private IetmAttachment relatedIetmAttachment;


    /**
     * ICN
     */
    @TableField(exist = false)
    private String icn;

}
