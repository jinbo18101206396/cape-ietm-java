package org.jeecg.modules.ietm.ietmroleauth.entity;

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
 * @Description: 手册授权管理
 * @Author: jeecg-boot
 * @Date: 2026-07-15
 * @Version: V1.0
 */
@Data
@TableName("ietm_roleauth")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ietm_roleauth对象", description="手册授权管理")
public class IetmRoleauth implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键*/
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;

    /**角色ID*/
    @Excel(name = "角色", width = 15, dictTable = "sys_role", dicText = "role_name", dicCode = "id")
    @Dict(dictTable = "sys_role", dicText = "role_name", dicCode = "id")
    @ApiModelProperty(value = "角色ID")
    private String roleId;

    /**授权对象类型*/
    @Excel(name = "授权类型", width = 15, dicCode = "auth_obj_type")
    @Dict(dicCode = "auth_obj_type")
    @ApiModelProperty(value = "授权对象类型：1=项目,2=构型")
    private String objType;

    /**项目ID*/
    @Excel(name = "项目", width = 15, dictTable = "ietm_project", dicText = "name", dicCode = "id")
    @Dict(dictTable = "ietm_project", dicText = "name", dicCode = "id")
    @ApiModelProperty(value = "项目ID")
    private String projectId;

    /**授权对象ID*/
    @ApiModelProperty(value = "授权对象ID")
    private String objId;

    /**浏览权限*/
    @Excel(name = "浏览权限", width = 15, dicCode = "yes_no")
    @Dict(dicCode = "yes_no")
    @ApiModelProperty(value = "浏览权限：Y/N")
    private String canRead;

    /**编辑权限*/
    @Excel(name = "编辑权限", width = 15, dicCode = "yes_no")
    @Dict(dicCode = "yes_no")
    @ApiModelProperty(value = "编辑权限：Y/N")
    private String canEdit;

    /**备注*/
    @Excel(name = "备注", width = 15)
    @ApiModelProperty(value = "备注")
    private String note;

    /**创建人*/
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /**创建日期*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建日期")
    private Date createTime;

    /**更新人*/
    @ApiModelProperty(value = "更新人")
    private String updateBy;

    /**更新日期*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新日期")
    private Date updateTime;

    /**所属部门*/
    @ApiModelProperty(value = "所属部门")
    private String sysOrgCode;
}
