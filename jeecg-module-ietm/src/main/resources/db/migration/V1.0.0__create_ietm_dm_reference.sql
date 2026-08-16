-- ================================================================
-- DM引用关系表创建脚本 (DM8数据库)
-- 功能：记录数据模块(DM)之间的引用关系
-- 创建日期：2026-08-15
-- ================================================================

-- 检查表是否存在（DM8语法）
-- 如果表已存在，可以注释掉DROP语句，直接跳过
-- DROP TABLE IF EXISTS ietm_dm_reference;

-- 创建引用关系表
CREATE TABLE ietm_dm_reference (
    id              VARCHAR(32) NOT NULL,
    source_dm_id    VARCHAR(32) NOT NULL,
    target_dm_id    VARCHAR(32) NOT NULL,
    ref_type        VARCHAR(20),
    ref_dmc         VARCHAR(200),
    target_dmc      VARCHAR(200),
    ref_position    VARCHAR(500),
    create_by       VARCHAR(50),
    create_time     TIMESTAMP,
    update_by       VARCHAR(50),
    update_time     TIMESTAMP,
    sys_org_code    VARCHAR(64),
    CONSTRAINT pk_ietm_dm_reference PRIMARY KEY (id)
);

-- 添加列注释
COMMENT ON TABLE ietm_dm_reference IS 'IETM数据模块引用关系表';
COMMENT ON COLUMN ietm_dm_reference.id IS '主键ID';
COMMENT ON COLUMN ietm_dm_reference.source_dm_id IS '引用方DM的ID（出引用一侧）';
COMMENT ON COLUMN ietm_dm_reference.target_dm_id IS '被引用方DM的ID（入引用一侧）';
COMMENT ON COLUMN ietm_dm_reference.ref_type IS '引用类型（dmRef/dmlRef/pmRef/graphic/multimedia）';
COMMENT ON COLUMN ietm_dm_reference.ref_dmc IS '引用方的DMC完整编码';
COMMENT ON COLUMN ietm_dm_reference.target_dmc IS '被引用方的DMC完整编码';
COMMENT ON COLUMN ietm_dm_reference.ref_position IS '引用在XML中的位置（XPath路径）';
COMMENT ON COLUMN ietm_dm_reference.create_by IS '创建人';
COMMENT ON COLUMN ietm_dm_reference.create_time IS '创建时间';
COMMENT ON COLUMN ietm_dm_reference.update_by IS '更新人';
COMMENT ON COLUMN ietm_dm_reference.update_time IS '更新时间';
COMMENT ON COLUMN ietm_dm_reference.sys_org_code IS '所属部门编码';

-- 创建索引（提升查询性能）
-- ① 出引用查询索引（查询某个DM引用了哪些DM）
CREATE INDEX idx_dmref_source ON ietm_dm_reference(source_dm_id);

-- ② 入引用查询索引（查询哪些DM引用了某个DM）
CREATE INDEX idx_dmref_target ON ietm_dm_reference(target_dm_id);

-- ③ 引用类型索引（按类型分类查询）
CREATE INDEX idx_dmref_type ON ietm_dm_reference(ref_type);

-- ④ 联合索引（优化统计查询）
CREATE INDEX idx_dmref_source_target ON ietm_dm_reference(source_dm_id, target_dm_id);

-- 添加外键约束（可选，根据实际需求决定是否启用）
-- 注意：外键约束会影响删除性能，如果业务逻辑已经保证数据完整性，可以不加
-- ALTER TABLE ietm_dm_reference ADD CONSTRAINT fk_dmref_source
--     FOREIGN KEY (source_dm_id) REFERENCES ietm_data_module(id) ON DELETE CASCADE;
-- ALTER TABLE ietm_dm_reference ADD CONSTRAINT fk_dmref_target
--     FOREIGN KEY (target_dm_id) REFERENCES ietm_data_module(id) ON DELETE CASCADE;

-- 初始化数据（可选）
-- 如果需要初始化一些测试数据，可以在这里添加
-- INSERT INTO ietm_dm_reference (id, source_dm_id, target_dm_id, ref_type, ref_dmc, target_dmc, create_by, create_time)
-- VALUES ('test001', 'dm001', 'dm002', 'dmRef', 'DMC-001', 'DMC-002', 'admin', SYSDATE);

-- 验证表创建成功
SELECT 'Table ietm_dm_reference created successfully' AS result FROM DUAL;
