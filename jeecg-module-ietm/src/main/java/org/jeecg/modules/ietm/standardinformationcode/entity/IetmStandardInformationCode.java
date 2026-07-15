package org.jeecg.modules.ietm.standardinformationcode.entity;

import com.baomidou.mybatisplus.annotation.*;
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
 * @Description: 手册预置模板-信息码管理
 * @Author: jeecg-boot
 * @Date: 2026-07-13
 * @Version: V1.0
 */
@Data
@TableName("ietm_standard_information_code")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ietm_standard_information_code对象", description = "手册预置模板-信息码管理")
public class IetmStandardInformationCode implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;

    /**
     * 手册标准
     */
    @Excel(name = "手册标准", width = 15, dicCode = "standard_type")
    @Dict(dicCode = "standard_type")
    @ApiModelProperty(value = "手册标准")
    private String standard;

    /**
     * 信息码编码
     */
    @Excel(name = "信息码编码", width = 15)
    @ApiModelProperty(value = "信息码编码")
    private String infoCode;

    /**
     * 数据模块类型ID
     */
    @ApiModelProperty(value = "数据模块类型ID")
    private String dmtypeId;

    /**
     * 数据模块类型名称
     */
    @Excel(name = "数据模块类型", width = 15)
    @ApiModelProperty(value = "数据模块类型名称")
    private String dmtypeName;

    /**
     * 描述
     */
    @Excel(name = "描述", width = 30)
    @ApiModelProperty(value = "描述")
    private String description;

    /**
     * 备注
     */
    @Excel(name = "备注", width = 30)
    @ApiModelProperty(value = "备注")
    private String remark;

    /**
     * 密级
     */
    @Excel(name = "密级", width = 15, dicCode = "security")
    @Dict(dicCode = "security")
    @ApiModelProperty(value = "密级")
    private Integer security;

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
     * 最后修改IP
     */
    @ApiModelProperty(value = "最后修改IP")
    private String updateIp;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号")
    private Integer version;
}
