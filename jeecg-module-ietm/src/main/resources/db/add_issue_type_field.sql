-- ============================================================================
-- 添加 issue_type 字段以符合S1000D标准
-- 创建时间: 2026-08-16
-- 用途: 存储数据模块的issueType，支持S1000D定义的8种枚举值
-- ============================================================================

-- 1. 添加 issue_type 字段
ALTER TABLE ietm_data_module ADD issue_type VARCHAR2(20);

-- 2. 添加注释
COMMENT ON COLUMN ietm_data_module.issue_type IS '版本类型(S1000D标准): new/changed/revised/deleted/status/rinstate-changed/rinstate-revised/rinstate-status';

-- 3. 添加检查约束，确保只能是S1000D标准的8种合法值
ALTER TABLE ietm_data_module ADD CONSTRAINT chk_issue_type
  CHECK (issue_type IN ('new', 'changed', 'revised', 'deleted', 'status',
                        'rinstate-changed', 'rinstate-revised', 'rinstate-status'));

-- 4. 为已有数据初始化 issue_type 值
-- 规则：issue_no=001 -> 'new', issue_no>001 -> 'revised'
UPDATE ietm_data_module
SET issue_type = CASE
  WHEN issue_no = '001' THEN 'new'
  WHEN issue_no > '001' THEN 'revised'
  ELSE 'new'
END
WHERE issue_type IS NULL;

-- 5. 提交变更
COMMIT;

-- 验证SQL
SELECT
  issue_type,
  COUNT(*) as count,
  MIN(issue_no) as min_issue_no,
  MAX(issue_no) as max_issue_no
FROM ietm_data_module
GROUP BY issue_type
ORDER BY issue_type;
