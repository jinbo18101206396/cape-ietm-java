#!/bin/bash
BASE_URL="http://localhost:9999/jeecg-boot"
BACKEND_LOG="/c/Users/86135/AppData/Local/Temp/claude/C--Users-86135/d0a81bb7-42d8-4fb1-891c-4962236a4384/tasks/b9osg97wr.output"

echo "=========================================="
echo "  IETM DM预览测试 (POST方法)"
echo "=========================================="
echo ""

# 登录
echo "步骤1: 登录..."
TOKEN=$(curl -s -X POST "$BASE_URL/sys/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "❌ 登录失败"
    exit 1
fi
echo "✅ 登录成功"
echo ""

# 标记测试开始
echo "步骤2: 标记日志..."
echo "--- TEST $(date '+%H:%M:%S') ---" >> "$BACKEND_LOG"
echo ""

# 调用预览接口（POST方法）
echo "步骤3: 调用预览接口..."
DM_ID="2096558746632130561"

RESPONSE=$(curl -s -X POST "$BASE_URL/ietm/dm-content/preview" \
  -H "X-Access-Token: $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"$DM_ID\"}" \
  -w "\nHTTP:%{http_code}")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP:" | cut -d':' -f2)
echo "HTTP状态码: $HTTP_CODE"

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ 请求失败"
    echo "$RESPONSE" | head -20
    exit 1
fi

echo "✅ 请求成功"
echo ""

# 等待日志写入
sleep 3

# 检查日志
echo "步骤4: 检查后端日志..."
LOGS=$(tail -150 "$BACKEND_LOG" | sed -n '/TEST/,$p')

echo "--- ICN相关日志 ---"
echo "$LOGS" | grep -E "ICN|加载|失败|错误|路径替换" | tail -20

echo ""
echo "--- 统计结果 ---"
SUCCESS=$(echo "$LOGS" | grep -c "ICN路径替换完成")
FAIL=$(echo "$LOGS" | grep -c "ICN加载失败")
ERROR=$(echo "$LOGS" | grep -c "加载ICN图片失败")
MULTI=$(echo "$LOGS" | grep -c "ICN存在多条记录")

echo "ICN替换成功: $SUCCESS"
echo "ICN加载失败: $FAIL"  
echo "ICN加载错误: $ERROR"
echo "多条记录警告: $MULTI"
echo ""

if [ $ERROR -gt 0 ]; then
    echo "❌ 测试失败：存在ICN加载错误"
    echo ""
    echo "$LOGS" | grep -A 3 "加载ICN图片失败"
elif [ $SUCCESS -gt 0 ]; then
    echo "✅ 测试成功！ICN加载正常"
    if [ $MULTI -gt 0 ]; then
        echo "ℹ️  多条记录已正确处理"
    fi
else
    echo "ℹ️  该DM可能不包含ICN图符"
fi

echo ""
echo "=========================================="
