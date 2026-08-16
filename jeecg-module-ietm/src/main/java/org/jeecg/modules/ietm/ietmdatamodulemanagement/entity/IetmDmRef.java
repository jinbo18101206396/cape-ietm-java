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
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

/**
 * @Description: IETM数据模块引用关系
 * @Author: jeecg-boot
 * @Date: 2026-07-22
 * @Version: V2.0
 */
@Data
@TableName("ietm_dm_reference")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ietm_dm_reference对象", description = "IETM数据模块引用关系")
public class IetmDmRef implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键ID")
    private String id;

    /** 引用方DM的ID（出引用一侧）*/
    @ApiModelProperty(value = "引用方DM的ID（该DM主动引用其他DM）", required = true)
    @NotBlank(message = "引用方DM ID不能为空")
    @TableField("source_dm_id")
    private String sourceDmId;

    /** 被引用方DM的ID（入引用一侧）*/
    @ApiModelProperty(value = "被引用方DM的ID（该DM被其他DM引用）", required = true)
    @NotBlank(message = "被引用方DM ID不能为空")
    @TableField("target_dm_id")
    private String targetDmId;

    /** 引用类型（dmRef/dmlRef/pmRef）*/
    @ApiModelProperty(value = "引用类型（dmRef=DM引用/dmlRef=列表引用/pmRef=PM引用）")
    @TableField("ref_type")
    private String refType;

    /** 引用方DMC编码 */
    @ApiModelProperty(value = "引用方的DMC完整编码")
    @TableField("ref_dmc")
    private String refDmc;

    /** 被引用方DMC编码 */
    @ApiModelProperty(value = "被引用方的DMC完整编码")
    @TableField("target_dmc")
    private String targetDmc;

    /** 引用在XML中的位置（XPath）*/
    @ApiModelProperty(value = "引用位置（XML中的XPath路径）")
    @TableField("ref_position")
    private String refPosition;

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
