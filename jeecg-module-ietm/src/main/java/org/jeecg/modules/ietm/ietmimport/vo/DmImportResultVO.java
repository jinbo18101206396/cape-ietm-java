package org.jeecg.modules.ietm.ietmimport.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * DM导入结果VO
 *
 * @author IETM Team
 * @date 2026-09-03
 */
@Data
@ApiModel(value = "DM导入结果", description = "数据模块导入结果")
public class DmImportResultVO {

    @ApiModelProperty(value = "成功导入的DM数量")
    private Integer dmSuccessCount;

    @ApiModelProperty(value = "成功导入的ICN数量")
    private Integer icnSuccessCount;

    @ApiModelProperty(value = "成功导入的资源文件数量")
    private Integer resourceSuccessCount;

    @ApiModelProperty(value = "失败数量")
    private Integer failureCount;

    @ApiModelProperty(value = "详细信息")
    private String message;

    @ApiModelProperty(value = "错误消息列表")
    private List<String> errors;
}
