-- 更新现有DDN记录的 impexp 和 ddn_type 字段
-- 执行日期：2026-09-15
-- 说明：为历史数据补充导入导出标识和DDN类型

-- 1. 更新所有现有记录的 impexp 为导出（默认都是导出产生的）
UPDATE ietm_ddn
SET impexp = 'e'
WHERE impexp IS NULL;

-- 2. 根据 dm_ids 字段判断DDN类型
--    - dm_ids 为空或NULL：ICN导出
--    - dm_ids 不为空：DM导出
--    注意：旧系统注释提到"通过dm_ids为空来区分ICN导出"
UPDATE ietm_ddn
SET ddn_type = CASE
    WHEN dm_ids IS NULL OR dm_ids = '' THEN 'ICN'
    ELSE 'DM'
END
WHERE ddn_type IS NULL;

-- 3. 验证更新结果
SELECT
    ddn_type,
    impexp,
    COUNT(*) as count,
    SUM(CASE WHEN dm_ids IS NULL OR dm_ids = '' THEN 1 ELSE 0 END) as empty_dm_ids_count,
    SUM(CASE WHEN dm_ids IS NOT NULL AND dm_ids != '' THEN 1 ELSE 0 END) as has_dm_ids_count
FROM ietm_ddn
WHERE status = '1'
GROUP BY ddn_type, impexp
ORDER BY ddn_type, impexp;

-- 4. 查看示例数据
SELECT
    id,
    ddn_code,
    ddn_type,
    impexp,
    CASE WHEN LENGTH(dm_ids) > 50 THEN SUBSTR(dm_ids, 1, 50) || '...' ELSE dm_ids END as dm_ids_preview,
    dm_count,
    icn_count,
    create_time
FROM ietm_ddn
WHERE status = '1'
ORDER BY create_time DESC
LIMIT 10;
