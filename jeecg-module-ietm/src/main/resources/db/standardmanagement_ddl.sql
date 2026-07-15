-- ============================================
-- 手册标准管理 DDL (DM8 达梦数据库)
-- 表: ietm_standard, ietm_standard_dmtype
-- ============================================

-- 主表: 标准管理左侧树
CREATE TABLE ietm_standard (
    id            VARCHAR(32)  NOT NULL COMMENT '主键',
    create_by     VARCHAR(50)  COMMENT '创建人',
    create_time   TIMESTAMP    COMMENT '创建日期',
    update_by     VARCHAR(50)  COMMENT '更新人',
    update_time   TIMESTAMP    COMMENT '更新日期',
    sys_org_code  VARCHAR(64)  COMMENT '所属部门',
    name          VARCHAR(100) NOT NULL COMMENT '标准名称',
    security      INT          COMMENT '密级',
    PRIMARY KEY (id)
);
COMMENT ON TABLE ietm_standard IS '手册管理-标准管理左侧树';
COMMENT ON COLUMN ietm_standard.name IS '标准名称';
COMMENT ON COLUMN ietm_standard.security IS '密级';

-- 子表: 标准数据模块类型(DM类型)
CREATE TABLE ietm_standard_dmtype (
    id            VARCHAR(32)  NOT NULL COMMENT '主键',
    create_by     VARCHAR(50)  COMMENT '创建人',
    create_time   TIMESTAMP    COMMENT '创建日期',
    update_by     VARCHAR(50)  COMMENT '更新人',
    update_time   TIMESTAMP    COMMENT '更新日期',
    sys_org_code  VARCHAR(64)  COMMENT '所属部门',
    pid           VARCHAR(32)  NOT NULL COMMENT '标准id(关联ietm_standard.id)',
    dmtype_name   VARCHAR(100) NOT NULL COMMENT 'DM类型名称',
    dtd           VARCHAR(200) COMMENT 'DTD/Schema',
    description   VARCHAR(500) COMMENT '描述',
    update_ip     VARCHAR(50)  COMMENT '最后修改IP',
    security      INT          COMMENT '密级',
    version       VARCHAR(50)  COMMENT '版本',
    PRIMARY KEY (id)
);
COMMENT ON TABLE ietm_standard_dmtype IS '手册管理-标准管理列表（标准数据模块）';
COMMENT ON COLUMN ietm_standard_dmtype.pid IS '标准id';
COMMENT ON COLUMN ietm_standard_dmtype.dmtype_name IS 'DM类型名称';
COMMENT ON COLUMN ietm_standard_dmtype.dtd IS 'DTD';

-- 索引
CREATE INDEX idx_standard_dmtype_pid ON ietm_standard_dmtype(pid);
