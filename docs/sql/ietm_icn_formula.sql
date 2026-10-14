-- =====================================================
-- IETM公式ICN独立存储表
-- =====================================================
-- 设计目的：
--   1. 绕过DM8聚集索引限制（ietm_icn_manage有CLUSTER索引无法添加CLOB）
--   2. 公式存储需求：完整的<img>标签HTML（含base64图片+LaTeX源码）
--   3. 与图符ICN完全隔离，互不影响
--
-- 表结构：
--   - 主键id：普通索引（非CLUSTER），允许添加CLOB字段
--   - icn：ICN完整编码（唯一索引）
--   - filecontent：CLOB字段存储公式HTML
--   - 其他字段：对齐ietm_icn_manage的核心字段
--
-- 对标旧系统：
--   旧系统Oracle/MySQL使用ietm_icn_manage.filecontent BLOB存储
--   新系统DM8因聚集索引限制，改用独立表存储
-- =====================================================

CREATE TABLE ietm_icn_formula (
    id                      VARCHAR(20) PRIMARY KEY,                    -- 雪花ID主键（普通索引，非CLUSTER）
    icn                     VARCHAR(100) NOT NULL UNIQUE,               -- ICN完整编码（唯一索引）
    sns                     VARCHAR(30),                                -- SNS编码
    rpc                     VARCHAR(10),                                -- RPC代码
    originator              VARCHAR(10),                                -- 发起者代码
    unique_id               VARCHAR(10),                                -- 唯一识别码
    variant_code            VARCHAR(10),                                -- 变体代码
    security_classification VARCHAR(10),                                -- 安全分类
    issue_no                VARCHAR(10),                                -- 发行号
    security                INT,                                        -- 密级
    cm_node_id              VARCHAR(20),                                -- 构型节点ID
    filecontent             CLOB,                                       -- 公式HTML内容（完整<img>标签）
    icn_type                VARCHAR(20) DEFAULT 'formula',              -- ICN类型（固定为formula）
    version                 VARCHAR(10),                                -- 版本号
    ispublished             CHAR(1) DEFAULT '0',                        -- 是否已发布（0：否，1：是）
    isdeleted               CHAR(1) DEFAULT '0',                        -- 是否已删除（0：否，1：是）
    create_by               VARCHAR(50),                                -- 创建人
    create_time             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 创建时间
    update_by               VARCHAR(50),                                -- 更新人
    update_time             TIMESTAMP,                                  -- 更新时间
    sys_org_code            VARCHAR(64)                                 -- 组织机构编码
);

-- 创建索引
CREATE INDEX idx_icn_formula_cmnode ON ietm_icn_formula(cm_node_id);
CREATE INDEX idx_icn_formula_createtime ON ietm_icn_formula(create_time);

-- 添加表注释
COMMENT ON TABLE ietm_icn_formula IS 'IETM公式ICN独立存储表（绕过DM8聚集索引限制）';
COMMENT ON COLUMN ietm_icn_formula.id IS '主键ID（普通索引，非CLUSTER）';
COMMENT ON COLUMN ietm_icn_formula.icn IS 'ICN完整编码';
COMMENT ON COLUMN ietm_icn_formula.filecontent IS '公式HTML内容（完整<img class="kfformula".../>标签，含base64图片和data-latex属性）';
COMMENT ON COLUMN ietm_icn_formula.icn_type IS 'ICN类型（固定为formula）';

-- 权限授予
GRANT SELECT, INSERT, UPDATE, DELETE ON ietm_icn_formula TO IETM;
