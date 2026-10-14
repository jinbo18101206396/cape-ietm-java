package org.jeecg.modules.ietm.icnmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * IETM公式ICN实体
 *
 * 设计目的：
 * - 绕过DM8聚集索引限制（ietm_icn_manage表有CLUSTER索引无法添加CLOB字段）
 * - 存储公式的完整HTML内容（<img>标签含base64图片和LaTeX源码）
 * - 与图符ICN完全隔离，互不影响
 *
 * 对标旧系统：
 * - 旧系统Oracle/MySQL使用ietm_icn_manage.filecontent BLOB存储
 * - 新系统DM8因聚集索引限制，改用独立表存储
 *
 * @author IETM Team
 * @date 2026-10-14
 */
@Data
@TableName("ietm_icn_formula")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ietm_icn_formula对象", description="IETM公式ICN存储表")
public class IetmIcnFormula implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键ID")
    private String id;

    /** ICN完整编码 */
    @Excel(name = "ICN编码", width = 30)
    @ApiModelProperty(value = "ICN完整编码")
    private String icn;

    /** SNS编码 */
    @Excel(name = "SNS编码", width = 15)
    @ApiModelProperty(value = "SNS编码")
    private String sns;

    /** RPC代码 */
    @Excel(name = "RPC代码", width = 10)
    @ApiModelProperty(value = "RPC代码")
    private String rpc;

    /** 发起者代码 */
    @Excel(name = "发起者代码", width = 10)
    @ApiModelProperty(value = "发起者代码")
    private String originator;

    /** 唯一识别码 */
    @Excel(name = "唯一识别码", width = 10)
    @ApiModelProperty(value = "唯一识别码")
    private String uniqueId;

    /** 变体代码 */
    @Excel(name = "变体代码", width = 10)
    @ApiModelProperty(value = "变体代码")
    private String variantCode;

    /** 安全分类 */
    @Excel(name = "安全分类", width = 10)
    @ApiModelProperty(value = "安全分类")
    private String securityClassification;

    /** 发行号 */
    @Excel(name = "发行号", width = 10)
    @ApiModelProperty(value = "发行号")
    private String issueNo;

    /** 密级 */
    @Excel(name = "密级", width = 10)
    @Dict(dicCode = "secret_level")
    @ApiModelProperty(value = "密级")
    private Integer security;

    /** 构型节点ID */
    @Excel(name = "构型节点ID", width = 20)
    @ApiModelProperty(value = "构型节点ID")
    private String cmNodeId;

    /** 公式HTML内容（完整<img>标签，含base64图片和data-latex属性） */
    @ApiModelProperty(value = "公式HTML内容")
    private byte[] filecontent;

    /** ICN类型（固定为formula） */
    @Excel(name = "ICN类型", width = 10)
    @ApiModelProperty(value = "ICN类型")
    private String icnType;

    /** 版本号 */
    @Excel(name = "版本号", width = 10)
    @ApiModelProperty(value = "版本号")
    private String version;

    /** 是否已发布（0：否，1：是） */
    @Excel(name = "是否已发布", width = 10)
    @ApiModelProperty(value = "是否已发布")
    private String ispublished;

    /** 是否已删除（0：否，1：是） */
    @Excel(name = "是否已删除", width = 10)
    @ApiModelProperty(value = "是否已删除")
    private String isdeleted;

    /** 创建人 */
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /** 创建时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新人 */
    @ApiModelProperty(value = "更新人")
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 组织机构编码 */
    @ApiModelProperty(value = "组织机构编码")
    private String sysOrgCode;
}
