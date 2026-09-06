-- ================================================================
-- 脚本名称：V1.0.1__add_unique_index_ietm_dm_reference.sql
-- 功能说明：为 ietm_dm_reference 表添加唯一索引，防止重复插入引用记录
-- 创建时间：2026-08-31
-- 作者：Claude Code
-- ================================================================

-- 说明：
-- 1. 唯一索引组合字段：(source_dm_id, target_dm_id, ref_type, ref_position)
-- 2. 这四个字段唯一标识一条DM引用关系
-- 3. 对标 ietm_icn_reference 表的 uk_icn_ref_dm 唯一索引设计
-- 4. 执行前请先检查是否存在重复数据，如有重复需先清理

-- ================================================================
-- 第一步：检查是否存在重复数据（执行前先运行此查询）
-- ================================================================
-- SELECT source_dm_id, target_dm_id, ref_type, ref_position, COUNT(*) as cnt
-- FROM ietm_dm_reference
-- GROUP BY source_dm_id, target_dm_id, ref_type, ref_position
-- HAVING COUNT(*) > 1;

-- 如果上述查询返回结果，说明存在重复数据，需先执行清理：
-- DELETE FROM ietm_dm_reference WHERE id IN (
--     SELECT id FROM (
--         SELECT id,
--                ROW_NUMBER() OVER (PARTITION BY source_dm_id, target_dm_id, ref_type, ref_position
--                                   ORDER BY create_time ASC) as rn
--         FROM ietm_dm_reference
--     ) t WHERE rn > 1
-- );

-- ================================================================
-- 第二步：创建唯一索引
-- ================================================================

-- 删除旧索引（如果存在）
BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX uk_dm_ref_unique';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1418 THEN  -- -1418 = index does not exist in DM8
            RAISE;
        END IF;
END;
/

-- 创建唯一索引
CREATE UNIQUE INDEX uk_dm_ref_unique ON ietm_dm_reference (
    source_dm_id,
    target_dm_id,
    ref_type,
    ref_position
);

-- 添加注释
COMMENT ON INDEX uk_dm_ref_unique IS '唯一索引：防止同一来源DM对同一目标DM在相同位置的相同类型引用重复插入';

-- ================================================================
-- 第三步：创建性能优化索引（可选，根据查询场景决定）
-- ================================================================

-- 来源DM查询索引（已存在则跳过）
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_dm_ref_source ON ietm_dm_reference(source_dm_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1408 THEN  -- index already exists
            DBMS_OUTPUT.PUT_LINE('索引 idx_dm_ref_source 已存在，跳过创建');
        ELSE
            RAISE;
        END IF;
END;
/

-- 目标DM查询索引（已存在则跳过）
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_dm_ref_target ON ietm_dm_reference(target_dm_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1408 THEN
            DBMS_OUTPUT.PUT_LINE('索引 idx_dm_ref_target 已存在，跳过创建');
        ELSE
            RAISE;
        END IF;
END;
/

-- 引用类型查询索引（已存在则跳过）
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_dm_ref_type ON ietm_dm_reference(ref_type)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1408 THEN
            DBMS_OUTPUT.PUT_LINE('索引 idx_dm_ref_type 已存在，跳过创建');
        ELSE
            RAISE;
        END IF;
END;
/

-- ================================================================
-- 验证脚本
-- ================================================================
-- 执行后运行以下查询验证索引创建成功：
-- SELECT index_name, uniqueness, status
-- FROM user_indexes
-- WHERE table_name = 'IETM_DM_REFERENCE';

COMMIT;
