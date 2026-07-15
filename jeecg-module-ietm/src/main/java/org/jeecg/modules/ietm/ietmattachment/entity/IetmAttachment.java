package org.jeecg.modules.ietm.ietmattachment.entity;

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
 * @Description: 附件表
 * @Author: jeecg-boot
 * @Date:   2026-03-03
 * @Version: V1.0
 */
@Data
@TableName("ietm_attachment")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ietm_attachment对象", description="附件表")
public class IetmAttachment implements Serializable {
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
	/**文件名称*/
	@Excel(name = "文件名称", width = 15)
    @ApiModelProperty(value = "文件名称")
    private String fileName;
	/**文件标识*/
	@Excel(name = "文件标识", width = 15)
    @ApiModelProperty(value = "文件标识")
    private String fileKey;
	/**附件父id*/
	@Excel(name = "附件父id", width = 15)
    @ApiModelProperty(value = "附件父id")
    private String pid;
    /**附件类型*/
    @Excel(name = "附件类型", width = 15)
    @ApiModelProperty(value = "附件类型")
    private String fileType;
	/**文件大小*/
	@Excel(name = "文件大小", width = 15)
    @ApiModelProperty(value = "文件大小")
    private BigDecimal fileSize;
	/**文件属性*/
	@Excel(name = "文件属性", width = 15)
    @ApiModelProperty(value = "文件属性")
    private String fileProp;
	/**备注*/
	@Excel(name = "备注", width = 15)
    @ApiModelProperty(value = "备注")
    private String remark;
	/**密级*/
	@Excel(name = "密级", width = 15)
    @ApiModelProperty(value = "密级")
    private Integer security;
	/**文件密级*/
	@Excel(name = "文件密级", width = 15)
    @ApiModelProperty(value = "文件密级")
    private Integer fileSecurity;
}
