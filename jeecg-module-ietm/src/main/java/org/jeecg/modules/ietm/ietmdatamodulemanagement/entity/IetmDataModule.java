package org.jeecg.modules.ietm.ietmdatamodulemanagement.entity;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
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
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 项目管理-项目数据模块管理
 * @Author: jeecg-boot
 * @Date:   2026-03-10
 * @Version: V1.0
 */
@Data
@TableName("ietm_data_module")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ietm_data_module对象", description="项目管理-项目数据模块管理")
public class IetmDataModule implements Serializable {
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
	/**数据模块编码*/
	@Excel(name = "数据模块编码", width = 15)
    @ApiModelProperty(value = "数据模块编码")
    private String dmc;
	/**技术名称*/
	@Excel(name = "技术名称", width = 15)
    @ApiModelProperty(value = "技术名称")
    private String techName;
	/**信息名称*/
	@Excel(name = "信息名称", width = 15)
    @ApiModelProperty(value = "信息名称")
    private String infoName;
	/**DM类型*/
	@Excel(name = "DM类型", width = 15)
    @ApiModelProperty(value = "DM类型")
    private String dmType;
	/**版本*/
	@Excel(name = "版本", width = 15)
    @ApiModelProperty(value = "版本")
    private String version;
	/**版本类型*/
	@Excel(name = "版本类型", width = 15)
    @ApiModelProperty(value = "版本类型")
    private String versionType;
	/**版本日期*/
	@Excel(name = "版本日期", width = 15, format = "yyyy-MM-dd")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @ApiModelProperty(value = "版本日期")
    private Date versionDate;
	/**密级*/
	@Excel(name = "密级", width = 15)
    @ApiModelProperty(value = "密级")
    private Integer security;
	/**流程当前步骤*/
	@Excel(name = "流程当前步骤", width = 15)
    @ApiModelProperty(value = "流程当前步骤")
    private String processStep;
	/**流程状态*/
	@Excel(name = "流程状态", width = 15)
    @ApiModelProperty(value = "流程状态")
    private String processStatus;
	/**迁入/迁出状态*/
	@Excel(name = "迁入/迁出状态", width = 15)
    @ApiModelProperty(value = "迁入/迁出状态")
    private Integer checkStatus;
}
