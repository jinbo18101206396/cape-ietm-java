-- ============================================================
-- DMC 与版本号不一致问题 - 诊断与修复 SQL
-- ============================================================
-- 问题描述：
--   历史版本页面显示的 DMC 中版本号（如 _001-01_）与数据库字段版本号（如 001-02）不一致
--
-- 根本原因：
--   editProp 方法在自动签出升级版本号时，更新了 issue_no/in_work 字段，
--   但未同步更新 dmc_code 字段，导致 DMC 仍包含旧版本号
--
-- 影响范围：
--   所有通过"编辑DM属性"触发自动签出的记录
--
-- 修复方案：
--   1. 后端代码已修复（editProp 方法现在会重新生成 DMC）
--   2. 对于历史遗留数据，可使用以下 SQL 或后端接口 POST /ietm/datamodule/fixDmc
-- ============================================================

-- 步骤1：诊断 - 查找不一致的记录
SELECT
    id,
    dmc_code,
    issue_no,
    in_work,
    -- 从 DMC 中提取版本号（假设格式为 DMC-..._issueNo-inWork_lang-country）
    SUBSTR(dmc_code, INSTR(dmc_code, '_', 1, 1) + 1, 6) AS dmc_version_part,
    -- 数据库字段组合的版本号
    issue_no || '-' || in_work AS field_version,
    -- 判断是否一致
    CASE
        WHEN SUBSTR(dmc_code, INSTR(dmc_code, '_', 1, 1) + 1, 6) = issue_no || '-' || in_work
        THEN '一致'
        ELSE '不一致'
    END AS consistency_status,
    tech_name,
    info_name,
    checkout_user,
    update_time
FROM ietm_data_module
WHERE status = '1'  -- 仅查有效记录
  AND dmc_code IS NOT NULL
  AND issue_no IS NOT NULL
  AND in_work IS NOT NULL
  -- 找出不一致的记录
  AND SUBSTR(dmc_code, INSTR(dmc_code, '_', 1, 1) + 1, 6) != issue_no || '-' || in_work
ORDER BY update_time DESC;

-- 步骤2：统计不一致记录数量
SELECT
    COUNT(*) AS total_inconsistent,
    COUNT(DISTINCT project_id) AS affected_projects,
    MIN(update_time) AS earliest_occurrence,
    MAX(update_time) AS latest_occurrence
FROM ietm_data_module
WHERE status = '1'
  AND dmc_code IS NOT NULL
  AND issue_no IS NOT NULL
  AND in_work IS NOT NULL
  AND SUBSTR(dmc_code, INSTR(dmc_code, '_', 1, 1) + 1, 6) != issue_no || '-' || in_work;

-- 步骤3：手动修复示例（单条记录）
-- 注意：此 SQL 仅为示例，实际建议使用后端接口 POST /ietm/datamodule/fixDmc
-- 因为后端方法会调用 generateDmc() 确保与业务逻辑完全一致

/*
-- 示例：修复单条记录（需要替换实际的 ID）
UPDATE ietm_data_module
SET dmc_code = (
    -- 重新拼接 DMC（需确保格式与 generateDmc() 方法一致）
    'DMC-' || sns || '-' || info_code ||
    COALESCE(info_code_variant, '') || '-' ||
    COALESCE(ietm_location_code, 'A') ||
    '_' || issue_no || '-' || in_work ||
    '_' || COALESCE(language_iso_code, 'zh') || '-' ||
    COALESCE(country_iso_code, 'CN')
),
update_time = SYSDATE,
update_by = 'system_fix'
WHERE id = 'YOUR_DM_ID_HERE'
  AND status = '1';
*/

-- 步骤4：【推荐】使用后端接口批量修复
-- POST http://localhost:9999/jeecg-boot/ietm/datamodule/fixDmc?limit=1000
-- 需要管理员权限（@RequiresRoles("admin")）
-- 接口会自动调用 generateDmc() 方法确保 DMC 格式完全正确

-- 步骤5：验证修复结果
SELECT
    COUNT(*) AS remaining_inconsistent
FROM ietm_data_module
WHERE status = '1'
  AND dmc_code IS NOT NULL
  AND issue_no IS NOT NULL
  AND in_work IS NOT NULL
  AND SUBSTR(dmc_code, INSTR(dmc_code, '_', 1, 1) + 1, 6) != issue_no || '-' || in_work;

-- 预期结果：remaining_inconsistent = 0

-- ============================================================
-- 附录：理解 DMC 格式
-- ============================================================
-- 完整 DMC 格式（S1000D 标准）：
--   DMC-{sns}-{infoCode}{infoCodeVariant}-{itemLocationCode}_{issueNo}-{inWork}_{lang}-{country}
--
-- 示例：
--   DMC-ZB1-A-02-00-00-00A-212A-A_001-02_zh-CN
--   ├── DMC-                                      前缀
--   ├── ZB1-A-02-00-00-00A                       SNS（系统编号）
--   ├── -212A-                                   信息码+变体
--   ├── A                                        位置码
--   ├── _001-02_                                 版本号（发行号-在编版本）
--   └── zh-CN                                    语言-国家
--
-- 版本号字段：
--   - issue_no：发行编号（001-999），每次正式发布时 +1
--   - in_work：在编版本（00-99），每次签出/编辑时 +1
--
-- 不一致示例：
--   DMC: DMC-..._001-01_zh-CN  （旧版本）
--   Fields: issue_no='001', in_work='02'  （新版本）
--   → DMC 应为：DMC-..._001-02_zh-CN
-- ============================================================
