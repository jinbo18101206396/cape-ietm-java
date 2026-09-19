package org.jeecg.modules.ietm.icnmanage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 多媒体信息VO
 * 用于返回ICN的多媒体元数据（MIME类型、文件大小等）
 *
 * @author Claude Code
 * @since 2026-09-18
 */
@Data
@ApiModel(value = "MultimediaInfoVO", description = "多媒体元数据信息")
public class MultimediaInfoVO {

    @ApiModelProperty(value = "ICN编码")
    private String icnCode;

    @ApiModelProperty(value = "MIME类型", example = "video/mp4")
    private String mimeType;

    @ApiModelProperty(value = "文件大小（字节）", example = "10485760")
    private Long fileSize;

    @ApiModelProperty(value = "是否为视频")
    private Boolean isVideo;

    @ApiModelProperty(value = "是否为音频")
    private Boolean isAudio;

    @ApiModelProperty(value = "是否为图片")
    private Boolean isImage;

    @ApiModelProperty(value = "文件名", example = "video.mp4")
    private String fileName;

    /**
     * 根据MIME类型设置文件类型标识
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
        if (mimeType != null) {
            this.isVideo = mimeType.startsWith("video/");
            this.isAudio = mimeType.startsWith("audio/");
            this.isImage = mimeType.startsWith("image/");
        } else {
            this.isVideo = false;
            this.isAudio = false;
            this.isImage = false;
        }
    }
}
