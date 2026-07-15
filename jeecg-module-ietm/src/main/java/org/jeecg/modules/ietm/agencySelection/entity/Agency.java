package org.jeecg.modules.ietm.agencySelection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @Description: 机构遴选-机构表
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@TableName("agency")
@ApiModel(value = "agency对象", description = "机构遴选-机构表")
public class Agency implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private String id;

    @ApiModelProperty(value = "所属部门")
    private String sysOrgCode;

    @Excel(name = "机构名称", width = 20)
    @ApiModelProperty(value = "机构名称")
    private String agencyName;

    @Excel(name = "2023年绩效分", width = 15)
    @TableField("score_2023")
    @ApiModelProperty(value = "2023年绩效分")
    private Integer score2023;

    @Excel(name = "2024年绩效分", width = 15)
    @TableField("score_2024")
    @ApiModelProperty(value = "2024年绩效分")
    private Integer score2024;

    @Excel(name = "2025年绩效分", width = 15)
    @TableField("score_2025")
    @ApiModelProperty(value = "2025年绩效分")
    private Integer score2025;

    @Excel(name = "三年平均分", width = 12)
    @ApiModelProperty(value = "三年平均分")
    @TableField("avg_score")
    private BigDecimal avgScore;

    @Excel(name = "绩效等级", width = 10)
    @ApiModelProperty(value = "绩效等级")
    private String grade;

    @Excel(name = "权重系数", width = 10)
    @TableField("weight_coefficient")
    @ApiModelProperty(value = "权重系数")
    private BigDecimal weightCoefficient;

    @Excel(name = "本次摇号预设比例(%)", width = 18)
    @ApiModelProperty(value = "本次摇号预设比例(%)")
    private Integer ratio;

    @Excel(name = "已承担项目比例(%)", width = 16)
    @TableField("ut_ratio")
    @ApiModelProperty(value = "本年度已承担项目比例(%)")
    private BigDecimal utRatio;

    @Excel(name = "已承担项目数", width = 14)
    @TableField("project_count")
    @ApiModelProperty(value = "本年度已承担项目数")
    private Integer projectCount;

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

    public String getSysOrgCode() { return sysOrgCode; }
    public void setSysOrgCode(String sysOrgCode) { this.sysOrgCode = sysOrgCode; }

    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }

    public Integer getScore2023() { return score2023; }
    public void setScore2023(Integer score2023) { this.score2023 = score2023; }

    public Integer getScore2024() { return score2024; }
    public void setScore2024(Integer score2024) { this.score2024 = score2024; }

    public Integer getScore2025() { return score2025; }
    public void setScore2025(Integer score2025) { this.score2025 = score2025; }

    public BigDecimal getAvgScore() { return avgScore; }
    public void setAvgScore(BigDecimal avgScore) { this.avgScore = avgScore; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public BigDecimal getWeightCoefficient() { return weightCoefficient; }
    public void setWeightCoefficient(BigDecimal weightCoefficient) { this.weightCoefficient = weightCoefficient; }

    public Integer getRatio() { return ratio; }
    public void setRatio(Integer ratio) { this.ratio = ratio; }

    public BigDecimal getUtRatio() { return utRatio; }
    public void setUtRatio(BigDecimal utRatio) { this.utRatio = utRatio; }

    public Integer getProjectCount() { return projectCount; }
    public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }

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
