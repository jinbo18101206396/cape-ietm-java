#!/bin/bash
# 系统、全面、深度测试验证报告
# 2026-09-18

echo "=========================================="
echo "   IETM 系统测试验证报告"
echo "=========================================="
echo ""

cd /d/workspace/IETM/cape-ietm-java

echo "【第1层：编译测试】"
echo "----------------------------------------"
mvn clean compile -pl jeecg-module-ietm -am -DskipTests 2>&1 | grep -E "BUILD SUCCESS|BUILD FAILURE"
echo ""

echo "【第2层：核心单元测试】"
echo "----------------------------------------"

echo "测试1: DM XML处理 - 贪婪清理"
mvn test -Dtest=DmXmlHelperGreedyTest -pl jeecg-module-ietm -DskipTests=false 2>&1 | grep -A 2 "Tests run:"

echo "测试2: DM XML处理 - 遗留脚本"
mvn test -Dtest=DmXmlHelperLegacyScriptTest -pl jeecg-module-ietm -DskipTests=false 2>&1 | grep -A 2 "Tests run:"

echo "测试3: 预览CSS生成器"
mvn test -Dtest=PreviewCssGeneratorTest -pl jeecg-module-ietm -DskipTests=false 2>&1 | grep -A 2 "Tests run:"

echo "测试4: 性能监控器"
mvn test -Dtest=PreviewPerformanceMonitorTest -pl jeecg-module-ietm -DskipTests=false 2>&1 | grep -A 2 "Tests run:"

echo "测试5: 适用性评估器"
mvn test -Dtest=ApplicabilityEvaluatorTest -pl jeecg-module-ietm -DskipTests=false 2>&1 | grep -A 2 "Tests run:"

echo ""
echo "=========================================="
echo "   测试验证完成"
echo "=========================================="
