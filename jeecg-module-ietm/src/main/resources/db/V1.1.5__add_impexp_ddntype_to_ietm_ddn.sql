-- 为ietm_ddn表添加impexp和ddn_type字段
-- 版本: V1.1.5
-- 日期: 2026-09-15
-- 说明: 新增导入导出标识和DDN类型字段

-- 1. 添加导入导出标识字段
ALTER TABLE ietm_ddn ADD COLUMN impexp VARCHAR(1);
COMMENT ON COLUMN ietm_ddn.impexp IS '导入导出标识：e=导出,i=导入';

-- 2. 添加DDN类型字段
ALTER TABLE ietm_ddn ADD COLUMN ddn_type VARCHAR(10);
COMMENT ON COLUMN ietm_ddn.ddn_type IS 'DDN类型：DM/ICN/PM';

-- 3. 创建索引优化查询性能
CREATE INDEX idx_ietm_ddn_project_type ON ietm_ddn(project_id, ddn_type, create_time DESC);

-- 4. 更新现有数据 - 所有现有记录标记为导出
UPDATE ietm_ddn SET impexp = 'e' WHERE impexp IS NULL;

-- 5. 更新现有数据 - 根据dm_ids判断类型（对标旧系统逻辑）
--    旧系统注释："通过dm_ids为空来区分ICN导出"
UPDATE ietm_ddn
SET ddn_type = CASE
    WHEN dm_ids IS NULL OR dm_ids = '' THEN 'ICN'
    ELSE 'DM'
END
WHERE ddn_type IS NULL;
