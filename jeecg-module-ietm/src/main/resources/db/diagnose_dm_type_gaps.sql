-- =============================================================================
-- dm_type 全类一致性诊断（只读，不改数据）。数据库：DM8 / IETM schema。
-- 目的：系统性发现"字典 / 类型表 / 存量数据"三方之间的缺口，覆盖以下问题类：
--   G1 字典有、类型表无 → UI 可选但 resolveDmType 匹配不中，静默回退 descript.xsd
--   G2 类型表有、字典无 → 存量/导入数据能解析模板，但 UI 下拉选不到，无法新建
--   G3 某标准类型集不全 → 同一 type_code 在标准 A 有、标准 B 无
--   G4 存量脏值 → ietm_data_module.dm_type 既不在字典也不在类型表
-- 用法：逐段执行，看输出判断。全部返回 0 行 = 无缺口。
-- =============================================================================

-- ---- G1 字典有、类型表无（按标准展开）----
-- 对每个"标准 × 字典项"，检查类型表是否存在对应 type_code。缺失 = UI 能选但建 DM 回退默认。
SELECT s.ietm_standard, di.item_value AS dict_code, di.item_text
FROM (SELECT DISTINCT ietm_standard FROM ietm_dm_type) s
CROSS JOIN sys_dict_item di
JOIN sys_dict d ON d.id = di.dict_id AND d.dict_code = 'dm_type'
WHERE NOT EXISTS (
  SELECT 1 FROM ietm_dm_type t
  WHERE t.type_code = di.item_value AND t.ietm_standard = s.ietm_standard AND t.status = '1'
)
ORDER BY s.ietm_standard, di.sort_order;

-- ---- G2 类型表有、字典无 ----
-- 类型表里存在、但字典未收录的 type_code = UI 下拉选不到，无法从界面新建此类 DM。
SELECT DISTINCT t.type_code, t.type_name, t.ietm_standard
FROM ietm_dm_type t
WHERE t.status = '1'
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item di
    JOIN sys_dict d ON d.id = di.dict_id AND d.dict_code = 'dm_type'
    WHERE di.item_value = t.type_code
  )
ORDER BY t.ietm_standard, t.type_code;

-- ---- G3 各标准类型集差异（以 type_code 全集为基准，列出每个标准缺哪些）----
SELECT allc.type_code, s.ietm_standard AS missing_in_standard
FROM (SELECT DISTINCT type_code FROM ietm_dm_type WHERE status = '1') allc
CROSS JOIN (SELECT DISTINCT ietm_standard FROM ietm_dm_type) s
WHERE NOT EXISTS (
  SELECT 1 FROM ietm_dm_type t
  WHERE t.type_code = allc.type_code AND t.ietm_standard = s.ietm_standard AND t.status = '1'
)
ORDER BY allc.type_code, s.ietm_standard;

-- ---- G4 存量脏值：dm_type 既不在字典也不在类型表 ----
SELECT dm.dm_type, COUNT(*) AS cnt
FROM ietm_data_module dm
WHERE dm.dm_type IS NOT NULL AND dm.dm_type <> ''
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item di
    JOIN sys_dict d ON d.id = di.dict_id AND d.dict_code = 'dm_type'
    WHERE di.item_value = dm.dm_type)
  AND NOT EXISTS (
    SELECT 1 FROM ietm_dm_type t WHERE t.type_code = dm.dm_type AND t.status = '1')
GROUP BY dm.dm_type
ORDER BY cnt DESC;

-- =============================================================================
-- 已知结论（截至排查时，供对照；实际以上面查询输出为准）：
--   G1: dm_type 字典含 'frontmatter'(前言类)，但类型表任何标准都无此行
--       → 新建"前言类"DM 会回退 descript.xsd。需在类型表补 frontmatter 行
--         (S1000D41/S1000D42 有 frontmatter.xsd/xml；S1000D40 无，需确认是否支持)。
--   G2: 类型表含 'faultReporting'(故障报告类) / 'illustratedPartsCatalog'(图解零件目录类)，
--       但字典未收录 → UI 选不到。若业务需要，补 sys_dict_item 两项。
-- 处置建议：先跑本脚本确认现状，再决定"补类型表行"还是"删字典项"，避免臆断。
-- =============================================================================
