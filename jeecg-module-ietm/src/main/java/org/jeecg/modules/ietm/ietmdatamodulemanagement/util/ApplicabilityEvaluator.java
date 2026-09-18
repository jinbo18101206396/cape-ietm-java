package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S1000D适用性表达式评估器
 *
 * 支持S1000D标准的适用性过滤表达式：
 * - 逻辑运算符：AND, OR, NOT
 * - 比较运算符：=, !=, >, <, >=, <=
 * - 集合运算符：IN
 * - 括号分组
 *
 * 示例表达式：
 * - "serialNumber = '001'"
 * - "modelCode = 'A' AND environment != 'ground'"
 * - "serialNumber IN ['001', '002', '003']"
 * - "(modelCode = 'A' OR modelCode = 'B') AND version >= '2.0'"
 *
 * @author IETM Team
 * @date 2026-09-18
 */
@Slf4j
public class ApplicabilityEvaluator {

    /**
     * 适用性上下文
     */
    @Data
    public static class ApplicabilityContext {
        private Map<String, String> variables = new HashMap<>();

        public ApplicabilityContext() {}

        public ApplicabilityContext(Map<String, String> variables) {
            this.variables = variables;
        }

        public void set(String key, String value) {
            variables.put(key, value);
        }

        public String get(String key) {
            return variables.get(key);
        }

        public boolean has(String key) {
            return variables.containsKey(key);
        }
    }

    /**
     * 评估适用性表达式
     *
     * @param expression 适用性表达式
     * @param context 用户配置上下文
     * @return true=适用，false=不适用
     */
    public static boolean evaluate(String expression, ApplicabilityContext context) {
        if (expression == null || expression.trim().isEmpty()) {
            // 空表达式默认适用
            return true;
        }

        try {
            // 1. 预处理：移除多余空白
            expression = expression.trim();

            // 2. 解析并评估表达式
            return evaluateExpression(expression, context);

        } catch (Exception e) {
            log.error("适用性表达式评估失败: expression={}", expression, e);
            // 评估失败时默认适用（安全策略）
            return true;
        }
    }

    /**
     * 递归评估表达式
     */
    private static boolean evaluateExpression(String expr, ApplicabilityContext context) {
        expr = expr.trim();

        // 处理最外层括号（完整包裹）
        if (expr.startsWith("(") && hasMatchingClosingParen(expr, 0)) {
            // 移除外层括号
            return evaluateExpression(expr.substring(1, expr.length() - 1), context);
        }

        // 处理 OR 运算符（优先级最低）
        int orIndex = findOperatorIndex(expr, "OR");
        if (orIndex != -1) {
            String left = expr.substring(0, orIndex).trim();
            String right = expr.substring(orIndex + 2).trim();
            return evaluateExpression(left, context) || evaluateExpression(right, context);
        }

        // 处理 AND 运算符
        int andIndex = findOperatorIndex(expr, "AND");
        if (andIndex != -1) {
            String left = expr.substring(0, andIndex).trim();
            String right = expr.substring(andIndex + 3).trim();
            return evaluateExpression(left, context) && evaluateExpression(right, context);
        }

        // 处理 NOT 运算符
        if (expr.toUpperCase().startsWith("NOT ")) {
            String innerExpr = expr.substring(4).trim();
            return !evaluateExpression(innerExpr, context);
        }

        // 处理 IN 运算符
        Pattern inPattern = Pattern.compile("(\\w+)\\s+IN\\s+\\[(.+?)\\]", Pattern.CASE_INSENSITIVE);
        Matcher inMatcher = inPattern.matcher(expr);
        if (inMatcher.matches()) {
            String varName = inMatcher.group(1);
            String valuesStr = inMatcher.group(2);
            return evaluateInOperator(varName, valuesStr, context);
        }

        // 处理比较运算符
        return evaluateComparison(expr, context);
    }

