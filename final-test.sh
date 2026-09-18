#!/bin/bash
# 测试修复后的ICN加载功能
# 策略：直接查询包含graphic标签的DM，然后预览

BASE_URL="http://localhost:9999/jeecg-boot"
BACKEND_LOG="/c/Users/86135/AppData/Local/Temp/claude/C--Users-86135/d0a81bb7-42d8-4fb1-891c-4962236a4384/tasks/b9osg97wr.output"

echo "=========================================="
echo "  IETM ICN加载修复验证测试"
echo "=========================================="
echo ""

# 登录
echo "步骤1: 登录系统..."
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/sys/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}')

TOKEN=$(echo "$LOGIN_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "❌ 登录失败"
    exit 1
fi

echo "✅ 登录成功: ${TOKEN:0:30}..."
echo ""

# 查询包含graphic的DM
echo "步骤2: 查询包含ICN图符的DM..."
QUERY_RESP=$(curl -s -X GET "$BASE_URL/ietm/dm-content/list?pageNo=1&pageSize=100" \
  -H "X-Access-Token: $TOKEN" \
  -H "Content-Type: application/json")

# 从响应中提取包含graphic的DM ID（简化处理）
# 实际上我们使用之前日志中出现的DM
DM_IDS=("2096558746632130561")  # 这是之前测试失败时的DM ID

echo "找到测试DM数量: ${#DM_IDS[@]}"
echo ""

# 测试每个DM的预览
for DM_ID in "${DM_IDS[@]}"; do
    echo "=========================================="
    echo "测试 DM ID: $DM_ID"
    echo "=========================================="

    # 标记日志
    echo "--- TEST DM:$DM_ID $(date '+%H:%M:%S') ---" >> "$BACKEND_LOG"

    # 调用预览接口
    echo "调用预览接口..."
    PREVIEW_RESP=$(curl -s -X POST "$BASE_URL/ietm/dm-content/preview" \
      -H "X-Access-Token: $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"id\":\"$DM_ID\"}" \
      -w "\nHTTP:%{http_code}")

    HTTP_CODE=$(echo "$PREVIEW_RESP" | grep "HTTP:" | cut -d':' -f2)

    if [ "$HTTP_CODE" != "200" ]; then
        echo "❌ 预览失败 (HTTP $HTTP_CODE)"
        continue
    fi

    echo "✅ 预览成功 (HTTP $HTTP_CODE)"

    # 等待日志写入
    sleep 2

    # 分析响应
    RESPONSE_BODY=$(echo "$PREVIEW_RESP" | grep -v "HTTP:")

    # 检查响应中是否有ICN相关内容
    if echo "$RESPONSE_BODY" | grep -q "graphic"; then
        echo "ℹ️  响应中包含graphic标签"
        HAS_GRAPHIC=1
    else
        echo "ℹ️  响应中不包含graphic标签"
        HAS_GRAPHIC=0
    fi

    if echo "$RESPONSE_BODY" | grep -q "ICN加载失败"; then
        echo "❌ 响应中包含'ICN加载失败'文字"
        HAS_ERROR_TEXT=1
    else
        HAS_ERROR_TEXT=0
    fi

    if echo "$RESPONSE_BODY" | grep -q "data:image"; then
        echo "✅ 响应中包含base64图片数据"
        HAS_IMAGE=1
    else
        HAS_IMAGE=0
    fi

    # 检查后端日志
    echo ""
    echo "检查后端日志..."
    LOGS=$(tail -200 "$BACKEND_LOG" | sed -n "/TEST DM:$DM_ID/,\$p")

    echo "--- 相关日志 ---"
    echo "$LOGS" | grep -E "ICN|加载|失败|ERROR|路径替换" | head -20

    # 统计
    echo ""
    echo "--- 统计结果 ---"
    ICN_FOUND=$(echo "$LOGS" | grep -c "找到ICN记录")
    ICN_SUCCESS=$(echo "$LOGS" | grep -c "ICN路径替换完成")
    ICN_FAIL=$(echo "$LOGS" | grep -c "ICN加载失败")
    ICN_ERROR=$(echo "$LOGS" | grep -c "加载ICN图片失败")
    MULTI_RECORD=$(echo "$LOGS" | grep -c "ICN存在多条记录")
    BUSINESS_ID_ERROR=$(echo "$LOGS" | grep -c "无效的列名\[business_id\]")

    echo "  - ICN记录查找: $ICN_FOUND"
    echo "  - ICN替换成功: $ICN_SUCCESS"
    echo "  - ICN加载失败: $ICN_FAIL"
    echo "  - ICN加载错误: $ICN_ERROR"
    echo "  - 多条记录处理: $MULTI_RECORD"
    echo "  - business_id错误: $BUSINESS_ID_ERROR"
    echo ""

    # 判断结果
    if [ $BUSINESS_ID_ERROR -gt 0 ]; then
        echo "❌❌❌ 严重：business_id错误仍存在！修复未生效！"
    elif [ $ICN_ERROR -gt 0 ]; then
        echo "❌ 测试失败：ICN加载出错"
        echo ""
        echo "详细错误："
        echo "$LOGS" | grep -A 5 "加载ICN图片失败"
    elif [ $ICN_SUCCESS -gt 0 ] && [ $ICN_FAIL -eq 0 ]; then
        echo "✅✅✅ 测试成功！ICN加载正常！"
        if [ $MULTI_RECORD -gt 0 ]; then
            echo "✅ 多条记录问题已正确处理"
        fi
        echo ""
        echo "修复验证："
        echo "  ✅ business_id → pid 修复生效"
        echo "  ✅ 多条记录使用list()而非selectOne()"
        echo "  ✅ ICN图片成功加载"
    elif [ $HAS_GRAPHIC -eq 0 ]; then
        echo "ℹ️  该DM不包含ICN图符（无graphic标签）"
    else
        echo "⚠️  结果不明确，请手动检查"
    fi

    echo ""
done

echo "=========================================="
echo "  测试完成"
echo "=========================================="
echo ""
echo "如需查看完整日志："
echo "tail -200 $BACKEND_LOG | grep -E 'ICN|加载|失败'"
echo ""
