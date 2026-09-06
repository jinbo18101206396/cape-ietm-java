package org.jeecg.modules.ietm.ietmimport.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * DM导入请求VO（包含DDN信息）
 *
 * @author IETM Team
 * @date 2026-09-03
 */
@Data
@ApiModel(value = "DM导入请求", description = "包含文件列表和DDN信息的导入请求")
public class DmImportRequestVO {

    @ApiModelProperty(value = "文件列表", required = true)
    private List<ImportFileItemVO> files;

    @ApiModelProperty(value = "DDN信息", required = true)
    private DdnInfoVO ddnInfo;
}
