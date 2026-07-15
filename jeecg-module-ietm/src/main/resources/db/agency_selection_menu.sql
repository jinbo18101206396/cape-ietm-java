-- =============================================
-- 机构遴选模块菜单权限 SQL（DM8 兼容）
-- 日期：2026-04-29
-- 注意：需要通过 disql 或 DM 管理工具手动执行
-- =============================================

-- 主菜单ID使用时间戳: 1777465484179

-- ----------------------------
-- 1. 主菜单
-- ----------------------------
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
VALUES ('177746548417901', NULL, '机构遴选', '/agencySelection', 'ietm/agencySelection/AgencySelection', NULL, NULL, 0, NULL, '1', 1.00, 0, 'team', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0);

-- ----------------------------
-- 2. 机构管理-按钮权限
-- ----------------------------
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417902', '177746548417901', '机构-新增', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:agency:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417903', '177746548417901', '机构-编辑', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:agency:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417904', '177746548417901', '机构-删除', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:agency:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417905', '177746548417901', '机构-批量删除', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:agency:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417906', '177746548417901', '机构-导出', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:agency:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417907', '177746548417901', '机构-导入', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:agency:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

-- ----------------------------
-- 3. 项目管理-按钮权限
-- ----------------------------
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417908', '177746548417901', '项目-新增', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:project:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417909', '177746548417901', '项目-编辑', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:project:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417910', '177746548417901', '项目-删除', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:project:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417911', '177746548417901', '项目-批量删除', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:project:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417912', '177746548417901', '项目-导出', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:project:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417913', '177746548417901', '项目-导入', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:project:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

-- ----------------------------
-- 4. 自定义操作权限
-- ----------------------------
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417914', '177746548417901', '重置预设比例', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:resetRatios', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('177746548417915', '177746548417901', '执行遴选', NULL, NULL, 0, NULL, NULL, 2, 'agencySelection:executeSelection', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-04-29 00:00:00', NULL, NULL, 0, 0, '1', 0);

-- ----------------------------
-- 5. admin角色授权
-- ----------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417916', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417901', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417917', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417902', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417918', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417903', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417919', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417904', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417920', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417905', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417921', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417906', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417922', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417907', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417923', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417908', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417924', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417909', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417925', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417910', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417926', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417911', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417927', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417912', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417928', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417913', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417929', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417914', NULL, '2026-04-29 00:00:00', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('177746548417930', 'f6817f48af4fb3af11b9e8bf182f618b', '177746548417915', NULL, '2026-04-29 00:00:00', '127.0.0.1');
