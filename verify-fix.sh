#!/bin/bash
# IETM ICN附件查询修复验证脚本
# 创建时间: 2026-09-17
# 用途: 验证business_id字段修复后DM预览功能是否正常

echo "=================================================="
echo "  IETM ICN附件查询修复验证脚本"
echo "=================================================="
echo ""

# 配置
BASE_URL="http://localhost:9999/jeecg-boot"
BACKEND_LOG="/c/Users/86135/AppData/Local/Temp/claude/C--Users-86135/d0a81bb7-42d8-4fb1-891c-4962236a4384/tasks/b0teaw9x6.output"

echo "1️⃣  检查后端服务状态..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/")
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ] || [ "$HTTP_CODE" = "404" ]; then
    echo "   ✅ 后端服务正常运行 (HTTP $HTTP_CODE)"
else
    echo "   ❌ 后端服务异常 (HTTP $HTTP_CODE)"
    exit 1
fi
echo ""

echo "2️⃣  检查启动日志中是否有business_id错误..."
if grep -q "无效的列名\[business_id\]" "$BACKEND_LOG" 2>/dev/null; then
    echo "   ❌ 发现business_id错误，修复未生效！"
    echo ""
    echo "   错误日志："
    grep -A 3 "无效的列名\[business_id\]" "$BACKEND_LOG" | head -10
    exit 1
else
    echo "   ✅ 未发现business_id错误"
fi
echo ""

echo "3️⃣  检查IETM模块是否成功加载..."
if grep -q "ietmAttachmentMapper" "$BACKEND_LOG" 2>/dev/null; then
    echo "   ✅ IETM附件Mapper已加载"
else
    echo "   ⚠️  未找到附件Mapper加载记录"
fi
echo ""

echo "4️⃣  检查修复的JAR包是否已部署..."
JAR_PATH="/d/maven/jeecgboot/org/jeecgframework/boot/jeecg-module-ietm/3.4.4/jeecg-module-ietm-3.4.4.jar"
if [ -f "$JAR_PATH" ]; then
    JAR_SIZE=$(ls -lh "$JAR_PATH" | awk '{print $5}')
    JAR_TIME=$(ls -l "$JAR_PATH" | awk '{print $6, $7, $8}')
    echo "   ✅ 找到IETM模块JAR包"
    echo "      路径: $JAR_PATH"
    echo "      大小: $JAR_SIZE"
    echo "      修改时间: $JAR_TIME"

    # 检查是否是今天编译的
    TODAY=$(date +%Y-%m-%d)
    if echo "$JAR_TIME" | grep -q "$TODAY\|$(date +%m-%d)"; then
        echo "   ✅ JAR包是最新编译的（今天）"
    else
        echo "   ⚠️  JAR包不是今天编译的，可能未更新"
    fi
else
    echo "   ⚠️  未找到Maven本地仓库中的JAR包"
fi
echo ""

echo "5️⃣  测试基本API连接..."
# 测试健康检查接口（不需要登录）
HEALTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null)
if [ "$HEALTH_CODE" = "200" ]; then
    echo "   ✅ 健康检查接口正常 (HTTP $HEALTH_CODE)"
else
    echo "   ⚠️  健康检查接口返回 HTTP $HEALTH_CODE"
fi
echo ""

echo "=================================================="
echo "  自动验证完成"
echo "=================================================="
echo ""
echo "📋 手动验证清单（需要在浏览器中操作）："
echo ""
echo "1. 打开前端: http://localhost:3000"
echo "2. 登录系统（使用您的账号密码）"
echo "3. 进入【数据模块管理】页面"
echo "4. 找一个包含ICN图符的DM（XML中有<graphic>标签）"
echo "5. 点击【预览】按钮"
echo ""
echo "✅ 预期结果:"
echo "   - 预览窗口正常打开"
echo "   - ICN图片正常显示（不是占位图）"
echo "   - 后端日志显示: 'ICN路径替换完成: 总数=X, 成功=X, 失败=0'"
echo ""
echo "❌ 如果失败:"
echo "   - 检查后端日志: tail -f $BACKEND_LOG"
echo "   - 查看是否有'加载ICN图片失败'错误"
echo "   - 查看是否有'business_id'相关错误"
echo ""
echo "📁 相关文档:"
echo "   - 修复报告: D:\\workspace\\IETM\\cape-ietm-java\\docs\\fix-reports\\IETM-ICN附件查询字段错误修复报告-20260917.md"
echo "   - 部署指南: D:\\workspace\\IETM\\cape-ietm-java\\docs\\fix-reports\\IETM-ICN附件查询修复-部署验证指南-20260917.md"
echo ""
echo "如有问题，请提供以下信息："
echo "1. 完整的后端错误日志"
echo "2. 测试的DM ID"
echo "3. 浏览器开发者工具的Network请求详情"
echo ""
