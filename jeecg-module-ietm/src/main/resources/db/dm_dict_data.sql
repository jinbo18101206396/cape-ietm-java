-- ========================================
-- IETM数据模块管理 - 数据字典配置SQL
-- 参考标准：ICN管理模块（已验证可用）
-- 字典编码：使用简洁命名（与ICN保持一致）
-- ========================================

-- 注意：密级字典(security)已在ICN模块创建，无需重复创建

-- ========================================
-- 1. DM类型字典 (dm_type)
-- ========================================
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_time)
VALUES ('1750000000010', 'DM类型', 'dm_type', 'IETM数据模块类型分类', 0, NOW());

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time) VALUES
('1750000001001', '1750000000010', '描述性DM', 'description', '描述性数据模块', 1, 1, NOW()),
('1750000001002', '1750000000010', '过程性DM', 'procedure', '过程性数据模块', 2, 1, NOW()),
('1750000001003', '1750000000010', '故障性DM', 'faultIsolation', '故障性数据模块', 3, 1, NOW()),
('1750000001004', '1750000000010', '乘员DM', 'crew', '乘员数据模块', 4, 1, NOW()),
('1750000001005', '1750000000010', '规划性DM', 'maintPlanning', '规划性数据模块', 5, 1, NOW()),
('1750000001006', '1750000000010', '工艺性DM', 'process', '工艺性数据模块', 6, 1, NOW()),
('1750000001007', '1750000000010', '故障报告DM', 'faultReporting', '故障报告数据模块', 7, 1, NOW()),
('1750000001008', '1750000000010', '图解零件目录DM', 'illustratedPartsCatalog', '图解零件目录数据模块', 8, 1, NOW());

-- ========================================
-- 2. DM状态字典 (dm_status)
-- ========================================
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_time)
VALUES ('1750000000011', 'DM状态', 'dm_status', 'IETM数据模块状态', 0, NOW());

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time) VALUES
('1750000001101', '1750000000011', '草稿', 'draft', '草稿状态', 1, 1, NOW()),
('1750000001102', '1750000000011', '编辑中', 'editing', '正在编辑', 2, 1, NOW()),
('1750000001103', '1750000000011', '审核中', 'reviewing', '正在审核', 3, 1, NOW()),
('1750000001104', '1750000000011', '已发布', 'published', '已发布状态', 4, 1, NOW()),
('1750000001105', '1750000000011', '已归档', 'archived', '已归档状态', 5, 1, NOW());

-- ========================================
-- 3. 工作流状态字典 (workflow_status)
-- ========================================
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_time)
VALUES ('1750000000012', '工作流状态', 'workflow_status', 'IETM工作流状态', 0, NOW());

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time) VALUES
('1750000001201', '1750000000012', '未提交', '0', '未提交工作流', 1, 1, NOW()),
('1750000001202', '1750000000012', '流转中', '1', '流程流转中', 2, 1, NOW()),
('1750000001203', '1750000000012', '已通过', '2', '审批通过', 3, 1, NOW()),
('1750000001204', '1750000000012', '已拒绝', '3', '审批拒绝', 4, 1, NOW());

-- ========================================
-- 4. 语言代码字典 (language)
-- ========================================
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_time)
VALUES ('1750000000013', '语言代码', 'language', 'ISO 639-1语言代码', 0, NOW());

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time) VALUES
('1750000001301', '1750000000013', '中文', 'zh', '简体中文', 1, 1, NOW()),
('1750000001302', '1750000000013', '英文', 'en', 'English', 2, 1, NOW()),
('1750000001303', '1750000000013', '繁体中文', 'zh-TW', '繁體中文', 3, 1, NOW()),
('1750000001304', '1750000000013', '日文', 'ja', '日本語', 4, 1, NOW()),
('1750000001305', '1750000000013', '韩文', 'ko', '한국어', 5, 1, NOW()),
('1750000001306', '1750000000013', '法文', 'fr', 'Français', 6, 1, NOW()),
('1750000001307', '1750000000013', '德文', 'de', 'Deutsch', 7, 1, NOW()),
('1750000001308', '1750000000013', '俄文', 'ru', 'Русский', 8, 1, NOW());

-- ========================================
-- 5. 国家代码字典 (country)
-- ========================================
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_time)
VALUES ('1750000000014', '国家代码', 'country', 'ISO 3166-1国家代码', 0, NOW());

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time) VALUES
('1750000001401', '1750000000014', '中国', 'CN', '中华人民共和国', 1, 1, NOW()),
('1750000001402', '1750000000014', '美国', 'US', 'United States', 2, 1, NOW()),
('1750000001403', '1750000000014', '英国', 'GB', 'United Kingdom', 3, 1, NOW()),
('1750000001404', '1750000000014', '日本', 'JP', '日本', 4, 1, NOW()),
('1750000001405', '1750000000014', '韩国', 'KR', '대한민국', 5, 1, NOW()),
('1750000001406', '1750000000014', '法国', 'FR', 'France', 6, 1, NOW()),
('1750000001407', '1750000000014', '德国', 'DE', 'Deutschland', 7, 1, NOW()),
('1750000001408', '1750000000014', '俄罗斯', 'RU', 'Россия', 8, 1, NOW());

