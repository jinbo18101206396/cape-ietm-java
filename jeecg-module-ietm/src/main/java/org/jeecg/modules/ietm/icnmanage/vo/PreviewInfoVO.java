package org.jeecg.modules.ietm.icnmanage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Description: ICN预览信息VO
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "PreviewInfoVO", description = "ICN预览信息VO")
public class PreviewInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ICN ID")
    private String icnId;

    @ApiModelProperty(value = "ICN编码")
    private String icn;

    @ApiModelProperty(value = "文件名称")
    private String fileName;

    @ApiModelProperty(value = "文件类型")
    private String fileType;

    @ApiModelProperty(value = "文件扩展名")
    private String fileExt;

    @ApiModelProperty(value = "文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty(value = "文件路径（相对路径）")
    private String filePath;

    @ApiModelProperty(value = "文件访问URL")
    private String fileUrl;

    @ApiModelProperty(value = "预览类型: IMAGE/VIDEO/AUDIO/CGM/FLASH/3D/SMG/OTHER")
    private String previewType;

    @ApiModelProperty(value = "是否支持预览")
    private Boolean canPreview;

    @ApiModelProperty(value = "版本号")
    private String issueNo;

    @ApiModelProperty(value = "密级")
    private Integer security;

    @ApiModelProperty(value = "创建时间")
    private String createTime;

    @ApiModelProperty(value = "备注")
    private String remark;
}
