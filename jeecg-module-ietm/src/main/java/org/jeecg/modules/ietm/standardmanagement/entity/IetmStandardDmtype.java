package org.jeecg.modules.ietm.standardmanagement.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import org.jeecg.common.aspect.annotation.Dict;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import java.util.Date;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.UnsupportedEncodingException;

/**
 * @Description: 手册管理-标准管理列表（标准数据模块）
 * @Author: jeecg-boot
 * @Date:   2026-01-08
 * @Version: V1.0
 */
@Data
@TableName("ietm_standard_dmtype")
@ApiModel(value="ietm_standard_dmtype对象", description="手册管理-标准管理列表（标准数据模块）")
public class IetmStandardDmtype implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private java.lang.String id;
	/**创建人*/
    @ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
	/**更新人*/
    @ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
	/**更新日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;
	/**所属部门*/
    @ApiModelProperty(value = "所属部门")
    private java.lang.String sysOrgCode;
	/**标准id*/
    @ApiModelProperty(value = "标准id")
    private java.lang.String pid;
	/**DM类型名称*/
	@Excel(name = "DM类型名称", width = 15)
    @ApiModelProperty(value = "DM类型名称")
    private java.lang.String dmtypeName;
	/**DTD*/
	@Excel(name = "DTD", width = 15)
    @ApiModelProperty(value = "DTD")
    private java.lang.String dtd;
	/**描述*/
	@Excel(name = "描述", width = 15)
    @ApiModelProperty(value = "描述")
    private java.lang.String description;
	/**最后修改IP*/
	@Excel(name = "最后修改IP", width = 15)
    @ApiModelProperty(value = "最后修改IP")
    private java.lang.String updateIp;
	/**密级*/
	@Excel(name = "密级", width = 15)
    @Dict(dicCode = "security")
    @ApiModelProperty(value = "密级")
    private java.lang.Integer security;
	/**版本*/
	@Excel(name = "版本", width = 15)
    @ApiModelProperty(value = "版本")
    private java.lang.String version;
}
