package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * DM XSD 校验请求体（需求 §17.5）
 */
@Data
@ApiModel(value = "DmValidateVO", description = "DM校验请求")
public class DmValidateVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "待校验XML正文(英文标签,已从根元素截起); 为空时需提供dmId从数据库读取")
    private String content;

    @ApiModelProperty("DM主键ID; content为空时从数据库读取dm_content")
    private String id;

    @ApiModelProperty("标准, 如 S1000D4.0; 为空则用DM所属项目标准")
    private String standard;

    @ApiModelProperty("XSD文件名, 如 descript.xsd; 为空则按dmType反查")
    private String schema;
}
