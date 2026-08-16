-- =============================================================================
-- 恢复 ietm_dm_type.type_code 为原始设计值 maintPlanning
-- 背景：此前一次错误订正把 maintPlanning→planning，与原始种子SQL(gjb6600_seed.sql)
--       及 S1000D4.0 的 content_root_elem='maintPlanning' 矛盾。
--       字典/前端同步改为 maintPlanning，三方统一。
-- 影响：ietm_dm_type 共 4 行（GJB6600/S1000D4.0/S1000D4.1/S1000D4.2 各1行）
--       sys_dict_item 共 1 行（dm_type 字典的"规划性DM"项）
-- 数据库：DM8 / IETM schema
-- 用法：先跑【核对】→ 跑【订正】→ 跑【复核】→ COMMIT。
-- =============================================================================

-- ---- 1 核对：当前应全为 planning（4 行）----
SELECT type_code, ietm_standard, content_root_elem FROM ietm_dm_type
WHERE type_code IN ('planning', 'maintPlanning')
ORDER BY ietm_standard;

-- ---- 2 订正 ----
-- 2.1 类型表：planning → maintPlanning（4 行：GJB6600/S1000D4.0/S1000D4.1/S1000D4.2）
UPDATE ietm_dm_type SET type_code = 'maintPlanning' WHERE type_code = 'planning';

-- 2.2 字典：sys_dict_item 中 dm_type 的"规划性DM"项 value 改为 maintPlanning
UPDATE sys_dict_item SET item_value = 'maintPlanning'
WHERE id = '1750000001005' AND item_value = 'planning';

-- 2.3 ietm_data_module：若 fix_dm_type_align_v2.sql 曾把"规划类/维修计划类"写成 'planning'，
--     此处一并订正，使其能再次命中 resolveDmType 的 type_code='maintPlanning' 条件。
UPDATE ietm_data_module SET dm_type = 'maintPlanning' WHERE dm_type = 'planning';

-- ---- 3 复核 ----
-- 3.1 类型表应显示4行 maintPlanning
SELECT type_code, ietm_standard, xsd_file, content_root_elem FROM ietm_dm_type
WHERE type_code = 'maintPlanning' ORDER BY ietm_standard;

-- 3.2 字典应显示 item_value = maintPlanning
SELECT item_text, item_value FROM sys_dict_item WHERE id = '1750000001005';

-- 3.3 ietm_data_module：'planning' 应返回 0 行
SELECT COUNT(*) AS remaining_planning FROM ietm_data_module WHERE dm_type = 'planning';

-- 确认无误后：
COMMIT;

-- =============================================================================
-- 回滚（仅 COMMIT 之前有效）
-- UPDATE ietm_dm_type SET type_code = 'planning' WHERE type_code = 'maintPlanning';
-- UPDATE sys_dict_item SET item_value = 'planning' WHERE id = '1750000001005';
-- COMMIT;
-- =============================================================================
