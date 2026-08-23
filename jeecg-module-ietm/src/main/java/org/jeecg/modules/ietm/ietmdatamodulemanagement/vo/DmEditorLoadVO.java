package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * DM 编辑器加载返回体（对标 legacy dmeditor/{id} 返回，需求 §9）
 */
@Data
@ApiModel(value = "DmEditorLoadVO", description = "DM编辑器加载数据")
public class DmEditorLoadVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("处理结果标志: success/failure")
    private String flag;

    @ApiModelProperty("DM的XML正文")
    private String xml;

    @ApiModelProperty("XSD Schema文件名, 如 descript.xsd")
    private String xsdSchema;

    @ApiModelProperty("所属标准, 如 S1000D4.0")
    private String ietmStandard;

    @ApiModelProperty("XSD解析出的英文约束对象 {元素:{children,setelem,attrs,setattr}}")
    private Map<String, Object> schema;

    @ApiModelProperty("中文化约束对象")
    private Map<String, Object> cnSchema;

    @ApiModelProperty("英文名→中文名 元素映射")
    private Map<String, String> en2cnElem;

    @ApiModelProperty("中文名→英文名 元素映射")
    private Map<String, String> cn2enElem;

    @ApiModelProperty("编辑器行为配置(designerSett)")
    private Map<String, Object> designerSett;

    @ApiModelProperty("乐观锁版本号(保存时原样回传，避免前端二次查询竞态)")
    private Integer version;

    @ApiModelProperty("签出用户(用于流程提交前校验，对齐旧系统)")
    private String checkoutUser;

    @ApiModelProperty("提示信息(加载失败时)")
    private String message;
}
