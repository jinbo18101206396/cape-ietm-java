#!/bin/bash
# 回归测试脚本 - 验证缓存回滚后系统功能正常
# 2026-09-18

echo "=========================================="
echo "   IETM 系统回归测试"
echo "=========================================="
echo ""

# 测试计数器
TOTAL=0
PASSED=0
FAILED=0

# 测试函数
run_test() {
    local test_name=$1
    local test_class=$2

    echo "运行测试: $test_name"
    TOTAL=$((TOTAL + 1))

    mvn test -Dtest=$test_class -pl jeecg-module-ietm -DskipTests=false -q 2>&1 | grep -q "BUILD SUCCESS"

    if [ $? -eq 0 ]; then
        echo "  ✅ 通过"
        PASSED=$((PASSED + 1))
    else
        echo "  ❌ 失败"
        FAILED=$((FAILED + 1))
    fi
    echo ""
}

echo "第1层: 编译测试"
echo "----------------------------------------"
mvn clean compile -pl jeecg-module-ietm -am -DskipTests -q
if [ $? -eq 0 ]; then
    echo "✅ 编译通过"
else
    echo "❌ 编译失败"
    exit 1
fi
echo ""

echo "第2层: 核心工具类单元测试"
echo "----------------------------------------"
run_test "DM XML处理 - 贪婪清理" "DmXmlHelperGreedyTest"
run_test "DM XML处理 - 遗留脚本" "DmXmlHelperLegacyScriptTest"
run_test "预览CSS生成器" "PreviewCssGeneratorTest"
run_test "性能监控器" "PreviewPerformanceMonitorTest"
run_test "适用性评估器" "ApplicabilityEvaluatorTest"

echo "=========================================="
echo "   测试总结"
echo "=========================================="
echo "总计: $TOTAL"
echo "通过: $PASSED"
echo "失败: $FAILED"
echo "通过率: $(echo "scale=2; $PASSED * 100 / $TOTAL" | bc)%"
echo ""

if [ $FAILED -eq 0 ]; then
    echo "✅ 所有测试通过！"
    exit 0
else
    echo "❌ 部分测试失败"
    exit 1
fi
