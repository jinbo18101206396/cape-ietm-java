package org.jeecg.modules.ietm.ietmroleauth.service;

import java.util.List;

/**
 * @Description: 手册授权校验服务
 * @Author: jeecg-boot
 * @Date: 2026-07-15
 * @Version: V1.0
 */
public interface IIetmAuthCheckService {

    /**
     * 获取当前授权类型
     * @return 0=不限制, 1=项目按角色授权, 2=构型按角色授权
     */
    String getAuthType();

    /**
     * 检查用户是否有项目浏览权限
     * @param userId 用户ID
     * @param projectId 项目ID
     * @return true=有权限, false=无权限
     */
    boolean hasProjectReadAuth(String userId, String projectId);

    /**
     * 检查用户是否有项目编辑权限
     * @param userId 用户ID
     * @param projectId 项目ID
     * @return true=有权限, false=无权限
     */
    boolean hasProjectEditAuth(String userId, String projectId);

    /**
     * 检查用户是否有构型浏览权限
     * @param userId 用户ID
     * @param objId 构型对象ID
     * @return true=有权限, false=无权限
     */
    boolean hasCmReadAuth(String userId, String objId);

    /**
     * 检查用户是否有构型编辑权限
     * @param userId 用户ID
     * @param objId 构型对象ID
     * @return true=有权限, false=无权限
     */
    boolean hasCmEditAuth(String userId, String objId);

    /**
     * 获取用户有权限的项目ID列表
     * @param userId 用户ID
     * @return 项目ID列表
     */
    List<String> getUserAuthorizedProjectIds(String userId);

    /**
     * 获取用户有权限的构型ID列表
     * @param userId 用户ID
     * @param projectId 项目ID（可选，用于过滤特定项目下的构型）
     * @return 构型ID列表
     */
    List<String> getUserAuthorizedCmIds(String userId, String projectId);
}
