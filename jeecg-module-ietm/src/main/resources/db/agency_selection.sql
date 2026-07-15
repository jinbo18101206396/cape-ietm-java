-- =============================================
-- 机构遴选模块建表脚本（DM8 兼容）
-- 日期：2026-04-28
-- 注意：DM8不支持在列定义中写COMMENT，所有注释使用独立的COMMENT ON COLUMN语句
-- =============================================

-- ----------------------------
-- 1. 机构表
-- ----------------------------
CREATE TABLE agency (
    id                  VARCHAR(36) PRIMARY KEY,
    sys_org_code        VARCHAR(64) DEFAULT NULL,
    agency_name         VARCHAR(200) NOT NULL UNIQUE,
    score_2023          INT NOT NULL DEFAULT 0,
    score_2024          INT NOT NULL DEFAULT 0,
    score_2025          INT NOT NULL DEFAULT 0,
    avg_score           DECIMAL(5,2) DEFAULT NULL,
    grade               VARCHAR(10) DEFAULT NULL,
    weight_coefficient  DECIMAL(3,1) DEFAULT NULL,
    ratio               INT NOT NULL DEFAULT 0,
    ut_ratio            DECIMAL(5,2) NOT NULL DEFAULT 0,
    project_count       INT NOT NULL DEFAULT 0,
    create_by           VARCHAR(50) DEFAULT NULL,
    create_time         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by           VARCHAR(50) DEFAULT NULL,
    update_time         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag            INT NOT NULL DEFAULT 0
);

COMMENT ON TABLE agency IS '机构遴选-机构表';
COMMENT ON COLUMN agency.id IS '主键';
COMMENT ON COLUMN agency.sys_org_code IS '所属部门';
COMMENT ON COLUMN agency.agency_name IS '机构名称';
COMMENT ON COLUMN agency.score_2023 IS '2023年绩效分';
COMMENT ON COLUMN agency.score_2024 IS '2024年绩效分';
COMMENT ON COLUMN agency.score_2025 IS '2025年绩效分';
COMMENT ON COLUMN agency.avg_score IS '三年平均分';
COMMENT ON COLUMN agency.grade IS '绩效等级';
COMMENT ON COLUMN agency.weight_coefficient IS '权重系数';
COMMENT ON COLUMN agency.ratio IS '本次摇号预设比例(%)';
COMMENT ON COLUMN agency.ut_ratio IS '本年度已承担项目比例(%)';
COMMENT ON COLUMN agency.project_count IS '本年度已承担项目数';
COMMENT ON COLUMN agency.create_by IS '创建人';
COMMENT ON COLUMN agency.create_time IS '创建时间';
COMMENT ON COLUMN agency.update_by IS '更新人';
COMMENT ON COLUMN agency.update_time IS '更新时间';
COMMENT ON COLUMN agency.del_flag IS '删除标志';

-- ----------------------------
-- 2. 项目表
-- ----------------------------
CREATE TABLE project (
    id              VARCHAR(36) PRIMARY KEY,
    sys_org_code    VARCHAR(64) DEFAULT NULL,
    project_name    VARCHAR(200) NOT NULL UNIQUE,
    quantity        INT NOT NULL DEFAULT 1,
    status          VARCHAR(20) NOT NULL DEFAULT '待分配',
    agency_id       VARCHAR(36) DEFAULT NULL,
    agency_name     VARCHAR(200) DEFAULT NULL,
    extract_time    VARCHAR(30) DEFAULT NULL,
    extract_user    VARCHAR(50) DEFAULT NULL,
    remarks         VARCHAR(1000) DEFAULT NULL,
    create_by       VARCHAR(50) DEFAULT NULL,
    create_time     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(50) DEFAULT NULL,
    update_time     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag        INT NOT NULL DEFAULT 0
);

COMMENT ON TABLE project IS '机构遴选-项目表';
COMMENT ON COLUMN project.id IS '主键';
COMMENT ON COLUMN project.sys_org_code IS '所属部门';
COMMENT ON COLUMN project.project_name IS '项目名称';
COMMENT ON COLUMN project.quantity IS '项目数量';
COMMENT ON COLUMN project.status IS '状态：已分配/待分配';
COMMENT ON COLUMN project.agency_id IS '分配机构ID';
COMMENT ON COLUMN project.agency_name IS '机构名称（冗余）';
COMMENT ON COLUMN project.extract_time IS '抽取时间';
COMMENT ON COLUMN project.extract_user IS '抽取人员';
COMMENT ON COLUMN project.remarks IS '备注';
COMMENT ON COLUMN project.create_by IS '创建人';
COMMENT ON COLUMN project.create_time IS '创建时间';
COMMENT ON COLUMN project.update_by IS '更新人';
COMMENT ON COLUMN project.update_time IS '更新时间';
COMMENT ON COLUMN project.del_flag IS '删除标志';

-- ----------------------------
-- 3. 附件表
-- ----------------------------
CREATE TABLE agency_file (
    id          VARCHAR(36) PRIMARY KEY,
    biz_id      VARCHAR(36) NOT NULL,
    biz_type    VARCHAR(50) NOT NULL,
    file_name   VARCHAR(500) NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    create_by   VARCHAR(50) DEFAULT NULL,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(50) DEFAULT NULL,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT NOT NULL DEFAULT 0,
    UNIQUE (biz_id, file_name)
);

COMMENT ON TABLE agency_file IS '机构遴选-附件表';
COMMENT ON COLUMN agency_file.id IS '主键';
COMMENT ON COLUMN agency_file.biz_id IS '业务ID（机构ID或项目ID）';
COMMENT ON COLUMN agency_file.biz_type IS '业务类型：agency / project';
COMMENT ON COLUMN agency_file.file_name IS '文件名';
COMMENT ON COLUMN agency_file.file_path IS '文件路径';
COMMENT ON COLUMN agency_file.create_by IS '创建人';
COMMENT ON COLUMN agency_file.create_time IS '创建时间';
COMMENT ON COLUMN agency_file.update_by IS '更新人';
COMMENT ON COLUMN agency_file.update_time IS '更新时间';
COMMENT ON COLUMN agency_file.del_flag IS '删除标志';

-- ----------------------------
-- 4. 索引
-- ----------------------------
CREATE INDEX idx_agency_file_biz ON agency_file(biz_id, biz_type);
CREATE INDEX idx_project_status ON project(status);
CREATE INDEX idx_project_agency ON project(agency_id);
CREATE INDEX idx_project_del_flag ON project(del_flag);
CREATE INDEX idx_agency_del_flag ON agency(del_flag);
CREATE INDEX idx_agency_ratio ON agency(ratio, del_flag);

-- ----------------------------
-- 5. 演示数据
-- ----------------------------
INSERT INTO agency (id, agency_name, score_2023, score_2024, score_2025, ratio, ut_ratio, project_count, del_flag) VALUES
('a001', '测试机构A', 92, 95, 88, 30, 0, 0, 0),
('a002', '测试机构B', 85, 90, 94, 20, 0, 0, 0),
('a003', '测试机构C', 78, 82, 80, 25, 0, 0, 0),
('a004', '测试机构D', 96, 91, 89, 15, 0, 0, 0);
