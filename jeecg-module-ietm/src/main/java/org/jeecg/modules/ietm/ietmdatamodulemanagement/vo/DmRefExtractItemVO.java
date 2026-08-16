package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import lombok.Data;

/**
 * XML解析时提取出的单条引用关系（用于 calculateDmReferences）
 * 注意：勿与 DmRefBuildItemVO 混淆，后者是引用DM弹窗生成 dmRef XML片段用的。
 */
@Data
public class DmRefExtractItemVO {

    /**
     * 引用类型：dmRef / dmlRef / pmRef / graphic / multimedia / internalRef
     */
    private String refType;

    /**
     * 被引用对象的编码（DMC 全串 / ICN编码 / 内部锚点 ID）
     */
    private String targetDmc;

    /**
     * 引用在 XML 中的位置（dom4j element.getUniquePath()）
     */
    private String refPosition;
}
