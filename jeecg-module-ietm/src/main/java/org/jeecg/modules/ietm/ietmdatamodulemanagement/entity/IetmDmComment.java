package org.jeecg.modules.ietm.ietmdatamodulemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

/**
 * @Description: IETM数据模块资源管理
 * @Author: jeecg-boot
 * @Date: 2026-07-22
 * @Version: V2.0
 *
 * <p><b>注意：类名与业务不符</b></p>
 * <ul>
 *   <li>类名为 IetmDmComment，但实际映射表为 ietm_dm_resource</li>
 *   <li>业务含义为"数据模块资源管理"，并非评论（Comment）</li>
 *   <li>建议后续重构时重命名为 IetmDmResource</li>
 * </ul>
 */
@Data
@TableName("ietm_dm_resource")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ietm_dm_resource对象", description = "IETM数据模块资源管理")
public class IetmDmComment implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键ID")
    private String id;

    /** 关联DM的ID */
    @Excel(name = "DM ID", width = 32)
    @ApiModelProperty(value = "关联DM的ID", required = true)
    @NotBlank(message = "DM ID不能为空")
    @TableField("dm_id")
    private String dmId;

    /** 资源名称 */
    @Excel(name = "资源名称", width = 30)
    @ApiModelProperty(value = "资源名称（业务名称）", required = true)
    @NotBlank(message = "资源名称不能为空")
    @TableField("resource_name")
    private String resourceName;

    /** 文件名称（原始文件名）*/
    @Excel(name = "文件名称", width = 30)
    @ApiModelProperty(value = "文件名称（原始文件名）")
    @TableField("file_name")
    private String fileName;

    /** 文件存储路径或URL */
    @ApiModelProperty(value = "文件存储路径或URL")
    @TableField("file_path")
    private String filePath;

    /** 文件大小（字节）*/
    @Excel(name = "文件大小", width = 15)
    @ApiModelProperty(value = "文件大小（字节）")
    @TableField("file_size")
    private Long fileSize;

    /** 文件类型 */
    @Excel(name = "文件类型", width = 15)
    @ApiModelProperty(value = "文件类型（image/video/model/audio/other）")
    @TableField("file_type")
    private String fileType;

    /** 备注说明 */
    @Excel(name = "说明", width = 50)
    @ApiModelProperty(value = "备注说明")
    @TableField("remark")
    private String remark;

    /** 操作人 */
    @Excel(name = "操作人", width = 15)
    @ApiModelProperty(value = "操作人用户名")
    @TableField("operator")
    private String operator;

    /** 操作时间 */
    @Excel(name = "操作时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "操作时间")
    @TableField("operate_time")
    private Date operateTime;

    /** 创建人 */
    @ApiModelProperty(value = "创建人")
    @TableField("create_by")
    private String createBy;

    /** 创建时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private Date createTime;

    /** 更新人 */
    @ApiModelProperty(value = "更新人")
    @TableField("update_by")
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private Date updateTime;

    /** 所属部门编码 */
    @ApiModelProperty(value = "所属部门编码")
    @TableField("sys_org_code")
    private String sysOrgCode;
}
