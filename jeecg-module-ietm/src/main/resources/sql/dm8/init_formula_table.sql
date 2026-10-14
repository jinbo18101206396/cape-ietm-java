--================================================================
-- 公式ICN表初始化脚本（DM8方言）
-- 创建日期: 2026-10-13
-- 版本: 1.0
--================================================================

-- 创建公式ICN表
CREATE TABLE IETM_ICN_FORMULA (
    ID                      VARCHAR(32) PRIMARY KEY,
    ICN                     VARCHAR(100) NOT NULL,
    ICN_NAME                VARCHAR(200),
    FORMULA_TYPE            VARCHAR(10),
    LATEX_SOURCE            CLOB,
    FORMULA_HTML            CLOB,
    BASE64_IMAGE            CLOB,
    IMAGE_WIDTH             INTEGER,
    IMAGE_HEIGHT            INTEGER,
    IMAGE_SCALE             INTEGER DEFAULT 100,
    DM_ID                   VARCHAR(32),
    DM_CODE                 VARCHAR(50),
    ELEMENT_ID              VARCHAR(32),
    UNIQUE_ID_IN_DM         INTEGER,
    SNS                     VARCHAR(50),
    ORIGINATOR              VARCHAR(10),
    RPC                     VARCHAR(10),
    VARIANT_CODE            VARCHAR(10) DEFAULT 'A',
    ISSUE_NO                VARCHAR(10) DEFAULT '001',
    SECURITY_CLASS          VARCHAR(10) DEFAULT '02',
    CREATE_BY               VARCHAR(32),
    CREATE_TIME             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATE_BY               VARCHAR(32),
    UPDATE_TIME             TIMESTAMP,
    IS_DELETE               INTEGER DEFAULT 0,
    REMARK                  VARCHAR(500),
    CONSTRAINT UK_ICN_DM UNIQUE (ICN, DM_ID)
);

-- 创建索引
CREATE INDEX IDX_IETM_ICN_FORMULA_ICN ON IETM_ICN_FORMULA(ICN);
CREATE INDEX IDX_IETM_ICN_FORMULA_DM_ID ON IETM_ICN_FORMULA(DM_ID);
CREATE INDEX IDX_IETM_ICN_FORMULA_ELEMENT_ID ON IETM_ICN_FORMULA(ELEMENT_ID);
CREATE INDEX IDX_IETM_ICN_FORMULA_CREATE_TIME ON IETM_ICN_FORMULA(CREATE_TIME);

COMMIT;
