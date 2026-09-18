package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ApplicabilityEvaluator 单元测试
 *
 * @author IETM Team
 * @date 2026-09-18
 */
public class ApplicabilityEvaluatorTest {

    @Test
    public void testSimpleEquality() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("modelCode", "A");

        assertTrue("相等比较应返回true",
            ApplicabilityEvaluator.evaluate("modelCode = 'A'", context));

        assertFalse("不相等比较应返回false",
            ApplicabilityEvaluator.evaluate("modelCode = 'B'", context));
    }

    @Test
    public void testNotEquality() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("environment", "flight");

        assertTrue("不等于比较应返回true",
            ApplicabilityEvaluator.evaluate("environment != 'ground'", context));

        assertFalse("不等于比较应返回false",
            ApplicabilityEvaluator.evaluate("environment != 'flight'", context));
    }

    @Test
    public void testNumericComparison() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("version", "2.5");

        assertTrue("大于比较", ApplicabilityEvaluator.evaluate("version > '2.0'", context));
        assertTrue("大于等于比较", ApplicabilityEvaluator.evaluate("version >= '2.5'", context));
        assertTrue("小于比较", ApplicabilityEvaluator.evaluate("version < '3.0'", context));
        assertTrue("小于等于比较", ApplicabilityEvaluator.evaluate("version <= '2.5'", context));
        assertFalse("大于比较失败", ApplicabilityEvaluator.evaluate("version > '3.0'", context));
    }

    @Test
    public void testAndOperator() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("modelCode", "A");
        context.set("serialNumber", "001");

        assertTrue("AND两者为true",
            ApplicabilityEvaluator.evaluate("modelCode = 'A' AND serialNumber = '001'", context));

        assertFalse("AND一个为false",
            ApplicabilityEvaluator.evaluate("modelCode = 'A' AND serialNumber = '002'", context));

        assertFalse("AND两者为false",
            ApplicabilityEvaluator.evaluate("modelCode = 'B' AND serialNumber = '002'", context));
    }

    @Test
    public void testOrOperator() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("modelCode", "A");
        context.set("serialNumber", "001");

        assertTrue("OR两者为true",
            ApplicabilityEvaluator.evaluate("modelCode = 'A' OR serialNumber = '002'", context));

        assertTrue("OR一个为true",
            ApplicabilityEvaluator.evaluate("modelCode = 'B' OR serialNumber = '001'", context));

        assertFalse("OR两者为false",
            ApplicabilityEvaluator.evaluate("modelCode = 'B' OR serialNumber = '002'", context));
    }

    @Test
    public void testNotOperator() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("environment", "flight");

        assertTrue("NOT false = true",
            ApplicabilityEvaluator.evaluate("NOT environment = 'ground'", context));

        assertFalse("NOT true = false",
            ApplicabilityEvaluator.evaluate("NOT environment = 'flight'", context));
    }

    @Test
    public void testInOperator() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("serialNumber", "002");

        assertTrue("IN操作符匹配",
            ApplicabilityEvaluator.evaluate("serialNumber IN ['001', '002', '003']", context));

        assertFalse("IN操作符不匹配",
            ApplicabilityEvaluator.evaluate("serialNumber IN ['001', '003', '005']", context));
    }

    @Test
    public void testParentheses() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("modelCode", "A");
        context.set("environment", "flight");
        context.set("version", "2.0");

        // (A OR B) AND C
        assertTrue("括号优先级1",
            ApplicabilityEvaluator.evaluate(
                "(modelCode = 'A' OR modelCode = 'B') AND environment = 'flight'", context));

        // A OR (B AND C)
        assertTrue("括号优先级2",
            ApplicabilityEvaluator.evaluate(
                "modelCode = 'B' OR (environment = 'flight' AND version = '2.0')", context));

        assertFalse("括号优先级3",
            ApplicabilityEvaluator.evaluate(
                "(modelCode = 'B' OR modelCode = 'C') AND environment = 'ground'", context));
    }

    @Test
    public void testComplexExpression() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("modelCode", "A");
        context.set("serialNumber", "002");
        context.set("environment", "flight");
        context.set("version", "2.5");

        // 复杂表达式: ((A OR B) AND C) OR (D AND E)
        String expr = "((modelCode = 'A' OR modelCode = 'B') AND environment = 'flight') " +
                     "OR (serialNumber = '999' AND version > '3.0')";

        assertTrue("复杂表达式", ApplicabilityEvaluator.evaluate(expr, context));
    }

    @Test
    public void testEmptyExpression() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();

        assertTrue("空表达式默认适用",
            ApplicabilityEvaluator.evaluate("", context));

        assertTrue("null表达式默认适用",
            ApplicabilityEvaluator.evaluate(null, context));
    }

    @Test
    public void testUndefinedVariable() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("modelCode", "A");

        // 未定义的变量应返回false（安全策略）
        assertFalse("未定义变量",
            ApplicabilityEvaluator.evaluate("undefinedVar = 'value'", context));
    }

    @Test
    public void testStringComparison() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("name", "Boeing");

        assertTrue("字符串相等", ApplicabilityEvaluator.evaluate("name = 'Boeing'", context));
        assertTrue("字符串大于", ApplicabilityEvaluator.evaluate("name > 'Airbus'", context));
        assertTrue("字符串小于", ApplicabilityEvaluator.evaluate("name < 'Cessna'", context));
    }

    @Test
    public void testCaseInsensitiveOperators() {
        ApplicabilityEvaluator.ApplicabilityContext context = new ApplicabilityEvaluator.ApplicabilityContext();
        context.set("modelCode", "A");
        context.set("serialNumber", "001");

        // 测试大小写不敏感的运算符
        assertTrue("AND大写",
            ApplicabilityEvaluator.evaluate("modelCode = 'A' AND serialNumber = '001'", context));

        assertTrue("and小写",
            ApplicabilityEvaluator.evaluate("modelCode = 'A' and serialNumber = '001'", context));

        assertTrue("OR大写",
            ApplicabilityEvaluator.evaluate("modelCode = 'A' OR serialNumber = '002'", context));

        assertTrue("or小写",
            ApplicabilityEvaluator.evaluate("modelCode = 'A' or serialNumber = '002'", context));
    }

    @Test
    public void testRealWorldScenarios() {
        // 场景1：航空器序列号过滤
        ApplicabilityEvaluator.ApplicabilityContext context1 = new ApplicabilityEvaluator.ApplicabilityContext();
        context1.set("serialNumber", "001");
        context1.set("modelCode", "B737-800");

        assertTrue("场景1：特定序列号",
            ApplicabilityEvaluator.evaluate(
                "serialNumber IN ['001', '002', '003'] AND modelCode = 'B737-800'", context1));

        // 场景2：维护环境过滤
        ApplicabilityEvaluator.ApplicabilityContext context2 = new ApplicabilityEvaluator.ApplicabilityContext();
        context2.set("environment", "flight");
        context2.set("altitude", "35000");

        assertTrue("场景2：飞行环境",
            ApplicabilityEvaluator.evaluate(
                "environment = 'flight' AND altitude > '10000'", context2));

        // 场景3：版本升级过滤
        ApplicabilityEvaluator.ApplicabilityContext context3 = new ApplicabilityEvaluator.ApplicabilityContext();
        context3.set("softwareVersion", "2.1.5");
        context3.set("hardwareVersion", "3.0");

        assertTrue("场景3：版本要求",
            ApplicabilityEvaluator.evaluate(
                "softwareVersion >= '2.0' AND hardwareVersion >= '3.0'", context3));
    }

    @Test
    public void testDefaultContext() {
        ApplicabilityEvaluator.ApplicabilityContext context =
            ApplicabilityEvaluator.createDefaultContext();

        assertNotNull("默认上下文不为null", context);
        assertEquals("默认序列号", "001", context.get("serialNumber"));
        assertEquals("默认型号", "A", context.get("modelCode"));
        assertEquals("默认版本", "1.0", context.get("version"));
        assertEquals("默认环境", "ground", context.get("environment"));
    }
}
