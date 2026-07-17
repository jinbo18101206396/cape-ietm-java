package org.jeecg.modules.ietm.projectconfigurationmanagement.entity;

import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.UnsupportedEncodingException;

/**
 * @Description: 项目管理-项目构型管理
 * @Author: jeecg-boot
 * @Date:   2026-02-10
 * @Version: V1.0
 */
@Data
@TableName("ietm_project_configuration_management")
@ApiModel(value="ietm_project_configuration_management对象", description="项目管理-项目构型管理")
public class IetmProjectConfigurationManagement implements Serializable {
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
	/**项目id*/
	@Excel(name = "项目id", width = 15)
    @ApiModelProperty(value = "项目id")
    private java.lang.String projectId;
	/**父节点id*/
	@Excel(name = "父节点id", width = 15)
    @ApiModelProperty(value = "父节点id")
    private java.lang.String pid;
	/**编码*/
	@Excel(name = "编码", width = 15)
    @ApiModelProperty(value = "编码")
    private java.lang.String code;
	/**技术名称*/
	@Excel(name = "技术名称", width = 15)
    @ApiModelProperty(value = "技术名称")
    private java.lang.String title;
	/**序号*/
    @Excel(name = "序号", width = 15)
    @ApiModelProperty(value = "序号")
    private Integer seq;
	/**路径*/
	@Excel(name = "路径", width = 15)
    @ApiModelProperty(value = "路径")
    private java.lang.String path;
	/**最后修改ip*/
	@Excel(name = "最后修改ip", width = 15)
    @ApiModelProperty(value = "最后修改ip")
    private java.lang.String updateIp;
	/**版本*/
	@Excel(name = "版本", width = 15)
    @ApiModelProperty(value = "版本")
    private java.lang.String version;
	/**状态*/
	@Excel(name = "状态", width = 15)
    @ApiModelProperty(value = "状态")
    private java.lang.String status;
	/**密级*/
	@Excel(name = "密级", width = 15, dicCode = "security")
	@Dict(dicCode = "security")
    @ApiModelProperty(value = "密级")
    private java.lang.Integer security;
	/**是否有子节点*/
	@Excel(name = "是否有子节点", width = 15, dicCode = "yn")
	@Dict(dicCode = "yn")
    @ApiModelProperty(value = "是否有子节点")
    private java.lang.String hasChild;
	/**节点层级（0-7级，根节点为0级，不存储到数据库，仅用于前端显示）*/
	@com.baomidou.mybatisplus.annotation.TableField(exist = false)
    @ApiModelProperty(value = "节点层级")
    private Integer level;
}
