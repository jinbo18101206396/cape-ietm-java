-- =============================================================================
-- 订正 ietm_data_module 脏值 dm_type='dm'（数据库：DM8 / IETM schema）
--   问题：1 条历史脏值 dm_type='dm'（非任何合法 type_code），回退默认 descript.xsd。
--         按内容归类订正为 'description'。
-- 注意：此脚本已移除原来的 maintPlanning→planning 订正（已另行恢复为 maintPlanning，
--       见 fix_dm_type_maintplanning_restore.sql）。
-- 用法：先跑【核对】→ 跑【订正】→ 跑【复核】→ COMMIT。
-- =============================================================================

-- ---- 1 核对：应显示 1 行 dm_type='dm' ----
SELECT id, dm_type, project_id FROM ietm_data_module WHERE id = '2084841754673999875';

-- ---- 2 订正：'dm' → 'description' ----
UPDATE ietm_data_module SET dm_type = 'description'
WHERE id = '2084841754673999875' AND dm_type = 'dm';

-- ---- 3 复核：应显示 dm_type='description'，结果='命中' ----
SELECT d.id, d.dm_type, p.ietm_standard,
       CASE WHEN t.type_code IS NULL THEN '未命中' ELSE '命中' END AS 结果
FROM ietm_data_module d
LEFT JOIN ietm_project p ON p.id = d.project_id
LEFT JOIN ietm_dm_type t ON t.type_code = d.dm_type AND t.ietm_standard = p.ietm_standard AND t.status = '1'
WHERE d.id = '2084841754673999875';

COMMIT;

-- =============================================================================
-- 回滚（仅 COMMIT 之前有效）
-- UPDATE ietm_data_module SET dm_type = 'dm' WHERE id = '2084841754673999875' AND dm_type = 'description';
-- COMMIT;
-- =============================================================================
