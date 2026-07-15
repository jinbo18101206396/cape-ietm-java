package org.jeecg.modules.ietm.projectmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecg.modules.ietm.ietmprojectcompany.entity.IetmProjectCompany;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Description: 手册管理-手册项目管理列表
 * @Author: jeecg-boot
 * @Date: 2026-01-17
 * @Version: V1.0
 */
@ApiModel(value = "ietm_project对象", description = "手册管理-手册项目管理列表")
@Data
@TableName("ietm_project")
public class IetmProject implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;
    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private String createBy;
    /**
     * 创建日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private Date createTime;
    /**
     * 更新人
     */
    @ApiModelProperty(value = "更新人")
    private String updateBy;
    /**
     * 更新日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private Date updateTime;
    /**
     * 所属部门
     */
    @ApiModelProperty(value = "所属部门")
    private String sysOrgCode;
    /**
     * 名称
     */
    @Excel(name = "名称", width = 15)
    @ApiModelProperty(value = "名称")
    private String name;
    /**
     * 描述
     */
    @Excel(name = "描述", width = 15)
    @ApiModelProperty(value = "描述")
    private String description;
    /**
     * 编码规则
     */
    @Excel(name = "编码规则", width = 15)
    @ApiModelProperty(value = "编码规则")
    private String codeRule;
    /**
     * 业务规则模板
     */
    @Excel(name = "业务规则模板", width = 15)
    @ApiModelProperty(value = "业务规则模板")
    private String businessRuleTemp;
    /**
     * 组织
     */
    @Excel(name = "组织", width = 15)
    @ApiModelProperty(value = "组织")
    private String organization;
    /**
     * 装备编码
     */
    @Excel(name = "装备编码", width = 15)
    @ApiModelProperty(value = "装备编码")
    private String equipmentCode;
    /**
     * 最后修改的ip
     */
    @Excel(name = "最后修改的ip", width = 15)
    @ApiModelProperty(value = "最后修改的ip")
    private String updateIp;
    /**
     * 版本
     */
    @Excel(name = "版本", width = 15)
    @ApiModelProperty(value = "版本")
    private String version;
    /**
     * 状态
     */
    @Excel(name = "状态", width = 15)
    @ApiModelProperty(value = "状态")
    private Integer status;
    /**
     * 密级
     */
    @Excel(name = "密级", width = 15, dicCode = "security")
    @Dict(dicCode = "security")
    @ApiModelProperty(value = "密级")
    private Integer security;
    /**
     * IETM标准
     */
    @Excel(name = "IETM标准", width = 15)
    @ApiModelProperty(value = "IETM标准")
    private String ietmStandard;
    /**
     * 商业和政府机构编码
     */
    @Excel(name = "商业和政府机构编码", width = 15)
    @ApiModelProperty(value = "商业和政府机构编码")
    private String cageCode;
    /**
     * 位置码
     */
    @Excel(name = "位置码", width = 15)
    @ApiModelProperty(value = "位置码")
    private String positionCode;
    /**
     * 默认业务规则
     */
    @Excel(name = "默认业务规则", width = 15)
    @ApiModelProperty(value = "默认业务规则")
    private String defaultBusinessRule;
    /**
     * 语言
     */
    @Excel(name = "语言", width = 15)
    @ApiModelProperty(value = "语言")
    private String lanuageCode;
    /**
     * 国家
     */
    @Excel(name = "国家", width = 15)
    @ApiModelProperty(value = "国家")
    private String countryCode;

    @TableField(exist = false)
    private List<IetmProjectCompany> projectCompany;
}
