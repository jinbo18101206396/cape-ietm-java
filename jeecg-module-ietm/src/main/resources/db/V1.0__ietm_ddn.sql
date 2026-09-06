-- ============================================================
-- IETM DDN 数据交换凭证表 - 达梦数据库（DM8）
-- 版本：V1.0
-- 日期：2026-09-01
-- 作者：jeecg-boot
-- 说明：导出数据模块功能所需的DDN记录表
-- ============================================================

-- 1. 创建表
CREATE TABLE IF NOT EXISTS ietm_ddn (
    id                        VARCHAR2(32)   NOT NULL,
    project_id                VARCHAR2(32),
    ddn_code                  VARCHAR2(200)  NOT NULL,
    model_ident_code          VARCHAR2(50),
    sender_ident              VARCHAR2(50),
    receiver_ident            VARCHAR2(50),
    year_of_data_issue        VARCHAR2(4),
    seq_number                VARCHAR2(10),
    security                  VARCHAR2(10),
    commercial_security       VARCHAR2(10),
    caveat                    VARCHAR2(10),
    issue_date                TIMESTAMP,
    dm_ids                    CLOB,
    include_ref_icn           CHAR(1)        DEFAULT '1',
    include_ref_dm            CHAR(1)        DEFAULT '1',
    include_dm_resource       CHAR(1)        DEFAULT '1',
    ddn_file_path             VARCHAR2(500),
    dm_count                  INT            DEFAULT 0,
    status                    VARCHAR2(10)   DEFAULT '1',
    create_by                 VARCHAR2(32),
    create_time               TIMESTAMP,
    update_by                 VARCHAR2(32),
    update_time               TIMESTAMP,
    sys_org_code              VARCHAR2(64),
    CONSTRAINT pk_ietm_ddn PRIMARY KEY (id)
);

-- 2. 注释
COMMENT ON TABLE ietm_ddn IS 'DDN数据交换凭证表';
COMMENT ON COLUMN ietm_ddn.id IS '主键';
COMMENT ON COLUMN ietm_ddn.project_id IS '所属项目ID';
COMMENT ON COLUMN ietm_ddn.ddn_code IS 'DDN完整编码';
COMMENT ON COLUMN ietm_ddn.model_ident_code IS '型号代码';
COMMENT ON COLUMN ietm_ddn.sender_ident IS '导出单位代码';
COMMENT ON COLUMN ietm_ddn.receiver_ident IS '接收单位代码';
COMMENT ON COLUMN ietm_ddn.year_of_data_issue IS '发布年份（4位）';
COMMENT ON COLUMN ietm_ddn.seq_number IS '序列号（5位补零）';
COMMENT ON COLUMN ietm_ddn.security IS '密级';
COMMENT ON COLUMN ietm_ddn.commercial_security IS '商业密级';
COMMENT ON COLUMN ietm_ddn.caveat IS '警告';
COMMENT ON COLUMN ietm_ddn.issue_date IS '发布日期';
COMMENT ON COLUMN ietm_ddn.dm_ids IS '包含的DM ID列表（逗号分隔）';
COMMENT ON COLUMN ietm_ddn.include_ref_icn IS '是否含引用ICN（1是0否）';
COMMENT ON COLUMN ietm_ddn.include_ref_dm IS '是否含引用DM（1是0否）';
COMMENT ON COLUMN ietm_ddn.include_dm_resource IS '是否含DM资源（1是0否）';
COMMENT ON COLUMN ietm_ddn.ddn_file_path IS 'DDN文件相对路径';
COMMENT ON COLUMN ietm_ddn.dm_count IS '导出DM数量（含引用）';
COMMENT ON COLUMN ietm_ddn.status IS '状态（1正常0删除）';
COMMENT ON COLUMN ietm_ddn.create_by IS '创建人';
COMMENT ON COLUMN ietm_ddn.create_time IS '创建时间';
COMMENT ON COLUMN ietm_ddn.update_by IS '更新人';
COMMENT ON COLUMN ietm_ddn.update_time IS '更新时间';
COMMENT ON COLUMN ietm_ddn.sys_org_code IS '所属部门';

-- 3. 索引
CREATE INDEX idx_ietm_ddn_year ON ietm_ddn (year_of_data_issue);
CREATE INDEX idx_ietm_ddn_project ON ietm_ddn (project_id);
CREATE INDEX idx_ietm_ddn_create_time ON ietm_ddn (create_time DESC);
CREATE UNIQUE INDEX uk_ietm_ddn_code ON ietm_ddn (ddn_code);

-- 4. 验证
SELECT COUNT(*) FROM ietm_ddn;  -- 应返回 0（新表）
