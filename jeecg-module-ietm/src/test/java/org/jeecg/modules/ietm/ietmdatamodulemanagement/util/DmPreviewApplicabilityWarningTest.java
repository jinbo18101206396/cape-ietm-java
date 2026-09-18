package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DM预览功能 - applicability条件内容和warning/caution/note验证测试
 *
 * 对标S1000D标准：
 * - §3.9.5.2.3: applicability条件内容渲染
 * - §3.9.5.2.4: 条件性内容（warning/caution/note）渲染
 *
 * 验证目标：
 * 1. applicability的displayText是否正确渲染
 * 2. warning是否有正确的样式类（warningBGImage/wcBorderSpacing）
 * 3. caution是否有正确的样式类（cautionBGImage/wcBorderSpacing）
 * 4. note是否有正确的样式类（noteBlock/noteTitle）
 * 5. 内容文本是否正确显示
 *
 * 注意：此测试不依赖Spring上下文，直接调用DmXmlHelper静态方法
 *
 * @author Claude
 * @date 2026-09-18
 */
@Slf4j
@DisplayName("DM预览 - applicability和warning/caution/note验证")
public class DmPreviewApplicabilityWarningTest {

    /**
     * 测试1: applicability条件内容渲染（§3.9.5.2.3）
     *
     * S1000D标准要求：
     * - 显示displayText内容
     * - 添加applicAnnotation样式类
     * - 支持内联applicability引用
     */
    @Test
    @DisplayName("测试applicability条件内容渲染")
    public void testApplicabilityDisplay() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n" +
                "  <identAndStatusSection>\n" +
                "    <dmAddress>\n" +
                "      <dmIdent>\n" +
                "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\"\n" +
                "                subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\"\n" +
                "                disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"000\"\n" +
                "                infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
                "      </dmIdent>\n" +
                "    </dmAddress>\n" +
                "    <dmStatus>\n" +
                "      <applic>\n" +
                "        <displayText>\n" +
                "          <simplePara>适用于：A320系列飞机，发动机型号CFM56-5B</simplePara>\n" +
                "        </displayText>\n" +
                "      </applic>\n" +
                "    </dmStatus>\n" +
                "  </identAndStatusSection>\n" +
                "  <content>\n" +
                "    <description>\n" +
                "      <levelledPara>\n" +
                "        <title>发动机检查</title>\n" +
                "        <para>\n" +
                "          检查液压系统压力，确保在正常范围内。\n" +
                "        </para>\n" +
                "      </levelledPara>\n" +
                "    </description>\n" +
                "  </content>\n" +
                "</dmodule>";

        String html = DmXmlHelper.renderHtml(xml, "/ietm");

        // 验证HTML生成成功
        assertNotNull(html, "HTML应该成功生成");
        assertFalse(html.contains("预览渲染失败"), "不应该有渲染错误");

        // 验证displayText内容被渲染
        assertTrue(html.contains("适用于") || html.contains("A320"),
                   "applicability的displayText应该被渲染");

        // 验证applicAnnotation样式类（如果有）
        // 注意：applic.xsl中定义了applicAnnotation类
        boolean hasApplicStyle = html.contains("applicAnnotation") ||
                                 html.contains("class=\"inlineApplic\"");

