package org.jeecg.modules.ietm.agencySelection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 机构遴选-附件表
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@TableName("agency_file")
@ApiModel(value = "agency_file对象", description = "机构遴选-附件表")
public class AgencyFile implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;

    @TableField("biz_id")
    @ApiModelProperty(value = "业务ID（机构ID或项目ID）")
    private String bizId;

    @TableField("biz_type")
    @ApiModelProperty(value = "业务类型：agency / project")
    private String bizType;

    @TableField("file_name")
    @ApiModelProperty(value = "文件名")
    private String fileName;

    @TableField("file_path")
    @ApiModelProperty(value = "文件路径")
    private String filePath;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    @TableLogic(value = "0", delval = "1")
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标志")
    private Integer delFlag;

    // ==================== Getter/Setter ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBizId() { return bizId; }
    public void setBizId(String bizId) { this.bizId = bizId; }

    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public Integer getDelFlag() { return delFlag; }
    public void setDelFlag(Integer delFlag) { this.delFlag = delFlag; }
}
