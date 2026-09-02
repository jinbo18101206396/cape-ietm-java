package org.jeecg.modules.ietm.ietmddn.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 生成DDN结果VO
 * @Author: jeecg-boot
 * @Date: 2026-09-01
 */
@Data
@ApiModel(value = "生成DDN结果VO")
public class DdnGenerateResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "DDN编码")
    private String ddnCode;

    @ApiModelProperty(value = "下载文件名（ZIP）")
    private String fileName;

    @ApiModelProperty(value = "下载相对路径")
    private String downloadUrl;

    @ApiModelProperty(value = "实际导出DM数量（含引用）")
    private Integer dmCount;

    @ApiModelProperty(value = "实际导出ICN数量")
    private Integer icnCount;

    @ApiModelProperty(value = "错误DM列表（无内容或不存在的DM ID）")
    private List<String> errorDmList;
}
