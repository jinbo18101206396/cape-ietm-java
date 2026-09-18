#!/bin/bash
# IETM P0修复验证脚本
# 2026-09-18

echo "=========================================="
echo "  IETM P0级问题修复验证"
echo "=========================================="
echo ""

# 切换到项目目录
cd /d/workspace/IETM/cape-ietm-java

# 计数器
PASSED=0
FAILED=0
TOTAL=0

# 测试函数
test_case() {
    TOTAL=$((TOTAL + 1))
    echo -n "[$TOTAL] $1 ... "
}

pass() {
    PASSED=$((PASSED + 1))
    echo "✅ PASS"
}

fail() {
    FAILED=$((FAILED + 1))
    echo "❌ FAIL: $1"
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. 编译验证"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_case "编译IETM模块"
mvn clean compile -pl jeecg-module-ietm -am -DskipTests -q
if [ $? -eq 0 ]; then
    pass
else
    fail "编译失败"
fi

test_case "检查Caffeine依赖"
if grep -q "caffeine" jeecg-module-ietm/pom.xml; then
    pass
else
    fail "Caffeine依赖未添加"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2. P0-1修复验证：ICN缓存并发安全"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_case "检查Caffeine Cache导入"
if grep -q "com.github.benmanes.caffeine.cache.Cache" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "未导入Caffeine Cache"
fi

test_case "检查LinkedHashMap已移除"
if grep -q "LinkedHashMap<String, IcnCacheEntry> CACHE" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    fail "仍在使用LinkedHashMap"
else
    pass
fi

test_case "检查Caffeine.newBuilder()"
if grep -q "Caffeine.newBuilder()" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "未使用Caffeine"
fi

test_case "检查maximumWeight配置"
if grep -q "maximumWeight" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "未配置maximumWeight"
fi

test_case "检查synchronized已移除"
SYNC_COUNT=$(grep -c "synchronized (CACHE)" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java)
if [ "$SYNC_COUNT" -eq 0 ]; then
    pass
else
    fail "仍存在${SYNC_COUNT}处synchronized (CACHE)"
fi

test_case "检查getIfPresent()使用"
if grep -q "CACHE.getIfPresent" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "未使用getIfPresent()"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "3. P0-3修复验证：线程池优雅关闭"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_case "检查@Component注解"
if grep -q "@Component" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "未添加@Component注解"
fi

test_case "检查DisposableBean接口"
if grep -q "implements DisposableBean" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "未实现DisposableBean接口"
fi

test_case "检查destroy()方法"
if grep -q "public void destroy()" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "未实现destroy()方法"
fi

test_case "检查EXECUTOR.shutdown()"
if grep -q "EXECUTOR.shutdown()" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "未调用shutdown()"
fi

test_case "检查awaitTermination()"
if grep -q "awaitTermination" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "未调用awaitTermination()"
fi

test_case "检查shutdownNow()"
if grep -q "shutdownNow()" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "未调用shutdownNow()"
fi

test_case "检查守护线程改为非守护"
if grep -q "setDaemon(false)" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "仍使用守护线程"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "4. 打包验证"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_case "打包应用"
mvn package -pl jeecg-module-system/jeecg-system-start -am -DskipTests -q
if [ $? -eq 0 ]; then
    pass
else
    fail "打包失败"
fi

test_case "检查JAR文件"
if [ -f "jeecg-module-system/jeecg-system-start/target/jeecg-system-start-3.4.4.jar" ]; then
    JAR_SIZE=$(stat -f%z "jeecg-module-system/jeecg-system-start/target/jeecg-system-start-3.4.4.jar" 2>/dev/null || stat -c%s "jeecg-module-system/jeecg-system-start/target/jeecg-system-start-3.4.4.jar" 2>/dev/null)
    if [ "$JAR_SIZE" -gt 100000000 ]; then
        pass
    else
        fail "JAR文件过小: ${JAR_SIZE} bytes"
    fi
else
    fail "JAR文件不存在"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "5. 代码质量检查"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

test_case "检查修复注释"
if grep -q "修复P0-1" jeecg-module-ietm/src/main/java/org/jeecg/modules/ietm/ietmdatamodulemanagement/util/IcnLoader.java; then
    pass
else
    fail "缺少修复注释"
fi

test_case "检查文档产出"
if [ -f "docs/code-quality/2026-09-18-p0-fixes-completed.md" ]; then
    pass
else
    fail "P0修复报告不存在"
fi

test_case "检查审核报告"
if [ -f "docs/code-quality/2026-09-18-code-quality-audit-summary.md" ]; then
    pass
else
    fail "审核总结报告不存在"
fi

echo ""
echo "=========================================="
echo "  测试结果汇总"
echo "=========================================="
echo ""
echo "总计: $TOTAL"
echo "通过: $PASSED ✅"
echo "失败: $FAILED ❌"
echo ""

PASS_RATE=$((PASSED * 100 / TOTAL))
echo "通过率: $PASS_RATE%"

if [ $FAILED -eq 0 ]; then
    echo ""
    echo "🎉 所有测试通过！P0修复验证成功！"
    echo ""
    exit 0
else
    echo ""
    echo "⚠️  有 $FAILED 个测试失败，请检查。"
    echo ""
    exit 1
fi
