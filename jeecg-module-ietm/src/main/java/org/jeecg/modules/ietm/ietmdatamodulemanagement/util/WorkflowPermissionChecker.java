package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.stereotype.Component;

/**
 * 工作流权限校验工具类
 *
 * @author Kiro AI
 * @date 2026-08-23
 */
@Slf4j
@Component
public class WorkflowPermissionChecker {

    /**
     * 检查用户是否有流程节点的执行权限
     *
     * @param workflowHandler 流程执行人列表（逗号分隔），格式：
     *                        - 用户UUID: "e9ca23d68d884d4ebb19d07889727dae"
     *                        - 用户名: "admin,zhangsan"
     *                        - 真实姓名: "管理员,张三"
     *                        - 角色/部门等（待实现）: "rol_admin,dpt_tech"
     * @param username 当前用户名
     * @return true=有权限，false=无权限
     */
    public boolean hasPermission(String workflowHandler, String username) {
        // 如果没有指定执行人，则不限制权限
        if (oConvertUtils.isEmpty(workflowHandler)) {
            log.debug("[权限校验] 无执行人限制，允许访问");
            return true;
        }

        // 获取当前登录用户完整信息
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser == null) {
            log.warn("[权限校验] 无法获取当前登录用户信息");
            return false;
        }

        String userId = loginUser.getId();
        String realname = loginUser.getRealname();

        log.debug("[权限校验] 当前用户：id={}, username={}, realname={}, 执行人列表：{}",
                userId, username, realname, workflowHandler);

        // 解析执行人列表
        String[] handlers = workflowHandler.split(",");
        for (String handler : handlers) {
            handler = handler.trim();

            // 空字符串跳过
            if (handler.isEmpty()) {
                continue;
            }

            // 优先级1：匹配用户UUID（sys_user.id）
            // v_wf_instance.currenthandler_ 存储的通常是用户UUID
            if (oConvertUtils.isNotEmpty(userId) && userId.equals(handler)) {
                log.debug("[权限校验] 匹配成功：用户UUID匹配 ({})", userId);
                return true;
            }

            // 优先级2：匹配用户名（username）
            if (username.equals(handler)) {
                log.debug("[权限校验] 匹配成功：用户名匹配 ({})", username);
                return true;
            }

            // 优先级3：匹配用户真实姓名（realname）
            if (oConvertUtils.isNotEmpty(realname) && realname.equals(handler)) {
                log.debug("[权限校验] 匹配成功：真实姓名匹配 ({})", realname);
                return true;
            }

            // 优先级4：角色/部门/岗位/工作组权限（扩展功能）
            if (handler.startsWith("rol_") || handler.startsWith("dpt_") ||
                handler.startsWith("pst_") || handler.startsWith("grp_")) {
                log.debug("[权限校验] 检测到角色/部门前缀，但尚未实现扩展权限匹配：{}", handler);
                // TODO: 实现角色/部门/岗位/工作组权限匹配
                // 1. 获取当前用户的角色/部门/岗位/工作组列表
                // 2. 解析handler前缀（rol_=角色，dpt_=部门，pst_=岗位，grp_=工作组）
                // 3. 检查用户是否属于对应的角色/部门等
            }
        }

        log.warn("[权限校验] 权限验证失败：username={}, realname={}, handlers={}",
                username, realname, workflowHandler);
        return false;
    }

    /**
     * 检查用户是否有权限，如果没有则抛出异常
     *
     * @param workflowHandler 流程执行人列表
     * @param username 当前用户名
     * @param operationName 操作名称（用于错误提示）
     * @throws org.jeecg.common.exception.JeecgBootException 如果没有权限
     */
    public void checkPermissionOrThrow(String workflowHandler, String username, String operationName) {
        if (!hasPermission(workflowHandler, username)) {
            throw new org.jeecg.common.exception.JeecgBootException(
                    String.format("您没有权限%s此DM，只能由流程指定的执行人操作（执行人：%s）",
                            operationName, workflowHandler)
            );
        }
    }
}
