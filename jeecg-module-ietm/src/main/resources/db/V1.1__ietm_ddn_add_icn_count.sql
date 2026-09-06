-- ========================================
-- DDN表新增icn_count字段（修复P1-5）
-- 用于记录导出的ICN数量，与dm_count对齐
-- ========================================

ALTER TABLE ietm_ddn ADD icn_count INT DEFAULT 0;

COMMENT ON COLUMN ietm_ddn.icn_count IS '导出ICN数量（含引用）';
