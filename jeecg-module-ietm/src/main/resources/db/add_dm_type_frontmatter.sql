-- =============================================================================
-- 为 ietm_dm_type 补齐 frontmatter(前言类) 行 —— 仅 S1000D4.1 / S1000D4.2
-- 数据库：DM8 / IETM schema
-- 背景：dm_type 字典(live)含 frontmatter，但类型表任何标准都无对应行 → 新建"前言类"DM
--       时 resolveDmType 匹配不中，静默回退 descript.xsd。
-- 范围：frontmatter.xsd / frontmatter.xml 仅存在于 classpath ietm/S1000D41/、ietm/S1000D42/；
--       S1000D40 无此 xsd，故不补 4.0（前言类在 4.0 下不支持）。
-- content_root_elem：该列 grep 确认无任何业务代码使用（resolveDmType 只用 xsd_file/template_file
--       拼 classpath 路径），此处填中文"前言"对齐 GJB6600 风格，纯文档性质。
-- 幂等：WHERE NOT EXISTS 保护，可重复执行。
-- 用法：先跑【核对】→ 跑【插入】→ 跑【复核】→ COMMIT。
-- =============================================================================

-- ---- 1 核对：当前是否已有 frontmatter 行（预期 0）+ 4.1/4.2 xsd 前置 ----
SELECT ietm_standard, type_code, xsd_file, template_file
FROM ietm_dm_type WHERE type_code = 'frontmatter' ORDER BY ietm_standard;

-- ---- 2 插入 ----
-- 2.1 S1000D4.1
INSERT INTO ietm_dm_type
  (id, type_code, type_name, type_name_en, ietm_standard, xsd_file, template_file, content_root_elem, sort_no, status, create_by, create_time)
SELECT 'S1000D41FM', 'frontmatter', '前言类', 'Front Matter', 'S1000D4.1',
       'frontmatter.xsd', 'frontmatter.xml', '前言', 90, '1', 'admin', SYSDATE
FROM dual
WHERE NOT EXISTS (
  SELECT 1 FROM ietm_dm_type WHERE type_code = 'frontmatter' AND ietm_standard = 'S1000D4.1'
);

-- 2.2 S1000D4.2
INSERT INTO ietm_dm_type
  (id, type_code, type_name, type_name_en, ietm_standard, xsd_file, template_file, content_root_elem, sort_no, status, create_by, create_time)
SELECT 'S1000D42FM', 'frontmatter', '前言类', 'Front Matter', 'S1000D4.2',
       'frontmatter.xsd', 'frontmatter.xml', '前言', 90, '1', 'admin', SYSDATE
FROM dual
WHERE NOT EXISTS (
  SELECT 1 FROM ietm_dm_type WHERE type_code = 'frontmatter' AND ietm_standard = 'S1000D4.2'
);

-- ---- 3 复核：应出现 2 行（4.1 / 4.2）----
SELECT ietm_standard, type_code, type_name, xsd_file, template_file, status
FROM ietm_dm_type WHERE type_code = 'frontmatter' ORDER BY ietm_standard;

-- 确认 2 行就位后：
COMMIT;

-- =============================================================================
-- 回滚（仅 COMMIT 之前有效）
-- DELETE FROM ietm_dm_type WHERE type_code = 'frontmatter'
--   AND ietm_standard IN ('S1000D4.1', 'S1000D4.2');
-- COMMIT;
-- =============================================================================
