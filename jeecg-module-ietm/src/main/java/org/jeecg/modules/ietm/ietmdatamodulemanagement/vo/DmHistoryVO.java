package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 数据模块历史版本VO
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 */
@Data
@ApiModel(value = "DmHistoryVO", description = "数据模块历史版本对象")
public class DmHistoryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "历史记录ID")
    private String id;

    @ApiModelProperty(value = "DM主记录ID")
    private String dmId;

    @ApiModelProperty(value = "完整DMC编码")
    private String dmcCode;

    @ApiModelProperty(value = "版本号（如001-00）")
    private String versionInfo;

    @ApiModelProperty(value = "发行编号")
    private String issueNo;

    @ApiModelProperty(value = "在编编号")
    private String inWork;

    @ApiModelProperty(value = "版本类型")
    private String versionType;

    @ApiModelProperty(value = "操作类型（签出、签入、发布、升级）")
    private String operationType;

    @ApiModelProperty(value = "操作用户")
    private String operationUser;

    @ApiModelProperty(value = "操作时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operationTime;

    @ApiModelProperty(value = "变更说明")
    private String changeComment;

    @ApiModelProperty(value = "技术名称")
    private String techName;

    @ApiModelProperty(value = "信息名称")
    private String infoName;

    @ApiModelProperty(value = "DM内容快照（XML格式）")
    private String dmContentSnapshot;

    @ApiModelProperty(value = "是否当前版本")
    private Boolean isCurrent;
}
