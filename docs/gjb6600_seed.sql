-- ============================================================
-- GJB6600 dm_type 种子数据
-- 生成依据：逐一读取 classpath:ietm/GJB6600/schema/*.xsd，
-- 从每个 <xs:element name="内容"> 定义中提取第一子元素名
-- 作为 content_root_elem。
-- 执行前提：ietm_dm_type 已有 id 1-8（S1000D4.0 行），不冲突。
-- template_file 只有 descript.xml 部署；其余类型新建 DM 时
-- 后端会回退到 DmXmlHelper.minimalSkeleton(standard)。
-- ============================================================

-- 执行前先确认当前 id 范围：
-- SELECT id, type_code, ietm_standard FROM ietm_dm_type ORDER BY id;

INSERT INTO ietm_dm_type
    (id, type_code, type_name, type_name_en, ietm_standard,
     xsd_file, content_root_elem, template_file, sort_no, status, create_by, create_time)
VALUES
    ('11', 'description',          '描述类',       'Description',          'GJB6600',
     'descript.xsd', '描述性信息', 'descript.xml', 10,  '1', 'admin', SYSDATE),
    ('12', 'procedure',            '程序类',       'Procedure',            'GJB6600',
     'proced.xsd',   '操作程序',   NULL,           20,  '1', 'admin', SYSDATE),
    ('13', 'faultIsolation',       '故障隔离类',   'Fault Isolation',      'GJB6600',
     'fault.xsd',    '故障隔离信息', NULL,          30,  '1', 'admin', SYSDATE),
    ('14', 'faultReporting',       '故障报告类',   'Fault Reporting',      'GJB6600',
     'fault.xsd',    '故障报告信息', NULL,          40,  '1', 'admin', SYSDATE),
    ('15', 'illustratedPartsCatalog', '图解零件目录类', 'IPD',             'GJB6600',
     'ipd.xsd',      '图解零件目录', NULL,          50,  '1', 'admin', SYSDATE),
    ('16', 'process',              '过程类',       'Process',              'GJB6600',
     'process.xsd',  '过程数据模块', NULL,          60,  '1', 'admin', SYSDATE),
    ('17', 'maintPlanning',        '维修计划类',   'Maintenance Planning', 'GJB6600',
     'schedul.xsd',  '计划',       NULL,           70,  '1', 'admin', SYSDATE),
    ('18', 'crew',                 '操作类',       'Crew',                 'GJB6600',
     'crew.xsd',     '操作类信息', NULL,           80,  '1', 'admin', SYSDATE);

COMMIT;

-- ============================================================
-- 清除脏数据 DM（S1000D 正文误存入 GJB6600 项目）
-- DM id: 2083556266365288450，正文根 <dmodule>，所属项目 ietm_standard=GJB6600
-- 置空后下次打开将自动回填 GJB6600 descript.xml 模板
-- ============================================================
UPDATE ietm_data_module
   SET dm_content = NULL,
       version    = 0,
       update_time = SYSDATE
 WHERE id = '2083556266365288450';

COMMIT;
