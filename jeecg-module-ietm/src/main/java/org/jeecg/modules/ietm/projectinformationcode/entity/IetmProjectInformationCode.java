package org.jeecg.modules.ietm.projectinformationcode.entity;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * @Description: 项目信息码管理
 * @Author: jeecg-boot
 * @Date: 2026-07-16
 * @Version: V1.0
 */
@Data
@TableName("ietm_project_information_code")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="项目信息码对象", description="项目信息码管理")
public class IetmProjectInformationCode implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键*/
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;

    /**创建人*/
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /**创建日期*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private Date createTime;

    /**更新人*/
    @ApiModelProperty(value = "更新人")
    private String updateBy;

    /**更新日期*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private Date updateTime;

    /**项目id*/
    @ApiModelProperty(value = "项目ID")
    @NotBlank(message = "项目ID不能为空")
    private String projectId;

    /**编码*/
    @Excel(name = "编码", width = 15)
    @ApiModelProperty(value = "编码")
    @NotBlank(message = "编码不能为空")
    @Pattern(regexp = "^[0-9A-Z]+$", message = "编码只能包含数字和大写字母")
    private String code;

    /**标准数据模块id*/
    @ApiModelProperty(value = "数据模块类型ID")
    @NotBlank(message = "数据模块类型不能为空")
    private String dmtypeId;

    /**数据模块类型*/
    @Excel(name = "数据模块类型", width = 15, dictTable = "ietm_standard_dmtype", dicText = "dmtype_name", dicCode = "dmtype_name")
    @Dict(dictTable = "ietm_standard_dmtype", dicText = "dmtype_name", dicCode = "dmtype_name")
    @ApiModelProperty(value = "数据模块类型名称")
    private String dmtypeName;

    /**描述*/
    @Excel(name = "描述", width = 20)
    @ApiModelProperty(value = "描述")
    @NotBlank(message = "描述不能为空")
    private String description;

    /**备注*/
    @Excel(name = "备注", width = 30)
    @ApiModelProperty(value = "备注")
    private String remark;

    /**最后修改ip*/
    @ApiModelProperty(value = "最后修改IP")
    private String updateIp;

    /**版本*/
    @Excel(name = "版本", width = 10)
    @ApiModelProperty(value = "版本")
    private String version;

    /**密级*/
    @Excel(name = "密级", width = 10, dicCode = "security")
    @Dict(dicCode = "security")
    @ApiModelProperty(value = "密级")
    @NotNull(message = "密级不能为空")
    private Integer security;
}
