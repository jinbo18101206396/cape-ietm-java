package org.jeecg.modules.ietm.ietmddn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DDN模板路径加载验证测试
 */
@SpringBootTest
public class DdnTemplatePathTest {

    /**
     * 测试：S1000D 4.0模板路径加载
     */
    @Test
    public void testLoadS1000D40Template() {
        String templatePath = "ietm/S1000D40/template/ddn.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);

        assertNotNull(is, "S1000D 4.0模板文件应该存在：" + templatePath);
        System.out.println("✅ S1000D 4.0模板加载成功：" + templatePath);
    }

    /**
     * 测试：S1000D 4.1模板路径加载
     */
    @Test
    public void testLoadS1000D41Template() {
        String templatePath = "ietm/S1000D41/template/ddn.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);

        assertNotNull(is, "S1000D 4.1模板文件应该存在：" + templatePath);
        System.out.println("✅ S1000D 4.1模板加载成功：" + templatePath);
    }

    /**
     * 测试：S1000D 4.2模板路径加载
     */
    @Test
    public void testLoadS1000D42Template() {
        String templatePath = "ietm/S1000D42/template/ddn.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);

        assertNotNull(is, "S1000D 4.2模板文件应该存在：" + templatePath);
        System.out.println("✅ S1000D 4.2模板加载成功：" + templatePath);
    }

    /**
     * 测试：动态路径拼接
     */
    @Test
    public void testDynamicTemplatePath() {
        String[] standards = {"S1000D40", "S1000D41", "S1000D42"};

        for (String standard : standards) {
            String templatePath = String.format("ietm/%s/template/ddn.xml", standard);
            InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);

            assertNotNull(is, "模板文件应该存在：" + templatePath);
            System.out.println("✅ 动态路径加载成功：" + templatePath);
        }
    }

    /**
     * 测试：错误的路径格式
     */
    @Test
    public void testInvalidTemplatePath() {
        // 测试1：路径开头有斜杠（错误）
        String wrongPath1 = "/ietm/S1000D40/template/ddn.xml";
        InputStream is1 = getClass().getClassLoader().getResourceAsStream(wrongPath1);
        assertNull(is1, "带前导斜杠的路径不应该加载成功：" + wrongPath1);
        System.out.println("✅ 带前导斜杠的路径正确拒绝：" + wrongPath1);

        // 测试2：使用反斜杠（错误）
        String wrongPath2 = "ietm\\S1000D40\\template\\ddn.xml";
        InputStream is2 = getClass().getClassLoader().getResourceAsStream(wrongPath2);
        assertNull(is2, "使用反斜杠的路径不应该加载成功：" + wrongPath2);
        System.out.println("✅ 反斜杠路径正确拒绝：" + wrongPath2);

        // 测试3：正确的路径格式（对比）
        String correctPath = "ietm/S1000D40/template/ddn.xml";
        InputStream is3 = getClass().getClassLoader().getResourceAsStream(correctPath);
        assertNotNull(is3, "正确的路径应该加载成功：" + correctPath);
        System.out.println("✅ 正确路径格式验证通过：" + correctPath);
    }

    /**
     * 测试：不存在的标准版本
     */
    @Test
    public void testNonExistentStandard() {
        String templatePath = "ietm/S1000D50/template/ddn.xml";
        InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);

        assertNull(is, "不存在的标准版本应该返回null：" + templatePath);
        System.out.println("✅ 不存在的版本正确处理：" + templatePath);
    }

    /**
     * 测试：模拟projectInfo传递
     */
    @Test
    public void testProjectInfoSimulation() {
        // 模拟从projectInfo获取标准
        java.util.Map<String, Object> projectInfo = new java.util.HashMap<>();

        // 场景1：配置了S1000D41
        projectInfo.put("ietmStandard", "S1000D41");
        String standard1 = (String) projectInfo.get("ietmStandard");
        String path1 = String.format("ietm/%s/template/ddn.xml", standard1);
        InputStream is1 = getClass().getClassLoader().getResourceAsStream(path1);
        assertNotNull(is1, "应该加载S1000D41模板");
        System.out.println("✅ projectInfo模拟（S1000D41）：" + path1);

        // 场景2：未配置标准（使用默认值）
        projectInfo.clear();
        String standard2 = (String) projectInfo.get("ietmStandard");
        if (standard2 == null || standard2.isEmpty()) {
            standard2 = "S1000D40";  // 默认值
        }
        String path2 = String.format("ietm/%s/template/ddn.xml", standard2);
        InputStream is2 = getClass().getClassLoader().getResourceAsStream(path2);
        assertNotNull(is2, "未配置时应该使用默认S1000D40模板");
        System.out.println("✅ projectInfo模拟（默认）：" + path2);
    }

    /**
     * 综合测试：完整路径拼接流程
     */
    @Test
    public void testCompletePathConstruction() {
        System.out.println("\n========== 完整路径拼接流程测试 ==========");

        // 模拟实际代码的路径构建逻辑
        String[] testCases = {
            "S1000D40",
            "S1000D41",
            "S1000D42",
            null,        // 测试null
            "",          // 测试空字符串
        };

        for (String ietmStandard : testCases) {
            System.out.println("\n测试标准：" + ietmStandard);

            // 模拟代码逻辑
            String finalStandard = ietmStandard;
            if (finalStandard == null || finalStandard.isEmpty()) {
                finalStandard = "S1000D40";
                System.out.println("  → 使用默认值：" + finalStandard);
            }

            String templatePath = String.format("ietm/%s/template/ddn.xml", finalStandard);
            System.out.println("  → 构建路径：" + templatePath);

            InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath);
            if (is != null) {
                System.out.println("  → 结果：✅ 加载成功");
            } else {
                System.out.println("  → 结果：❌ 加载失败");
            }

            // S1000D40/41/42都应该加载成功
            if ("S1000D40".equals(finalStandard) ||
                "S1000D41".equals(finalStandard) ||
                "S1000D42".equals(finalStandard)) {
                assertNotNull(is, "标准版本" + finalStandard + "应该加载成功");
            }
        }

        System.out.println("\n========== 测试完成 ==========\n");
    }
}
