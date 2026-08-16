-- =============================================================================
-- 为字典 dm_type 补齐 faultReporting / illustratedPartsCatalog 两项
-- 数据库：DM8 / IETM schema
-- 背景：这两个 type_code 在 ietm_dm_type(GJB6600) 已存在，但 sys_dict_item 未收录，
--       导致新建/复制弹框的 DM类型 下拉选不到 → 无法从界面新建这两类 DM。
-- item_value 必须精确等于类型表 type_code（区分大小写），否则 @Dict 解析不到 _dictText。
-- 幂等：WHERE NOT EXISTS 保护，可重复执行。
-- 用法：先跑【核对】→ 跑【插入】→ 跑【复核】→ COMMIT。
-- =============================================================================

-- ---- 1 核对：当前 dm_type 字典项（应为 7 项：6 种子 + live 追加的 frontmatter）----
SELECT di.item_value, di.item_text, di.sort_order
FROM sys_dict_item di
JOIN sys_dict d ON d.id = di.dict_id AND d.dict_code = 'dm_type'
ORDER BY di.sort_order;

-- ---- 2 插入（仅当不存在时）----
-- 2.1 故障报告类
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time)
SELECT '1750000001007', '1750000000010', '故障报告DM', 'faultReporting', '故障报告数据模块', 7, 1, SYSDATE
FROM dual
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_item WHERE dict_id = '1750000000010' AND item_value = 'faultReporting'
);

-- 2.2 图解零件目录类
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time)
SELECT '1750000001008', '1750000000010', '图解零件目录DM', 'illustratedPartsCatalog', '图解零件目录数据模块', 8, 1, SYSDATE
FROM dual
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_item WHERE dict_id = '1750000000010' AND item_value = 'illustratedPartsCatalog'
);

-- ---- 3 复核：应出现 faultReporting / illustratedPartsCatalog 两项 ----
SELECT di.item_value, di.item_text, di.sort_order
FROM sys_dict_item di
JOIN sys_dict d ON d.id = di.dict_id AND d.dict_code = 'dm_type'
ORDER BY di.sort_order;

-- 注：字典缓存来自登录响应 sysAllDictItems，插入后需重新登录（或清缓存）前端下拉才刷新。
-- 确认两项就位后：
COMMIT;

-- =============================================================================
-- 回滚（仅 COMMIT 之前有效）
-- DELETE FROM sys_dict_item WHERE dict_id = '1750000000010'
--   AND item_value IN ('faultReporting', 'illustratedPartsCatalog');
-- COMMIT;
-- =============================================================================
