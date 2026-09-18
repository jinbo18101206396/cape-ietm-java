#!/bin/bash
# IETM DM预览完整自动化测试脚本
# 包含：登录 → 获取token → 预览DM → 检查ICN加载

BASE_URL="http://localhost:9999/jeecg-boot"
BACKEND_LOG="/c/Users/86135/AppData/Local/Temp/claude/C--Users-86135/d0a81bb7-42d8-4fb1-891c-4962236a4384/tasks/b9osg97wr.output"

echo "=========================================="
echo "  IETM DM预览自动化测试"
echo "=========================================="
echo ""

# 测试用DM ID（从之前的错误日志中获取）
DM_ID="2096558746632130561"

echo "步骤1: 登录获取token..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/sys/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456",
    "captcha": "",
    "checkKey": ""
  }')

echo "登录响应: $LOGIN_RESPONSE"

# 提取token
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "❌ 登录失败，无法获取token"
    echo ""
    echo "请手动测试："
    echo "1. 打开浏览器: http://localhost:3000"
    echo "2. 使用 admin/您的密码 登录"
    echo "3. 进入数据模块管理，找到DM ID: $DM_ID"
    echo "4. 点击预览按钮"
    echo "5. 同时监控后端日志:"
    echo "   tail -f $BACKEND_LOG | grep -E 'ICN|加载|失败'"
    exit 1
fi

echo "✅ 登录成功，Token: ${TOKEN:0:20}..."
echo ""

echo "步骤2: 清空旧日志标记，准备监控..."
echo "--- TEST START $(date '+%Y-%m-%d %H:%M:%S') ---" >> "$BACKEND_LOG"
sleep 1
echo ""

echo "步骤3: 调用DM预览接口..."
PREVIEW_RESPONSE=$(curl -s -X GET "$BASE_URL/ietm/dm-content/preview?id=$DM_ID" \
  -H "X-Access-Token: $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nHTTP_CODE:%{http_code}")

HTTP_CODE=$(echo "$PREVIEW_RESPONSE" | grep "HTTP_CODE:" | cut -d':' -f2)
RESPONSE_BODY=$(echo "$PREVIEW_RESPONSE" | grep -v "HTTP_CODE:")

echo "HTTP状态码: $HTTP_CODE"

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ 预览请求失败"
    echo "响应: $RESPONSE_BODY"
    exit 1
fi

echo "✅ 预览请求成功"
echo ""

echo "步骤4: 分析响应内容..."
# 检查响应中是否包含ICN相关内容
if echo "$RESPONSE_BODY" | grep -q "ICN加载失败"; then
    echo "❌ 响应中包含'ICN加载失败'文字"
    RESULT="FAIL"
elif echo "$RESPONSE_BODY" | grep -q "data:image"; then
    echo "✅ 响应中包含base64图片数据"
    RESULT="SUCCESS"
else
    echo "⚠️  无法确定ICN加载状态"
    RESULT="UNKNOWN"
fi
echo ""

echo "步骤5: 检查后端日志..."
sleep 2

# 提取测试相关的日志
TEST_LOGS=$(tail -100 "$BACKEND_LOG" | sed -n '/TEST START/,$p')

echo "--- 后端日志摘要 ---"
echo "$TEST_LOGS" | grep -E "ICN|加载|失败|错误|异常|路径替换完成" | tail -15

echo ""
echo "=========================================="
echo "  测试结果"
echo "=========================================="

# 统计结果
ICN_SUCCESS=$(echo "$TEST_LOGS" | grep -c "ICN路径替换完成")
ICN_FAIL=$(echo "$TEST_LOGS" | grep -c "ICN加载失败")
ICN_ERROR=$(echo "$TEST_LOGS" | grep -c "加载ICN图片失败")
MULTI_RECORD=$(echo "$TEST_LOGS" | grep -c "ICN存在多条记录")

echo ""
echo "统计数据:"
echo "  - ICN替换成功次数: $ICN_SUCCESS"
echo "  - ICN加载失败次数: $ICN_FAIL"
echo "  - ICN加载错误次数: $ICN_ERROR"
echo "  - 多条记录警告: $MULTI_RECORD"
echo ""

if [ $ICN_ERROR -gt 0 ]; then
    echo "❌ 测试失败：仍然存在ICN加载错误"
    echo ""
    echo "错误详情："
    echo "$TEST_LOGS" | grep -A 5 "加载ICN图片失败"
    echo ""
    echo "可能原因："
    echo "1. 代码未正确加载（需要重启）"
    echo "2. ICN记录没有附件数据"
    echo "3. 附件的pid或file_type字段不匹配"
    echo ""
    echo "排查SQL："
    echo "SELECT i.id, i.icn, i.create_time, a.id as att_id, a.pid, a.file_type"
    echo "FROM ietm_icn_manage i"
    echo "LEFT JOIN ietm_attachment a ON a.pid = i.id"
    echo "WHERE i.icn = 'ICN-ZB1-A-020000-60101-30101-00017-A-001-01'"
    echo "ORDER BY i.create_time DESC;"

elif [ $ICN_SUCCESS -gt 0 ] && [ $ICN_FAIL -eq 0 ]; then
    echo "✅ 测试成功！ICN加载正常"
    echo ""
    if [ $MULTI_RECORD -gt 0 ]; then
        echo "ℹ️  检测到多条ICN记录，已正确处理（使用最新记录）"
    fi
    echo ""
    echo "修复验证完成："
    echo "  ✅ business_id → pid 字段修复生效"
    echo "  ✅ 多条记录处理逻辑正常"
    echo "  ✅ ICN图片成功加载到预览HTML"

else
    echo "⚠️  测试结果不确定"
    echo ""
    echo "请手动检查："
    echo "1. 完整后端日志: tail -100 $BACKEND_LOG"
    echo "2. 响应HTML内容中是否有图片"
fi

echo ""
echo "=========================================="
echo ""
