package org.jeecg.modules.ietm.ietmimport.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jeecg.modules.ietm.ietmimport.constants.DmImportConstants;

/**
 * 导入文件项VO
 *
 * @author IETM Team
 * @date 2026-09-03
 */
@Data
@ApiModel(value = "导入文件项", description = "导入文件的校验结果项")
public class ImportFileItemVO {

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件类型(DM/ICN/RESOURCE)")
    private String fileType;

    @ApiModelProperty(value = "校验结果码(-10/-1/-2.../-99/1)")
    private String resultCode;

    @ApiModelProperty(value = "结果描述")
    private String resultMessage;

    @ApiModelProperty(value = "DMC编码（仅DM）")
    private String dmcCode;

    @ApiModelProperty(value = "临时文件路径（用于导入阶段）")
    private String tempFilePath;

    @ApiModelProperty(value = "XML内容（仅DM）")
    private String xmlContent;

    @ApiModelProperty(value = "关联的DMC编码（仅RESOURCE）")
    private String associatedDmcCode;

    @ApiModelProperty(value = "文件大小（字节）")
    private Long fileSize;

    /**
     * 是否可以导入
     */
    public boolean canImport() {
        return DmImportConstants.SUCCESS.equals(resultCode);
    }
}