-- ========================================
-- 6. 技术名称字典 (tech_name) - DMC第9段
-- ========================================
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_time)
VALUES ('1750000000015', '技术名称', 'tech_name', 'IETM技术名称（DMC第9段）', 0, NOW());

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time) VALUES
('1750000001501', '1750000000015', '安装', '00', '安装相关技术', 1, 1, NOW()),
('1750000001502', '1750000000015', '拆卸', '01', '拆卸相关技术', 2, 1, NOW()),
('1750000001503', '1750000000015', '检查', '02', '检查相关技术', 3, 1, NOW()),
('1750000001504', '1750000000015', '测试', '03', '测试相关技术', 4, 1, NOW()),
('1750000001505', '1750000000015', '维修', '04', '维修相关技术', 5, 1, NOW()),
('1750000001506', '1750000000015', '调整', '05', '调整相关技术', 6, 1, NOW()),
('1750000001507', '1750000000015', '润滑', '06', '润滑相关技术', 7, 1, NOW()),
('1750000001508', '1750000000015', '清洁', '07', '清洁相关技术', 8, 1, NOW());

-- ========================================
-- 7. 信息名称字典 (info_name) - DMC第10段
-- ========================================
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_time)
VALUES ('1750000000016', '信息名称', 'info_name', 'IETM信息名称（DMC第10段）', 0, NOW());

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time) VALUES
('1750000001601', '1750000000016', '描述', '00', '描述信息', 1, 1, NOW()),
('1750000001602', '1750000000016', '操作', '01', '操作信息', 2, 1, NOW()),
('1750000001603', '1750000000016', '规范', '02', '规范信息', 3, 1, NOW()),
('1750000001604', '1750000000016', '图示', '03', '图示信息', 4, 1, NOW()),
('1750000001605', '1750000000016', '数据', '04', '数据信息', 5, 1, NOW()),
('1750000001606', '1750000000016', '工具', '05', '工具信息', 6, 1, NOW()),
('1750000001607', '1750000000016', '耗材', '06', '耗材信息', 7, 1, NOW()),
('1750000001608', '1750000000016', '附件', '07', '附件信息', 8, 1, NOW());

-- ========================================
-- 8. 变体代码字典 (variant_code) - DMC第11段A
-- ========================================
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_time)
VALUES ('1750000000017', '变体代码', 'variant_code', 'IETM变体代码（DMC第11段A）', 0, NOW());

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time) VALUES
('1750000001701', '1750000000017', 'A型', 'A', 'A型变体', 1, 1, NOW()),
('1750000001702', '1750000000017', 'B型', 'B', 'B型变体', 2, 1, NOW()),
('1750000001703', '1750000000017', 'C型', 'C', 'C型变体', 3, 1, NOW()),
('1750000001704', '1750000000017', 'D型', 'D', 'D型变体', 4, 1, NOW()),
('1750000001705', '1750000000017', 'E型', 'E', 'E型变体', 5, 1, NOW());

-- ========================================
-- 9. 拆卸代码字典 (disassy_code) - DMC第11段B
-- ========================================
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_time)
VALUES ('1750000000018', '拆卸代码', 'disassy_code', 'IETM拆卸代码（DMC第11段B）', 0, NOW());

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time) VALUES
('1750000001801', '1750000000018', '拆卸到0级', '0', '完全拆卸', 1, 1, NOW()),
('1750000001802', '1750000000018', '拆卸到1级', '1', '拆卸到1级', 2, 1, NOW()),
('1750000001803', '1750000000018', '拆卸到2级', '2', '拆卸到2级', 3, 1, NOW()),
('1750000001804', '1750000000018', '拆卸到3级', '3', '拆卸到3级', 4, 1, NOW()),
('1750000001805', '1750000000018', '拆卸到4级', '4', '拆卸到4级', 5, 1, NOW());

-- ========================================
-- 10. 发布类型字典 (issue_type) - DMC第11段C
-- ========================================
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_time)
VALUES ('1750000000019', '发布类型', 'issue_type', 'IETM发布类型（DMC第11段C）', 0, NOW());

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time) VALUES
('1750000001901', '1750000000019', '正式发布', 'N', '正式发布版本', 1, 1, NOW()),
('1750000001902', '1750000000019', '临时发布', 'T', '临时发布版本', 2, 1, NOW()),
('1750000001903', '1750000000019', '紧急发布', 'E', '紧急发布版本', 3, 1, NOW());

-- ========================================
-- 11. 位置码字典 (dm_location_code) - DMC第6段
-- ========================================
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_time)
VALUES ('1750000000020', '位置码', 'dm_location_code', 'IETM位置码（DMC第6段）', 0, NOW());

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, description, sort_order, status, create_time) VALUES
('1750000002001', '1750000000020', 'A-基本位置', 'A', '基本位置', 1, 1, NOW()),
('1750000002002', '1750000000020', 'B-次要位置', 'B', '次要位置', 2, 1, NOW()),
('1750000002003', '1750000000020', 'C-补充位置', 'C', '补充位置', 3, 1, NOW()),
('1750000002004', '1750000000020', 'D-详细位置', 'D', '详细位置', 4, 1, NOW()),
('1750000002005', '1750000000020', 'T-培训位置', 'T', '培训用位置', 5, 1, NOW());

-- ========================================
-- 验证SQL
-- ========================================
-- 查询所有创建的字典
SELECT d.dict_code, d.dict_name, COUNT(di.id) AS item_count
FROM sys_dict d
LEFT JOIN sys_dict_item di ON d.id = di.dict_id
WHERE d.dict_code IN ('dm_type', 'dm_status', 'workflow_status', 'language', 'country',
                       'tech_name', 'info_name', 'variant_code', 'disassy_code', 'issue_type', 'dm_location_code')
GROUP BY d.dict_code, d.dict_name
ORDER BY d.create_time;
