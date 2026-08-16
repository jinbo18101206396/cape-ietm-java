package org.jeecg.modules.ietm.icnmanage.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 下载任务VO
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "DownloadTaskVO", description = "下载任务VO")
public class DownloadTaskVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "任务ID")
    private String taskId;

    @ApiModelProperty(value = "任务状态: PENDING/PROCESSING/COMPLETED/FAILED/NOT_FOUND")
    private String status;

    @ApiModelProperty(value = "进度百分比(0-100)")
    private Integer progress;

    @ApiModelProperty(value = "总文件数")
    private Integer totalFiles;

    @ApiModelProperty(value = "已处理文件数")
    private Integer processedFiles;

    @ApiModelProperty(value = "下载URL（任务完成后）")
    private String downloadUrl;

    @ApiModelProperty(value = "错误信息")
    private String errorMessage;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @ApiModelProperty(value = "开始时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @ApiModelProperty(value = "耗时（秒）")
    private Long duration;

    // ===== 兼容字段（保留以下字段用于向后兼容） =====

    @ApiModelProperty(value = "下载日志ID")
    private String logId;

    @ApiModelProperty(value = "下载类型: SINGLE/BATCH/ASYNC")
    private String downloadType;

    @ApiModelProperty(value = "ICN数量")
    private Integer icnCount;

    @ApiModelProperty(value = "文件数量")
    private Integer fileCount;

    @ApiModelProperty(value = "总大小（字节）")
    private Long totalSize;

    @ApiModelProperty(value = "已处理文件数（别名）")
    public Integer getProcessedFileCount() {
        return processedFiles;
    }

    public void setProcessedFileCount(Integer processedFileCount) {
        this.processedFiles = processedFileCount;
    }

    @ApiModelProperty(value = "下载文件路径（任务完成后）")
    private String downloadFilePath;

    @ApiModelProperty(value = "下载文件URL（任务完成后，别名）")
    public String getDownloadFileUrl() {
        return downloadUrl;
    }

    public void setDownloadFileUrl(String downloadFileUrl) {
        this.downloadUrl = downloadFileUrl;
    }

    @ApiModelProperty(value = "下载用户")
    private String downloadUser;

    @ApiModelProperty(value = "完成时间（别名）")
    public String getCompleteTime() {
        if (endTime != null) {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime);
        }
        return null;
    }

    @ApiModelProperty(value = "错误信息（别名）")
    public String getErrorMsg() {
        return errorMessage;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMessage = errorMsg;
    }

    @ApiModelProperty(value = "备注")
    private String remark;
}
