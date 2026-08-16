-- =============================================================================
-- 订正 ietm_data_module.dm_type，使其与 ietm_dm_type.type_code 对齐
-- 背景：DM 表 dm_type 存 descriptive/procedural/fault，类型表 type_code 存
--       description/procedure/faultIsolation，导致 resolveDmType 匹配不到、
--       编辑器加载模板走默认 descript.xsd。本脚本按语义对齐 DM 侧数据。
-- 策略：B（UPDATE DM 表，共 8 行；类型表保持规范不动）
-- 数据库：DM8（IETM schema）
-- 使用：先执行【1 订正前核对】确认行数无误，再执行【2 订正】，最后【3 订正后核对】。
--       确认无误后 COMMIT；若有异常，用【回滚】段（见文末）恢复。
-- =============================================================================

-- ---- 1 订正前核对：应显示 descriptive=4, procedural=3, fault=1 ----
SELECT dm_type, COUNT(*) AS cnt FROM ietm_data_module GROUP BY dm_type;

-- ---- 2 订正（精确匹配旧值，不触碰其他数据）----
UPDATE ietm_data_module SET dm_type = 'description'    WHERE dm_type = 'descriptive';
UPDATE ietm_data_module SET dm_type = 'procedure'      WHERE dm_type = 'procedural';
UPDATE ietm_data_module SET dm_type = 'faultIsolation' WHERE dm_type = 'fault';

-- ---- 3 订正后核对：dm_type 应全部能在 ietm_dm_type.type_code 命中 ----
SELECT d.dm_type, COUNT(*) AS cnt,
       CASE WHEN t.type_code IS NULL THEN '未命中' ELSE '已命中' END AS matched
FROM ietm_data_module d
LEFT JOIN ietm_dm_type t ON t.type_code = d.dm_type AND t.ietm_standard = 'S1000D4.0'
GROUP BY d.dm_type, CASE WHEN t.type_code IS NULL THEN '未命中' ELSE '已命中' END;

-- 全部 matched=已命中 且行数与订正前一致（4/3/1）后：
COMMIT;

-- =============================================================================
-- 回滚（仅在 COMMIT 之前有效；若已 COMMIT，执行下面反向 UPDATE 后再 COMMIT）
-- UPDATE ietm_data_module SET dm_type = 'descriptive' WHERE dm_type = 'description';
-- UPDATE ietm_data_module SET dm_type = 'procedural'  WHERE dm_type = 'procedure';
-- UPDATE ietm_data_module SET dm_type = 'fault'       WHERE dm_type = 'faultIsolation';
-- COMMIT;
-- =============================================================================