        log.info("✅ applicability渲染验证:");
        log.info("   - displayText内容: {}", html.contains("适用于") ? "✓ 已渲染" : "✗ 未渲染");
        log.info("   - applicAnnotation样式: {}", hasApplicStyle ? "✓ 存在" : "⚠ 不存在（可能使用其他样式）");
        log.info("   - HTML长度: {} 字节", html.length());
    }

    /**
     * 测试2: warning警告内容渲染（§3.9.5.2.4）
     *
     * S1000D标准要求：
     * - 红色背景或红色边框
     * - 警告图标（warningBGImage）
     * - 粗体标题
     * - 内容文本
     */
    @Test
    @DisplayName("测试warning警告内容渲染")
    public void testWarningDisplay() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <identAndStatusSection>\n" +
                "    <dmAddress>\n" +
                "      <dmIdent>\n" +
                "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\"\n" +
                "                subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\"\n" +
                "                disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"000\"\n" +
                "                infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
                "      </dmIdent>\n" +
                "    </dmAddress>\n" +
                "  </identAndStatusSection>\n" +
                "  <content>\n" +
                "    <description>\n" +
                "      <levelledPara>\n" +
                "        <title>高压警告</title>\n" +
                "        <warning>\n" +
                "          <warningAndCautionPara>高压危险！操作前务必断开电源。未断电操作可能导致严重电击伤害或死亡。</warningAndCautionPara>\n" +
                "        </warning>\n" +
                "        <para>断开主电源开关...</para>\n" +
                "      </levelledPara>\n" +
                "    </description>\n" +
                "  </content>\n" +
                "</dmodule>";

        String html = DmXmlHelper.renderHtml(xml, "/ietm");

        assertNotNull(html, "HTML应该成功生成");
        assertFalse(html.contains("预览渲染失败"), "不应该有渲染错误");

        // 验证warning内容被渲染
        assertTrue(html.contains("高压危险") || html.contains("电击"),
                   "warning的内容应该被渲染");

        // 验证warning样式类（根据wcnpWarningCaution.xsl:9）
        boolean hasWarningStyle = html.contains("warningBGImage") ||
                                  html.contains("wcBorderSpacing");
        assertTrue(hasWarningStyle, "warning应该有warningBGImage或wcBorderSpacing样式类");

        // 验证wcContentArea类（根据wcnpWarningCaution.xsl:12）
        assertTrue(html.contains("wcContentArea"),
                   "warning应该有wcContentArea内容区域");

        // 验证wcTitle类（标题）
        boolean hasTitle = html.contains("wcTitle");

        log.info("✅ warning渲染验证:");
        log.info("   - warning内容: {}", html.contains("高压危险") ? "✓ 已渲染" : "✗ 未渲染");
        log.info("   - warningBGImage样式: {}", html.contains("warningBGImage") ? "✓ 存在" : "✗ 不存在");
        log.info("   - wcBorderSpacing样式: {}", html.contains("wcBorderSpacing") ? "✓ 存在" : "✗ 不存在");
        log.info("   - wcContentArea: {}", html.contains("wcContentArea") ? "✓ 存在" : "✗ 不存在");
        log.info("   - wcTitle标题: {}", hasTitle ? "✓ 存在" : "✗ 不存在");
    }

    /**
     * 测试3: caution注意内容渲染（§3.9.5.2.4）
     *
     * S1000D标准要求：
     * - 黄色背景或黄色边框
     * - 注意图标（cautionBGImage）
     * - 粗体标题
     * - 内容文本
     */
    @Test
    @DisplayName("测试caution注意内容渲染")
    public void testCautionDisplay() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <identAndStatusSection>\n" +
                "    <dmAddress>\n" +
                "      <dmIdent>\n" +
                "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\"\n" +
                "                subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\"\n" +
                "                disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"000\"\n" +
                "                infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
                "      </dmIdent>\n" +
                "    </dmAddress>\n" +
                "  </identAndStatusSection>\n" +
                "  <content>\n" +
                "    <description>\n" +
                "      <levelledPara>\n" +
                "        <title>设备保护注意事项</title>\n" +
                "        <caution>\n" +
                "          <warningAndCautionPara>注意：操作前请佩戴防护手套和护目镜。避免化学品接触皮肤。</warningAndCautionPara>\n" +
                "        </caution>\n" +
                "        <para>准备工具和防护装备...</para>\n" +
                "      </levelledPara>\n" +
                "    </description>\n" +
                "  </content>\n" +
                "</dmodule>";

        String html = DmXmlHelper.renderHtml(xml, "/ietm");

        assertNotNull(html, "HTML应该成功生成");
        assertFalse(html.contains("预览渲染失败"), "不应该有渲染错误");

        // 验证caution内容被渲染
        assertTrue(html.contains("防护手套") || html.contains("护目镜"),
                   "caution的内容应该被渲染");

        // 验证caution样式类（根据wcnpWarningCaution.xsl:20）
        boolean hasCautionStyle = html.contains("cautionBGImage") ||
                                  html.contains("wcBorderSpacing");
        assertTrue(hasCautionStyle, "caution应该有cautionBGImage或wcBorderSpacing样式类");

        // 验证wcContentArea类
        assertTrue(html.contains("wcContentArea"),
                   "caution应该有wcContentArea内容区域");

        log.info("✅ caution渲染验证:");
        log.info("   - caution内容: {}", html.contains("防护手套") ? "✓ 已渲染" : "✗ 未渲染");
        log.info("   - cautionBGImage样式: {}", html.contains("cautionBGImage") ? "✓ 存在" : "✗ 不存在");
        log.info("   - wcBorderSpacing样式: {}", html.contains("wcBorderSpacing") ? "✓ 存在" : "✗ 不存在");
        log.info("   - wcContentArea: {}", html.contains("wcContentArea") ? "✓ 存在" : "✗ 不存在");
    }

    /**
     * 测试4: note说明内容渲染（§3.9.5.2.4）
     *
     * S1000D标准要求：
     * - 蓝色背景或蓝色边框（可选）
     * - 信息图标（可选）
     * - 粗体标题
     * - 内容文本
     */
    @Test
    @DisplayName("测试note说明内容渲染")
    public void testNoteDisplay() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <identAndStatusSection>\n" +
                "    <dmAddress>\n" +
                "      <dmIdent>\n" +
                "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\"\n" +
                "                subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\"\n" +
                "                disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"000\"\n" +
                "                infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
                "      </dmIdent>\n" +
                "    </dmAddress>\n" +
                "  </identAndStatusSection>\n" +
                "  <content>\n" +
                "    <description>\n" +
                "      <levelledPara>\n" +
                "        <title>维护建议</title>\n" +
                "        <note>\n" +
                "          <notePara>建议使用原厂零件以确保最佳性能。参考维护手册第5章获取详细信息。</notePara>\n" +
                "        </note>\n" +
                "        <para>更换零件步骤...</para>\n" +
                "      </levelledPara>\n" +
                "    </description>\n" +
                "  </content>\n" +
                "</dmodule>";

        String html = DmXmlHelper.renderHtml(xml, "/ietm");

        assertNotNull(html, "HTML应该成功生成");
        assertFalse(html.contains("预览渲染失败"), "不应该有渲染错误");

        // 验证note内容被渲染
        assertTrue(html.contains("原厂零件") || html.contains("维护手册"),
                   "note的内容应该被渲染");

        // 验证note样式类（根据wcnpNote.xsl:11, 15）
        boolean hasNoteStyle = html.contains("noteBlock") ||
                               html.contains("noteTitle") ||
                               html.contains("noteContent");
        assertTrue(hasNoteStyle, "note应该有noteBlock/noteTitle/noteContent样式类");

        log.info("✅ note渲染验证:");
        log.info("   - note内容: {}", html.contains("原厂零件") ? "✓ 已渲染" : "✗ 未渲染");
        log.info("   - noteBlock样式: {}", html.contains("noteBlock") ? "✓ 存在" : "✗ 不存在");
        log.info("   - noteTitle样式: {}", html.contains("noteTitle") ? "✓ 存在" : "✗ 不存在");
        log.info("   - noteContent样式: {}", html.contains("noteContent") ? "✓ 存在" : "✗ 不存在");
    }

    /**
     * 测试5: 混合场景 - 同一段落包含warning/caution/note
     */
    @Test
    @DisplayName("测试混合场景 - warning/caution/note同时存在")
    public void testMixedWarningCautionNote() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <identAndStatusSection>\n" +
                "    <dmAddress>\n" +
                "      <dmIdent>\n" +
                "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\"\n" +
                "                subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\"\n" +
                "                disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"000\"\n" +
                "                infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
                "      </dmIdent>\n" +
                "    </dmAddress>\n" +
                "  </identAndStatusSection>\n" +
                "  <content>\n" +
                "    <description>\n" +
                "      <levelledPara>\n" +
                "        <title>电气系统维护</title>\n" +
                "        <warning>\n" +
                "          <warningAndCautionPara>致命电压！操作前断开所有电源。</warningAndCautionPara>\n" +
                "        </warning>\n" +
                "        <caution>\n" +
                "          <warningAndCautionPara>佩戴绝缘手套和安全鞋。</warningAndCautionPara>\n" +
                "        </caution>\n" +
                "        <note>\n" +
                "          <notePara>建议由持证电工执行此操作。</notePara>\n" +
                "        </note>\n" +
                "        <para>维护步骤如下...</para>\n" +
                "      </levelledPara>\n" +
                "    </description>\n" +
                "  </content>\n" +
                "</dmodule>";

        String html = DmXmlHelper.renderHtml(xml, "/ietm");

        assertNotNull(html, "HTML应该成功生成");

        // 验证所有三种内容都被渲染
        assertTrue(html.contains("致命电压"), "warning内容应该被渲染");
        assertTrue(html.contains("绝缘手套"), "caution内容应该被渲染");
        assertTrue(html.contains("持证电工"), "note内容应该被渲染");

        // 验证样式类都存在
        int warningCount = countOccurrences(html, "warningBGImage");
        int cautionCount = countOccurrences(html, "cautionBGImage");
        int noteCount = countOccurrences(html, "noteBlock");

        log.info("✅ 混合场景渲染验证:");
        log.info("   - warning数量: {}", warningCount);
        log.info("   - caution数量: {}", cautionCount);
        log.info("   - note数量: {}", noteCount);
        log.info("   - 总HTML长度: {} 字节", html.length());

        // 至少应该渲染出这些样式类
        assertTrue(warningCount > 0 || html.contains("wcBorderSpacing"),
                   "应该有warning样式");
        assertTrue(cautionCount > 0 || html.contains("wcBorderSpacing"),
                   "应该有caution样式");
        assertTrue(noteCount > 0 || html.contains("noteTitle"),
                   "应该有note样式");
    }

    /**
     * 测试6: CSS样式验证 - 检查main.css中的样式定义
     */
    @Test
    @DisplayName("测试CSS样式定义验证")
    public void testCssStyleDefinitions() {
        // 这个测试验证CSS文件中是否有正确的样式定义
        // 实际上CSS是在后端通过DmXsltTransformer.enhancePreviewHtml()注入的

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <identAndStatusSection>\n" +
                "    <dmAddress>\n" +
                "      <dmIdent>\n" +
                "        <dmCode modelIdentCode=\"TEST\" systemDiffCode=\"A\" systemCode=\"00\"\n" +
                "                subSystemCode=\"0\" subSubSystemCode=\"0\" assyCode=\"00\"\n" +
                "                disassyCode=\"00\" disassyCodeVariant=\"A\" infoCode=\"000\"\n" +
                "                infoCodeVariant=\"A\" itemLocationCode=\"A\"/>\n" +
                "      </dmIdent>\n" +
                "    </dmAddress>\n" +
                "  </identAndStatusSection>\n" +
                "  <content>\n" +
                "    <description>\n" +
                "      <levelledPara>\n" +
                "        <warning>\n" +
                "          <warningAndCautionPara>测试CSS</warningAndCautionPara>\n" +
                "        </warning>\n" +
                "      </levelledPara>\n" +
                "    </description>\n" +
                "  </content>\n" +
                "</dmodule>";

        String html = DmXmlHelper.renderHtml(xml, "/ietm");

        // 验证HTML中是否包含CSS样式（应该由后端注入）
        boolean hasCss = html.contains("<style>") || html.contains("stylesheet");

        // 验证关键样式类定义
        boolean hasWcStyles = html.contains("wcContentArea") ||
                              html.contains("wcTitle") ||
                              html.contains("wcBorderSpacing");

        log.info("✅ CSS样式验证:");
        log.info("   - CSS注入: {}", hasCss ? "✓ 已注入" : "⚠ 未找到<style>标签");
        log.info("   - warning/caution样式类: {}", hasWcStyles ? "✓ 使用中" : "✗ 未使用");

        // 根据main.css（456-486行），这些样式应该存在：
        // .wcContentArea: background-color: white; border-style: solid; border-width: 3pt;
        // .wcTitle: font-weight: bold; text-align: center;
        // .noteBlock: margin-bottom: 15pt; margin-top: 15pt;

        assertTrue(hasWcStyles, "应该使用warning/caution的样式类");
    }

    /**
     * 辅助方法：统计子串出现次数
     */
    private int countOccurrences(String str, String substr) {
        if (str == null || substr == null || substr.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substr, index)) != -1) {
            count++;
            index += substr.length();
        }
        return count;
    }
}
