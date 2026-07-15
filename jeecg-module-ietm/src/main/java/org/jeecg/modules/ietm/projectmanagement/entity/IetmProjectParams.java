package org.jeecg.modules.ietm.projectmanagement.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import java.util.Date;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.UnsupportedEncodingException;

/**
 * @Description: 项目管理-项目参数
 * @Author: jeecg-boot
 * @Date:   2026-01-09
 * @Version: V1.0
 */
@ApiModel(value="ietm_project_params对象", description="项目管理-项目参数")
@Data
@TableName("ietm_project_params")
public class IetmProjectParams implements Serializable {
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
	/**密级*/
	@Excel(name = "密级", width = 15)
    @ApiModelProperty(value = "密级")
    private Integer security;
	/**项目id*/
    @ApiModelProperty(value = "项目id")
    private String pid;
    /**商业和政府机构编码*/
    @Excel(name = "商业和政府机构编码", width = 15)
    @ApiModelProperty(value = "商业和政府机构编码")
    private String cageCode;
    /**位置码*/
    @Excel(name = "位置码", width = 15)
    @ApiModelProperty(value = "位置码")
    private String positionCode;
    /**默认业务规则*/
    @Excel(name = "默认业务规则", width = 15)
    @ApiModelProperty(value = "默认业务规则")
    private String defaultBusinessRule;
    /**国家*/
    @Excel(name = "国家", width = 15)
    @ApiModelProperty(value = "国家")
    private String countryCode;
    /**语言*/
    @Excel(name = "语言", width = 15)
    @ApiModelProperty(value = "语言")
    private String lanuageCode;



}
