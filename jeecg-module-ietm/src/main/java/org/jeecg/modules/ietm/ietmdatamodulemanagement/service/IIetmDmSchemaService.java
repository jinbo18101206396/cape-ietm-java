package org.jeecg.modules.ietm.ietmdatamodulemanagement.service;

import java.util.Map;

/** XSD → 前端约束对象（需求 §12/§25.6）。契约：结构对标 legacy schema2Designer 输出 */
public interface IIetmDmSchemaService {

    /** 解析英文约束对象。standard 如 S1000D4.0；xsd 如 descript.xsd */
    Map<String, Object> schema2Designer(String standard, String xsd);

    /** 中文化约束对象（元素名 key 与 children 翻译；attrs/setattr 的 key 保持英文，需求 §12.3） */
    Map<String, Object> schema2DesignerCn(Map<String, Object> enSchema, Map<String, String> en2cnElem);

    /** 元素英中映射（来自 ietm_translate） */
    Map<String, String> loadEn2CnElem(String standard);

    /** 元素中英映射 */
    Map<String, String> loadCn2EnElem(String standard);
}
