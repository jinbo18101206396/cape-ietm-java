package org.jeecg.modules.ietm.ietmstandardconfigurationmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 预制模板-构型管理
 * @Author: jeecg-boot
 * @Date:   2026-01-07
 * @Version: V1.0
 */
@Data
@TableName("ietm_standard_configuration_management")
@ApiModel(value="ietm_standard_configuration_management对象", description="预制模板-构型管理")
public class IetmStandardConfigurationManagement implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;
	/**创建人*/
    @ApiModelProperty(value = "创建人")
    private String createBy;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private Date createTime;
	/**更新人*/
    @ApiModelProperty(value = "更新人")
    private String updateBy;
	/**更新日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private Date updateTime;
	/**所属部门*/
    @ApiModelProperty(value = "所属部门")
    private String sysOrgCode;
	/**父节点id*/
	@Excel(name = "父节点id", width = 15)
    @ApiModelProperty(value = "父节点id")
    private String pid;
	/**关联的标准*/
	@Excel(name = "关联的标准", width = 15, dicCode = "standard_type")
    @ApiModelProperty(value = "关联的标准")
    @Dict(dicCode = "standard_type")
    private String standard;
	/**装备类型*/
	@Excel(name = "装备类型", width = 15, dicCode = "equipment_type")
    @Dict(dicCode = "equipment_type")
    @ApiModelProperty(value = "装备类型")
    private String equipmentType;
	/**编码*/
	@Excel(name = "编码", width = 15)
    @ApiModelProperty(value = "编码")
    private String code;
	/**标题*/
	@Excel(name = "标题", width = 15)
    @ApiModelProperty(value = "标题")
    private String title;
	/**序号*/
	@Excel(name = "序号", width = 15)
    @ApiModelProperty(value = "序号")
    private Integer seq;
	/**最后更新ip*/
	@Excel(name = "最后更新ip", width = 15)
    @ApiModelProperty(value = "最后更新ip")
    private String updateIp;
	/**状态*/
	@Excel(name = "状态", width = 15)
    @ApiModelProperty(value = "状态")
    private String status;
	/**版本*/
	@Excel(name = "版本", width = 15)
    @ApiModelProperty(value = "版本")
    private String version;
    /**密级*/
    @Excel(name = "密级", width = 15, dicCode = "security")
    @Dict(dicCode = "security")
    @ApiModelProperty(value = "密级")
    private Integer security;
	/**是否有子节点*/
	@Excel(name = "是否有子节点", width = 15, dicCode = "yn")
	@Dict(dicCode = "yn")
    @ApiModelProperty(value = "是否有子节点")
    private String hasChild;
    /**
     * 路径
     */
    @TableField(exist = false)
    private String way;
}
