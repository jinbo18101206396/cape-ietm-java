package org.jeecg.modules.ietm.ietmdatamodulemanagement.service;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmEditorLoadVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmValidateItemVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefBuildItemVO;
import java.util.List;
import java.util.Map;

/** DM 编辑器内容服务（加载/保存/校验/预览，需求 §9/§15/§17/§18） */
public interface IIetmDmContentService {

    /**
     * 加载编辑器数据：xml + schema + cnSchema + 中英映射 + designerSett（§9）
     * @param id DM主表ID
     * @param historyId 历史版本表ID（可选），如果提供则从历史版本表加载内容
     */
    DmEditorLoadVO loadEditorData(String id, String historyId);

    /**
     * 仅保存 XML 正文（§15）。契约：只更新 dm_content + 乐观锁；前置校验"本人已签出"。
     * @return 空=成功；非空=错误信息（如版本冲突/未签出/非本人）
     */
    String saveContent(String id, String content, Integer clientVersion, String username);

    /** XSD 校验（§17.5 CONFIRMED）。返回错误列表；空列表=通过 */
    List<DmValidateItemVO> validateXsd(String content, String standard, String schema, String dmId);

    /**
     * 按DM ID从数据库读取内容后校验（列表页调用，§17.5 列表页入口）。
     * 返回三态 map：flag="0"|"1"|"error"，flag=error时含errors列表。
     */
    Map<String, Object> validateById(String id);

    /** XML → HTML 预览（§18） */
    String renderHtml(String content, String dmId);

    /** 提取DM内部可引用片段列表（§14.5.2③）。返回 {flag:"success", refs:[...]} */
    Map<String, Object> getRef(String id);

    /**
     * 批量生成 {@code <dmRef>} XML（§14.5.4）。
     * dmCode 从目标DM自身 XML 提取，保证属性值权威正确。
     * 返回 {flag:"success", xml:"拼接后的dmRef字符串"} 或 {flag:"failure", message:...}
     */
    Map<String, Object> buildDmRef(List<DmRefBuildItemVO> items);
}
