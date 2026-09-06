package org.jeecg.modules.ietm.ietmimport.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * DM校验结果VO
 *
 * @author IETM Team
 * @date 2026-09-03
 */
@Data
@ApiModel(value = "DM校验结果", description = "数据模块校验结果")
public class DmValidateResultVO {

    @ApiModelProperty(value = "文件列表")
    private List<ImportFileItemVO> files;

    @ApiModelProperty(value = "总文件数")
    private Integer totalCount;

    @ApiModelProperty(value = "可导入文件数")
    private Integer successCount;

    @ApiModelProperty(value = "失败文件数")
    private Integer failureCount;
}
