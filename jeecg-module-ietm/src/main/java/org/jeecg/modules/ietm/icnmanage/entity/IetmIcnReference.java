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
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: ICN引用关系表
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 * @Version: V1.0
 */
@Data
@TableName("ietm_icn_reference")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ietm_icn_reference对象", description = "ICN引用关系表")
public class IetmIcnReference implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键*/
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;

    /**源ICN ID（引用方）*/
    @Excel(name = "源ICN ID", width = 15)
    @ApiModelProperty(value = "源ICN ID（引用方）")
    private String sourceIcnId;

    /**目标ICN ID（被引用方）*/
    @Excel(name = "目标ICN ID", width = 15)
    @ApiModelProperty(value = "目标ICN ID（被引用方）")
    private String targetIcnId;

    /**引用类型: ICN_TO_ICN(ICN引用ICN), ICN_TO_DM(ICN被DM引用)*/
    @Excel(name = "引用类型", width = 15)
    @ApiModelProperty(value = "引用类型: ICN_TO_ICN(ICN引用ICN), ICN_TO_DM(ICN被DM引用)")
    private String referenceType;

    /**DM模块编码（当reference_type=ICN_TO_DM时使用）*/
    @Excel(name = "DM模块编码", width = 15)
    @ApiModelProperty(value = "DM模块编码（当reference_type=ICN_TO_DM时使用）")
    private String dmCode;

    /**创建人*/
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /**创建时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /**更新人*/
    @ApiModelProperty(value = "更新人")
    private String updateBy;

    /**更新时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /**备注*/
    @Excel(name = "备注", width = 30)
    @ApiModelProperty(value = "备注")
    private String remark;
}
