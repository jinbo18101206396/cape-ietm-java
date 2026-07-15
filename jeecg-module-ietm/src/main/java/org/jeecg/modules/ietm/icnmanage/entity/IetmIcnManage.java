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
 * @Description: 项目管理-项目实体管理
 * @Author: jeecg-boot
 * @Date: 2026-02-06
 * @Version: V1.0
 */
@Data
@TableName("ietm_icn_manage")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ietm_icn_manage对象", description = "项目管理-项目实体管理")
public class IetmIcnManage implements Serializable {
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
     * ICN
     */
    @Excel(name = "ICN", width = 15)
    @ApiModelProperty(value = "ICN")
    private String icn;
    /**
     * 文件名称
     */
    @Excel(name = "文件名称", width = 15)
    @ApiModelProperty(value = "文件名称")
    private String fileName;
    /**
     * 版本号
     */
    @Excel(name = "版本号", width = 15)
    @ApiModelProperty(value = "版本号")
    private String versionNo;
    /**
     * 密级
     */
    @Excel(name = "密级", width = 15, dicCode = "security")
    @Dict(dicCode = "security")
    @ApiModelProperty(value = "密级")
    private Integer security;
    /**
     * 文件大小
     */
    @Excel(name = "文件大小", width = 15)
    @ApiModelProperty(value = "文件大小")
    private String fileSize;
}
