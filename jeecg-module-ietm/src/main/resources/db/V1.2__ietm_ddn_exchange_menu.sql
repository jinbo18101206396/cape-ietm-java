-- 数据交换记录菜单配置
-- 注意：需要根据实际的父菜单ID调整parent_id值

-- 1. 添加impexp和ddn_type字段（如果之前未执行）
-- ALTER TABLE ietm_ddn ADD COLUMN impexp VARCHAR(1);
-- ALTER TABLE ietm_ddn ADD COLUMN ddn_type VARCHAR(10);

-- 2. 添加字段注释
-- COMMENT ON COLUMN ietm_ddn.impexp IS '导入导出标识：e=导出,i=导入';
-- COMMENT ON COLUMN ietm_ddn.ddn_type IS 'DDN类型：DM/ICN/PM';

-- 3. 创建索引
-- CREATE INDEX idx_ietm_ddn_project_type ON ietm_ddn(project_id, ddn_type, create_time DESC);

-- 4. 更新存量数据
-- UPDATE ietm_ddn SET impexp = 'e' WHERE impexp IS NULL;
-- UPDATE ietm_ddn SET ddn_type = CASE
--     WHEN ddn_code LIKE '%DDN-DM-%' OR ddn_code LIKE '%DM-%' THEN 'DM'
--     WHEN ddn_code LIKE '%DDN-ICN-%' OR ddn_code LIKE '%ICN-%' THEN 'ICN'
--     WHEN ddn_code LIKE '%DDN-PM-%' OR ddn_code LIKE '%PM-%' THEN 'PM'
--     ELSE 'DM'
-- END WHERE ddn_type IS NULL;

-- 5. 插入菜单权限（注意：需要手动在系统后台配置菜单，或者根据实际sys_permission表结构插入）
-- 数据交换记录菜单配置示例（需要根据实际情况调整ID和parent_id）:
--
-- 菜单名称: 数据交换记录
-- 菜单路径: /ietmddn/IetmDdnExchangeList
-- 组件路径: ietm/ietmddn/IetmDdnExchangeList
-- 父菜单: 数据交换（需要查询实际parent_id）
-- 菜单类型: 1（菜单）
-- 排序: 2
-- 图标: file-text
-- 权限标识: ietmddn:ietmDdnExchange:list
--
-- 按钮权限:
-- - 新增: ietmddn:ietmDdnExchange:add
-- - 编辑: ietmddn:ietmDdnExchange:edit
-- - 删除: ietmddn:ietmDdnExchange:delete
-- - 批量删除: ietmddn:ietmDdnExchange:deleteBatch
-- - 查看内容: ietmddn:ietmDdnExchange:view
-- - 导出: ietmddn:ietmDdnExchange:exportXls

-- 注意：JeecgBoot通常通过后台管理界面配置菜单，而不是直接插入SQL
-- 请在系统管理 -> 菜单管理中手动添加以下菜单配置
