#!/bin/bash
# IETM DM预览ICN加载测试脚本
# 用途：模拟前端调用DM预览接口，验证ICN图片是否正常加载

echo "=========================================="
echo "  IETM DM预览ICN加载测试"
echo "=========================================="
echo ""

# 配置
BASE_URL="http://localhost:9999/jeecg-boot"
BACKEND_LOG="/c/Users/86135/AppData/Local/Temp/claude/C--Users-86135/d0a81bb7-42d8-4fb1-891c-4962236a4384/tasks/b9osg97wr.output"

echo "1. 检查后端服务状态..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/")
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ] || [ "$HTTP_CODE" = "404" ]; then
    echo "   ✅ 后端服务运行正常 (HTTP $HTTP_CODE)"
else
    echo "   ❌ 后端服务异常 (HTTP $HTTP_CODE)"
    exit 1
fi
echo ""

echo "2. 检查新代码是否已加载..."
RESTART_TIME=$(grep "Application Jeecg-Boot is running" "$BACKEND_LOG" | tail -1 | awk '{print $1, $2}')
echo "   最后启动时间: $RESTART_TIME"
if echo "$RESTART_TIME" | grep -q "15:21"; then
    echo "   ✅ 服务已重启，新代码已加载"
else
    echo "   ⚠️  服务可能使用旧代码"
fi
echo ""

echo "3. 监控后端日志（实时）..."
echo "   请在另一个终端运行以下命令查看实时日志："
echo "   tail -f $BACKEND_LOG | grep -E 'ICN|加载|失败|错误'"
echo ""

echo "=========================================="
echo "  手动测试步骤"
echo "=========================================="
echo ""
echo "请在浏览器中完成以下操作："
echo ""
echo "1️⃣  打开前端: http://localhost:3000"
echo ""
echo "2️⃣  登录系统"
echo ""
echo "3️⃣  进入【数据模块管理】页面"
echo ""
echo "4️⃣  找到测试DM（根据之前的日志，ICN编码为）:"
echo "    - ICN-ZB1-A-020000-60101-30101-00016-A-001-01"
echo "    - ICN-ZB1-A-020000-60101-30101-00017-A-001-01"
echo ""
echo "5️⃣  点击【预览】按钮"
echo ""
echo "6️⃣  观察结果："
echo ""
echo "✅ 成功标志："
echo "   - ICN图片正常显示（不是占位图或\"ICN加载失败\"）"
echo "   - 后端日志显示:"
echo "     🔍 ICN存在多条记录，使用最新的: icnCode=xxx, 记录数=3"
echo "     🔍 找到ICN记录: icnCode=xxx, icnId=xxx"
echo "     🔍 找到ICN附件: icnCode=xxx, 附件数量=1"
echo "     ICN路径替换完成: 总数=X, 成功=X, 失败=0"
echo ""
echo "❌ 失败标志："
echo "   - 图片显示\"ICN加载失败\"或占位图"
echo "   - 后端日志报错:"
echo "     ERROR - 加载ICN图片失败"
echo "     MybatisPlusException: One record is expected, but the query result is multiple records"
echo ""
echo "=========================================="
echo "  问题排查SQL"
echo "=========================================="
echo ""
echo "如果测试失败，在数据库中执行以下SQL："
echo ""
echo "-- 检查ICN记录数量"
echo "SELECT icn, COUNT(*) as cnt"
echo "FROM ietm_icn_manage"
echo "WHERE icn IN ("
echo "  'ICN-ZB1-A-020000-60101-30101-00016-A-001-01',"
echo "  'ICN-ZB1-A-020000-60101-30101-00017-A-001-01'"
echo ")"
echo "GROUP BY icn;"
echo ""
echo "-- 检查最新ICN的附件"
echo "SELECT i.id, i.icn, i.create_time, a.id as attachment_id, a.file_key, a.file_type"
echo "FROM ietm_icn_manage i"
echo "LEFT JOIN ietm_attachment a ON a.pid = i.id"
echo "WHERE i.icn = 'ICN-ZB1-A-020000-60101-30101-00017-A-001-01'"
echo "ORDER BY i.create_time DESC, a.create_time ASC;"
echo ""
echo "=========================================="
echo ""
echo "准备好后，请进行测试并告诉我结果！"
echo ""
