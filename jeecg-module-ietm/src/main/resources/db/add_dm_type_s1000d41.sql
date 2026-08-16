-- =============================================================================
-- 为 ietm_dm_type 补齐 S1000D4.1 整套类型（数据库：DM8 / IETM schema）
-- 背景：类型表原本只有 GJB6600 与 S1000D4.0 两套，缺 S1000D4.1。项目"123"配
--       的是 S1000D4.1，其下一旦建 DM，resolveDmType 的 ietm_standard 等值条件
--       匹配不中，无论什么类型都回退默认 descript.xsd。
-- 前置确认：S1000D4.1 的 7 类 XSD + 模板文件在 classpath ietm/S1000D41/ 下全部存在。
-- 策略：INSERT...SELECT 复制 S1000D4.0 的 8 行，仅改 ietm_standard，其余字段原样照搬，
--       保证与 4.0 完全一致（type_name / content_root_elem / sort_no 不手工臆造）。
-- 说明：type_code 原样复制（maintPlanning 保持 maintPlanning，与方案A一致），
--       可独立执行。
-- 用法：先跑【核对】→ 跑【插入】→ 跑【复核】确认 8 行后 COMMIT。
-- =============================================================================

-- ---- 1 插入前核对 ----
-- 1.1 确认 S1000D4.1 目前无数据（应返回 0）
SELECT COUNT(*) AS s41_cnt FROM ietm_dm_type WHERE ietm_standard = 'S1000D4.1';
-- 1.2 确认 S1000D4.0 源数据存在（应返回 8）
SELECT COUNT(*) AS s40_cnt FROM ietm_dm_type WHERE ietm_standard = 'S1000D4.0';

-- ---- 2 插入：复制 S1000D4.0 → S1000D4.1 ----
--   id 用可读前缀 + ROWNUM 生成，保证唯一且不与雪花ID冲突；
--   type_code 原样照搬（maintPlanning 保持 maintPlanning，与方案A一致）。
INSERT INTO ietm_dm_type
  (id, type_code, type_name, type_name_en, ietm_standard, xsd_file, template_file, content_root_elem, sort_no, status, create_by, create_time)
SELECT
  'S1000D41' || LPAD(TO_CHAR(ROWNUM), 3, '0'),
  type_code,
  type_name,
  type_name_en,
  'S1000D4.1',
  xsd_file,
  template_file,
  content_root_elem,
  sort_no,
  '1',
  'admin',
  SYSDATE
FROM ietm_dm_type
WHERE ietm_standard = 'S1000D4.0';

-- ---- 3 复核：S1000D4.1 应有 8 行，type_code 与 4.0 一致（maintPlanning 已保持）----
SELECT type_code, type_name, ietm_standard, xsd_file, template_file, status
FROM ietm_dm_type
WHERE ietm_standard = 'S1000D4.1'
ORDER BY sort_no;

-- 确认 8 行后：
COMMIT;

-- =============================================================================
-- 回滚（仅 COMMIT 之前有效）
-- DELETE FROM ietm_dm_type WHERE ietm_standard = 'S1000D4.1';
-- COMMIT;
-- =============================================================================