    /**
     * 检查字符串从指定位置开始的括号是否在字符串末尾匹配
     */
    private static boolean hasMatchingClosingParen(String expr, int startIndex) {
        if (!expr.startsWith("(")) {
            return false;
        }

        int depth = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    // 找到匹配的右括号，检查是否在末尾
                    return i == expr.length() - 1;
                }
            }
        }
        return false;
    }

    /**
     * 查找运算符索引（忽略括号内的）
     */
    private static int findOperatorIndex(String expr, String operator) {
        int depth = 0;
        int opLength = operator.length();

        for (int i = 0; i <= expr.length() - opLength; i++) {
            char c = expr.charAt(i);

            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0) {
                // 在括号外部，检查是否匹配运算符
                if (i + opLength <= expr.length()) {
                    String sub = expr.substring(i, i + opLength);
                    if (sub.equalsIgnoreCase(operator)) {
                        // 确保是完整的单词（前后是空白或边界）
                        boolean beforeOk = (i == 0) || Character.isWhitespace(expr.charAt(i - 1));
                        boolean afterOk = (i + opLength >= expr.length()) ||
                                         Character.isWhitespace(expr.charAt(i + opLength));
                        if (beforeOk && afterOk) {
                            return i;
                        }
                    }
                }
            }
        }

        return -1;
    }

    /**
     * 评估 IN 运算符
     */
    private static boolean evaluateInOperator(String varName, String valuesStr, ApplicabilityContext context) {
        String varValue = context.get(varName);
        if (varValue == null) {
            log.warn("变量未定义: {}", varName);
            return false;
        }

        // 解析值列表
        String[] values = valuesStr.split(",");
        for (String value : values) {
            value = value.trim().replaceAll("^['\"]|['\"]$", ""); // 移除引号
            if (varValue.equals(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 评估比较表达式
     */
    private static boolean evaluateComparison(String expr, ApplicabilityContext context) {
        // 支持的比较运算符
        String[] operators = {">=", "<=", "!=", "=", ">", "<"};

        for (String op : operators) {
            int index = expr.indexOf(op);
            if (index != -1) {
                String left = expr.substring(0, index).trim();
                String right = expr.substring(index + op.length()).trim();

                // 获取左侧变量值
                String leftValue = context.get(left);
                if (leftValue == null) {
                    log.warn("变量未定义: {}", left);
                    return false;
                }

                // 获取右侧值（可能是变量或字面量）
                String rightValue = right.replaceAll("^['\"]|['\"]$", ""); // 移除引号

                // 执行比较
                return compare(leftValue, rightValue, op);
            }
        }

        log.warn("无法解析比较表达式: {}", expr);
        return false;
    }

    /**
     * 执行比较操作
     */
    private static boolean compare(String left, String right, String operator) {
        try {
            // 尝试作为数字比较
            try {
                double leftNum = Double.parseDouble(left);
                double rightNum = Double.parseDouble(right);
                return compareNumbers(leftNum, rightNum, operator);
            } catch (NumberFormatException e) {
                // 不是数字，作为字符串比较
                return compareStrings(left, right, operator);
            }
        } catch (Exception e) {
            log.error("比较操作失败: left={}, right={}, op={}", left, right, operator, e);
            return false;
        }
    }

    /**
     * 数字比较
     */
    private static boolean compareNumbers(double left, double right, String operator) {
        switch (operator) {
            case "=":
                return Math.abs(left - right) < 0.0001; // 浮点数相等比较
            case "!=":
                return Math.abs(left - right) >= 0.0001;
            case ">":
                return left > right;
            case "<":
                return left < right;
            case ">=":
                return left >= right;
            case "<=":
                return left <= right;
            default:
                return false;
        }
    }

    /**
     * 字符串比较
     */
    private static boolean compareStrings(String left, String right, String operator) {
        switch (operator) {
            case "=":
                return left.equals(right);
            case "!=":
                return !left.equals(right);
            case ">":
                return left.compareTo(right) > 0;
            case "<":
                return left.compareTo(right) < 0;
            case ">=":
                return left.compareTo(right) >= 0;
            case "<=":
                return left.compareTo(right) <= 0;
            default:
                return false;
        }
    }

    /**
     * 从XML属性解析适用性表达式
     *
     * S1000D中适用性可能通过applicRefId引用或直接嵌入表达式
     */
    public static String extractApplicabilityExpression(String applicRefId, String xmlContent) {
        if (applicRefId == null || applicRefId.isEmpty()) {
            return null;
        }

        // 简化实现：从XML中查找对应的适用性定义
        // 实际应该解析完整的applicCrossRefTable
        // 这里作为示例返回简单表达式

        // TODO: 完整实现需要解析 <applicCrossRefTable> 结构
        return null;
    }

    /**
     * 创建默认上下文（用于测试）
     */
    public static ApplicabilityContext createDefaultContext() {
        ApplicabilityContext context = new ApplicabilityContext();
        context.set("serialNumber", "001");
        context.set("modelCode", "A");
        context.set("version", "1.0");
        context.set("environment", "ground");
        return context;
    }
}
