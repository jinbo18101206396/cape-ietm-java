-- =============================================
-- 机构遴选测试模块建表脚本（DM8 兼容）
-- 日期：2026-04-29
-- =============================================

-- ----------------------------
-- 1. 测试机构表
-- ----------------------------
CREATE TABLE test_agency (
    id                  VARCHAR(36) PRIMARY KEY,
    sys_org_code        VARCHAR(64) DEFAULT NULL,
    agency_name         VARCHAR(200) NOT NULL,
    score2023           INT NOT NULL DEFAULT 0,
    score2024           INT NOT NULL DEFAULT 0,
    score2025           INT NOT NULL DEFAULT 0,
    avg_score           DECIMAL(5,2) DEFAULT NULL,
    grade               VARCHAR(10) DEFAULT NULL,
    weight_coefficient  DECIMAL(3,1) DEFAULT NULL,
    ratio               INT NOT NULL DEFAULT 0,
    ut_ratio            DECIMAL(5,2) NOT NULL DEFAULT 0,
    project_count       INT NOT NULL DEFAULT 0,
    create_by           VARCHAR(50) DEFAULT NULL,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by           VARCHAR(50) DEFAULT NULL,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag            INT NOT NULL DEFAULT 0
);

COMMENT ON TABLE test_agency IS '测试-机构遴选-机构表';
COMMENT ON COLUMN test_agency.id IS '主键';
COMMENT ON COLUMN test_agency.sys_org_code IS '所属部门';
COMMENT ON COLUMN test_agency.agency_name IS '机构名称';
COMMENT ON COLUMN test_agency.score2023 IS '2023年绩效分';
COMMENT ON COLUMN test_agency.score2024 IS '2024年绩效分';
COMMENT ON COLUMN test_agency.score2025 IS '2025年绩效分';
COMMENT ON COLUMN test_agency.avg_score IS '三年平均分';
COMMENT ON COLUMN test_agency.grade IS '绩效等级';
COMMENT ON COLUMN test_agency.weight_coefficient IS '权重系数';
COMMENT ON COLUMN test_agency.ratio IS '本次摇号预设比例(%)';
COMMENT ON COLUMN test_agency.ut_ratio IS '本年度已承担项目比例(%)';
COMMENT ON COLUMN test_agency.project_count IS '本年度已承担项目数';
COMMENT ON COLUMN test_agency.create_by IS '创建人';
COMMENT ON COLUMN test_agency.create_time IS '创建时间';
COMMENT ON COLUMN test_agency.update_by IS '更新人';
COMMENT ON COLUMN test_agency.update_time IS '更新时间';
COMMENT ON COLUMN test_agency.del_flag IS '删除标志';

-- ----------------------------
-- 2. 测试项目表
-- ----------------------------
CREATE TABLE test_project (
    id              VARCHAR(36) PRIMARY KEY,
    sys_org_code    VARCHAR(64) DEFAULT NULL,
    project_name    VARCHAR(200) NOT NULL,
    quantity        INT NOT NULL DEFAULT 1,
    status          VARCHAR(20) NOT NULL DEFAULT '待分配',
    agency_id       VARCHAR(36) DEFAULT NULL,
    agency_name     VARCHAR(200) DEFAULT NULL,
    extract_time    VARCHAR(30) DEFAULT NULL,
    extract_user    VARCHAR(50) DEFAULT NULL,
    remarks         VARCHAR(1000) DEFAULT NULL,
    create_by       VARCHAR(50) DEFAULT NULL,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(50) DEFAULT NULL,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag        INT NOT NULL DEFAULT 0
);

COMMENT ON TABLE test_project IS '测试-机构遴选-项目表';
COMMENT ON COLUMN test_project.id IS '主键';
COMMENT ON COLUMN test_project.sys_org_code IS '所属部门';
COMMENT ON COLUMN test_project.project_name IS '项目名称';
COMMENT ON COLUMN test_project.quantity IS '项目数量';
COMMENT ON COLUMN test_project.status IS '状态：已分配/待分配';
COMMENT ON COLUMN test_project.agency_id IS '分配机构ID';
COMMENT ON COLUMN test_project.agency_name IS '机构名称（冗余）';
COMMENT ON COLUMN test_project.extract_time IS '抽取时间';
COMMENT ON COLUMN test_project.extract_user IS '抽取人员';
COMMENT ON COLUMN test_project.remarks IS '备注';
COMMENT ON COLUMN test_project.create_by IS '创建人';
COMMENT ON COLUMN test_project.create_time IS '创建时间';
COMMENT ON COLUMN test_project.update_by IS '更新人';
COMMENT ON COLUMN test_project.update_time IS '更新时间';
COMMENT ON COLUMN test_project.del_flag IS '删除标志';

-- ----------------------------
-- 3. 测试附件表
-- ----------------------------
CREATE TABLE test_agency_file (
    id          VARCHAR(36) PRIMARY KEY,
    biz_id      VARCHAR(36) NOT NULL,
    biz_type    VARCHAR(50) NOT NULL,
    file_name   VARCHAR(500) NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    create_by   VARCHAR(50) DEFAULT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(50) DEFAULT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT NOT NULL DEFAULT 0
);

COMMENT ON TABLE test_agency_file IS '测试-机构遴选-附件表';
COMMENT ON COLUMN test_agency_file.id IS '主键';
COMMENT ON COLUMN test_agency_file.biz_id IS '业务ID（机构ID或项目ID）';
COMMENT ON COLUMN test_agency_file.biz_type IS '业务类型：agency / project';
COMMENT ON COLUMN test_agency_file.file_name IS '文件名';
COMMENT ON COLUMN test_agency_file.file_path IS '文件路径';
COMMENT ON COLUMN test_agency_file.create_by IS '创建人';
COMMENT ON COLUMN test_agency_file.create_time IS '创建时间';
COMMENT ON COLUMN test_agency_file.update_by IS '更新人';
COMMENT ON COLUMN test_agency_file.update_time IS '更新时间';
COMMENT ON COLUMN test_agency_file.del_flag IS '删除标志';
