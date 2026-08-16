package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BUG #4 和 BUG #5 修复测试
 * 测试 Matcher.replaceAll() 必须使用 Matcher.quoteReplacement()
 */
@DisplayName("Java Matcher.replaceAll quoteReplacement 修复测试")
class MatcherQuoteReplacementTest {

    private static final Pattern ISSUE_INFO_PATTERN = Pattern.compile(
        "<issueInfo[^>]*/>",
        Pattern.CASE_INSENSITIVE
    );

    @Test
    @DisplayName("BUG #4/5: replaceAll不加quoteReplacement会抛异常")
    void testReplaceAllWithoutQuote_ThrowsException() {
        String xmlContent = "<dmodule><issueInfo issueNumber=\"001\" inWork=\"00\"/></dmodule>";

        // 模拟包含$的版本号（极端场景）
        String newTag = "<issueInfo issueNumber=\"001\" inWork=\"00$test\"/>";

        // ❌ 错误写法：直接replaceAll
        Matcher matcher = ISSUE_INFO_PATTERN.matcher(xmlContent);
        assertThrows(IllegalArgumentException.class, () -> {
            matcher.replaceAll(newTag);
        }, "包含$的替换字符串应该抛出IllegalArgumentException");
    }

    @Test
    @DisplayName("BUG #4/5修复：使用quoteReplacement正常处理$字符")
    void testReplaceAllWithQuote_HandlesSpecialChars() {
        String xmlContent = "<dmodule><issueInfo issueNumber=\"001\" inWork=\"00\"/></dmodule>";

        // 包含$的版本号
        String newTag = "<issueInfo issueNumber=\"001\" inWork=\"00$test\"/>";

        // ✅ 正确写法：使用quoteReplacement
        Matcher matcher = ISSUE_INFO_PATTERN.matcher(xmlContent);
        String result = matcher.replaceAll(Matcher.quoteReplacement(newTag));

        // 验证：$应该被保留为字面量
        assertTrue(result.contains("inWork=\"00$test\""),
            "$字符应该被保留");
        assertEquals("<dmodule><issueInfo issueNumber=\"001\" inWork=\"00$test\"/></dmodule>", result);
    }

    @Test
    @DisplayName("BUG #4/5修复：处理反斜杠字符")
    void testReplaceAllWithQuote_HandlesBackslash() {
        String xmlContent = "<dmodule><issueInfo issueNumber=\"001\" inWork=\"00\"/></dmodule>";

        // 包含\的版本号（极端场景）
        String newTag = "<issueInfo issueNumber=\"001\" inWork=\"00\\test\"/>";

        // ✅ 使用quoteReplacement
        Matcher matcher = ISSUE_INFO_PATTERN.matcher(xmlContent);
        String result = matcher.replaceAll(Matcher.quoteReplacement(newTag));

        // 验证：\应该被保留为字面量
        assertTrue(result.contains("inWork=\"00\\test\""),
            "反斜杠应该被保留");
    }

    @Test
    @DisplayName("正常情况：纯数字版本号两种方式结果一致")
    void testReplaceAll_NormalCase_BothWaysWork() {
        String xmlContent = "<dmodule><issueInfo issueNumber=\"001\" inWork=\"00\"/></dmodule>";

        // 纯数字版本号（常规场景）
        String newTag = "<issueInfo issueNumber=\"002\" inWork=\"01\"/>";

        // 两种写法结果应该一致
        String result1 = ISSUE_INFO_PATTERN.matcher(xmlContent).replaceAll(newTag);
        String result2 = ISSUE_INFO_PATTERN.matcher(xmlContent).replaceAll(
            Matcher.quoteReplacement(newTag)
        );

        assertEquals(result1, result2, "纯数字版本号时两种写法应该一致");
        assertEquals("<dmodule><issueInfo issueNumber=\"002\" inWork=\"01\"/></dmodule>", result1);
    }

    @Test
    @DisplayName("边界测试：XML转义后的字符")
    void testReplaceAllWithQuote_EscapedXml() {
        String xmlContent = "<dmodule><issueInfo issueNumber=\"001\" inWork=\"00\"/></dmodule>";

        // 已经XML转义的版本号（包含&amp;）
        String newTag = "<issueInfo issueNumber=\"001\" inWork=\"00&amp;test\"/>";

        // ✅ 使用quoteReplacement
        String result = ISSUE_INFO_PATTERN.matcher(xmlContent).replaceAll(
            Matcher.quoteReplacement(newTag)
        );

        // 验证：&amp;应该被保留
        assertTrue(result.contains("inWork=\"00&amp;test\""),
            "XML转义字符应该被保留");
    }

    @Test
    @DisplayName("quoteReplacement功能验证")
    void testQuoteReplacementBehavior() {
        // quoteReplacement的作用：转义$和\

        // 测试$转义
        String withDollar = "test$1";
        String quoted1 = Matcher.quoteReplacement(withDollar);
        assertEquals("test\\$1", quoted1, "$应该被转义为\\$");

        // 测试\转义
        String withBackslash = "test\\n";
        String quoted2 = Matcher.quoteReplacement(withBackslash);
        assertEquals("test\\\\n", quoted2, "\\应该被转义为\\\\");

        // 测试普通字符不变
        String normal = "test123";
        String quoted3 = Matcher.quoteReplacement(normal);
        assertEquals("test123", quoted3, "普通字符应该不变");
    }

    @Test
    @DisplayName("IetmDataModuleServiceImpl.java:1491 真实场景模拟")
    void testIetmDataModuleServiceImpl_RealScenario() {
        // 模拟完整XML
        String xmlContent = "<dmodule>\n" +
            "  <identAndStatusSection>\n" +
            "    <issueInfo issueNumber=\"001\" inWork=\"00\"/>\n" +
            "  </identAndStatusSection>\n" +
            "</dmodule>";

        // 模拟数据库版本号
        String dbIssueNo = "002";
        String dbInWork = "01";

        // escapeXml模拟
        String newTag = String.format(
            "<issueInfo issueNumber=\"%s\" inWork=\"%s\"/>",
            escapeXml(dbIssueNo),
            escapeXml(dbInWork)
        );

        // ✅ 修复后的写法
        String result = ISSUE_INFO_PATTERN.matcher(xmlContent).replaceAll(
            Matcher.quoteReplacement(newTag)
        );

        // 验证
        assertTrue(result.contains("issueNumber=\"002\""));
        assertTrue(result.contains("inWork=\"01\""));
        assertFalse(result.contains("issueNumber=\"001\""));
    }

    @Test
    @DisplayName("IetmDmContentServiceImpl.java:223 真实场景模拟")
    void testIetmDmContentServiceImpl_RealScenario() {
        String xmlContent = "<dmodule><issueInfo issueNumber=\"001\" inWork=\"00\"/></dmodule>";

        String dbIssueNo = "003";
        String dbInWork = "02";

        // escapeXmlAttr模拟
        String newTag = String.format(
            "<issueInfo issueNumber=\"%s\" inWork=\"%s\"/>",
            escapeXmlAttr(dbIssueNo),
            escapeXmlAttr(dbInWork)
        );

        // ✅ 修复后的写法
        String result = ISSUE_INFO_PATTERN.matcher(xmlContent).replaceAll(
            Matcher.quoteReplacement(newTag)
        );

        assertEquals("<dmodule><issueInfo issueNumber=\"003\" inWork=\"02\"/></dmodule>", result);
    }

    // ── 辅助方法（模拟实际代码） ──

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    private String escapeXmlAttr(String val) {
        if (val == null) return "";
        return val.replace("&", "&amp;")
                  .replace("\"", "&quot;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;");
    }
}
