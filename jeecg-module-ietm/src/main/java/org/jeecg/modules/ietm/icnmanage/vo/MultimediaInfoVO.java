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

    @ApiModelProperty(value = "是否为3D模型")
    private Boolean is3DModel;

    @ApiModelProperty(value = "文件名", example = "video.mp4")
    private String fileName;

    @ApiModelProperty(value = "ICN类型", example = "formula", notes = "formula=公式ICN, 其他为普通ICN")
    private String icnType;

    @ApiModelProperty(value = "多媒体类型", example = "3D", notes = "3D, video, audio, image, other")
    private String multimediaType;

    /**
     * 根据MIME类型设置文件类型标识
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
        if (mimeType != null) {
            this.isVideo = mimeType.startsWith("video/");
            this.isAudio = mimeType.startsWith("audio/");
            this.isImage = mimeType.startsWith("image/");
            this.is3DModel = mimeType.startsWith("model/");
        } else {
            this.isVideo = false;
            this.isAudio = false;
            this.isImage = false;
            this.is3DModel = false;
        }
    }

    /**
     * 根据文件扩展名设置多媒体类型
     * @param fileName 文件名
     */
    public void setMultimediaTypeByFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            this.multimediaType = "other";
            return;
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        switch (extension) {
            // 3D模型
            case "glb":
            case "gltf":
                this.multimediaType = "3D";
                break;
            // 视频
            case "mp4":
            case "avi":
            case "mov":
            case "webm":
            case "mkv":
            case "flv":
            case "wmv":
                this.multimediaType = "video";
                break;
            // 音频
            case "mp3":
            case "wav":
            case "aac":
            case "flac":
            case "ogg":
            case "m4a":
            case "wma":
                this.multimediaType = "audio";
                break;
            // 图片
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "tif":
            case "tiff":
            case "svg":
            case "cgm":
                this.multimediaType = "image";
                break;
            // 其他
            default:
                this.multimediaType = "other";
        }
    }
}
