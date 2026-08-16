-- =============================================================================
-- 订正 ietm_data_module.dm_type 脏值，使其与字典 dm_type 的 value 对齐
-- 达梦 DM8 / IETM schema。在 IDEA Database 控制台或达梦客户端执行。
--
-- 背景：旧版前端 InfoCodeSelector 映射表把中文 dmtypename 错误映射成
--   descriptive/procedural/fault（字典无此键），或直接把中文原串写入 dm_type，
--   导致 @Dict 解析 dmType_dictText 为空 →「DM类型」列不显示。
--   前端映射已修复（新建/复制不再产生脏值），本脚本订正存量数据。
--
-- 字典 dm_type 规范 value：description / procedure / faultIsolation / crew / maintPlanning / process
--
-- 用法：先跑【1 核对】看清现状 → 跑【2 订正】→ 跑【3 复核】全部“已命中”后 COMMIT。
-- =============================================================================

-- ---- 1 订正前核对：列出当前所有 dm_type 取值及行数 ----
SELECT dm_type, COUNT(*) AS cnt FROM ietm_data_module GROUP BY dm_type ORDER BY cnt DESC;

-- ---- 2 订正（精确匹配旧脏值，不触碰已规范的行）----
-- 2.1 英文错误码 → 规范码
UPDATE ietm_data_module SET dm_type = 'description'    WHERE dm_type = 'descriptive';
UPDATE ietm_data_module SET dm_type = 'procedure'      WHERE dm_type = 'procedural';
UPDATE ietm_data_module SET dm_type = 'faultIsolation' WHERE dm_type IN ('fault', 'faultisolation');
-- 2.2 曾被直接写入的中文原串 → 规范码（复制弹框旧逻辑遗留）
UPDATE ietm_data_module SET dm_type = 'description'    WHERE dm_type = '描述类';
UPDATE ietm_data_module SET dm_type = 'procedure'      WHERE dm_type = '程序类';
UPDATE ietm_data_module SET dm_type = 'process'        WHERE dm_type IN ('过程类', '工艺类');
UPDATE ietm_data_module SET dm_type = 'faultIsolation' WHERE dm_type IN ('故障类', '故障隔离类');
UPDATE ietm_data_module SET dm_type = 'crew'           WHERE dm_type IN ('乘员类', '操作类');
UPDATE ietm_data_module SET dm_type = 'maintPlanning'  WHERE dm_type IN ('规划类', '维修计划类');
-- 注：'产品交叉引用表类''容器类' 等字典无对应值的，此处不动，需人工确认后单独处理。

-- ---- 3 复核：全部应能在字典 sys_dict_item(dm_type) 命中 ----
SELECT d.dm_type, COUNT(*) AS cnt,
       CASE WHEN i.item_value IS NULL THEN '未命中【需人工确认】' ELSE '已命中' END AS matched
FROM ietm_data_module d
LEFT JOIN sys_dict s ON s.dict_code = 'dm_type'
LEFT JOIN sys_dict_item i ON i.dict_id = s.id AND i.item_value = d.dm_type
WHERE d.dm_type IS NOT NULL AND d.dm_type <> ''
GROUP BY d.dm_type, CASE WHEN i.item_value IS NULL THEN '未命中【需人工确认】' ELSE '已命中' END
ORDER BY matched, cnt DESC;

-- 复核结果若仍有“未命中”，说明存在字典未覆盖的类型，请贴出来我再判断。
-- 全部“已命中”后：
COMMIT;
