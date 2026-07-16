package org.jeecg.modules.ietm.ietmroleauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.modules.ietm.ietmroleauth.entity.IetmAuthConfig;
import org.jeecg.modules.ietm.ietmroleauth.entity.IetmRoleauth;
import org.jeecg.modules.ietm.ietmroleauth.mapper.IetmAuthConfigMapper;
import org.jeecg.modules.ietm.ietmroleauth.mapper.IetmRoleauthMapper;
import org.jeecg.modules.ietm.ietmroleauth.service.IIetmAuthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 手册授权校验服务实现
 * @Author: jeecg-boot
 * @Date: 2026-07-15
 * @Version: V1.0
 */
@Service
public class IetmAuthCheckServiceImpl implements IIetmAuthCheckService {

    @Autowired
    private IetmAuthConfigMapper authConfigMapper;

    @Autowired
    private IetmRoleauthMapper roleauthMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取当前授权类型
     */
    @Override
    public String getAuthType() {
        QueryWrapper<IetmAuthConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_key", "authtype");
        IetmAuthConfig config = authConfigMapper.selectOne(queryWrapper);
        return config != null ? config.getConfigValue() : "0";
    }

    /**
     * 检查用户是否有项目浏览权限
     */
    @Override
    public boolean hasProjectReadAuth(String userId, String projectId) {
        String authType = getAuthType();

        // 授权类型为"不限制"
        if ("0".equals(authType)) {
            return true;
        }

        // 授权类型为"项目按角色授权"
        if ("1".equals(authType)) {
            List<String> roleIds = getUserRoleIds(userId);
            if (roleIds == null || roleIds.isEmpty()) {
                return false;
            }

            QueryWrapper<IetmRoleauth> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("role_id", roleIds);
            queryWrapper.eq("obj_type", "1");
            queryWrapper.eq("obj_id", projectId);
            queryWrapper.eq("can_read", "Y");

            return roleauthMapper.selectCount(queryWrapper) > 0;
        }

        // 授权类型为"构型按角色授权"，项目级别不限制
        return true;
    }

    /**
     * 检查用户是否有项目编辑权限
     */
    @Override
    public boolean hasProjectEditAuth(String userId, String projectId) {
        String authType = getAuthType();

        // 授权类型为"不限制"
        if ("0".equals(authType)) {
            return true;
        }

        // 授权类型为"项目按角色授权"
        if ("1".equals(authType)) {
            List<String> roleIds = getUserRoleIds(userId);
            if (roleIds == null || roleIds.isEmpty()) {
                return false;
            }

            QueryWrapper<IetmRoleauth> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("role_id", roleIds);
            queryWrapper.eq("obj_type", "1");
            queryWrapper.eq("obj_id", projectId);
            queryWrapper.eq("can_edit", "Y");

            return roleauthMapper.selectCount(queryWrapper) > 0;
        }

        // 授权类型为"构型按角色授权"，项目级别不限制
        return true;
    }

    /**
     * 检查用户是否有构型浏览权限
     */
    @Override
    public boolean hasCmReadAuth(String userId, String objId) {
        String authType = getAuthType();

        // 授权类型为"不限制"或"项目按角色授权"
        if ("0".equals(authType) || "1".equals(authType)) {
            return true;
        }

        // 授权类型为"构型按角色授权"
        if ("2".equals(authType)) {
            List<String> roleIds = getUserRoleIds(userId);
            if (roleIds == null || roleIds.isEmpty()) {
                return false;
            }

            QueryWrapper<IetmRoleauth> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("role_id", roleIds);
            queryWrapper.eq("obj_type", "2");
            queryWrapper.eq("obj_id", objId);
            queryWrapper.eq("can_read", "Y");

            return roleauthMapper.selectCount(queryWrapper) > 0;
        }

        return false;
    }

    /**
     * 检查用户是否有构型编辑权限
     */
    @Override
    public boolean hasCmEditAuth(String userId, String objId) {
        String authType = getAuthType();

        // 授权类型为"不限制"或"项目按角色授权"
        if ("0".equals(authType) || "1".equals(authType)) {
            return true;
        }

        // 授权类型为"构型按角色授权"
        if ("2".equals(authType)) {
            List<String> roleIds = getUserRoleIds(userId);
            if (roleIds == null || roleIds.isEmpty()) {
                return false;
            }

            QueryWrapper<IetmRoleauth> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("role_id", roleIds);
            queryWrapper.eq("obj_type", "2");
            queryWrapper.eq("obj_id", objId);
            queryWrapper.eq("can_edit", "Y");

            return roleauthMapper.selectCount(queryWrapper) > 0;
        }

        return false;
    }

    /**
     * 获取用户有权限的项目ID列表
     */
    @Override
    public List<String> getUserAuthorizedProjectIds(String userId) {
        String authType = getAuthType();

        // 授权类型为"不限制"
        if ("0".equals(authType)) {
            return null; // null表示不限制，由调用方处理
        }

        // 授权类型为"项目按角色授权"
        if ("1".equals(authType)) {
            List<String> roleIds = getUserRoleIds(userId);
            if (roleIds == null || roleIds.isEmpty()) {
                return new ArrayList<>();
            }

            QueryWrapper<IetmRoleauth> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("DISTINCT obj_id");
            queryWrapper.in("role_id", roleIds);
            queryWrapper.eq("obj_type", "1");
            queryWrapper.eq("can_read", "Y");

            List<IetmRoleauth> list = roleauthMapper.selectList(queryWrapper);
            List<String> projectIds = new ArrayList<>();
            for (IetmRoleauth auth : list) {
                projectIds.add(auth.getObjId());
            }
            return projectIds;
        }

        // 授权类型为"构型按角色授权"，项目级别不限制
        return null;
    }

    /**
     * 获取用户有权限的构型ID列表
     */
    @Override
    public List<String> getUserAuthorizedCmIds(String userId, String projectId) {
        String authType = getAuthType();

        // 授权类型为"不限制"或"项目按角色授权"
        if ("0".equals(authType) || "1".equals(authType)) {
            return null; // null表示不限制
        }

        // 授权类型为"构型按角色授权"
        if ("2".equals(authType)) {
            List<String> roleIds = getUserRoleIds(userId);
            if (roleIds == null || roleIds.isEmpty()) {
                return new ArrayList<>();
            }

            QueryWrapper<IetmRoleauth> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("DISTINCT obj_id");
            queryWrapper.in("role_id", roleIds);
            queryWrapper.eq("obj_type", "2");
            queryWrapper.eq("can_read", "Y");

            if (projectId != null && !projectId.isEmpty()) {
                queryWrapper.eq("project_id", projectId);
            }

            List<IetmRoleauth> list = roleauthMapper.selectList(queryWrapper);
            List<String> cmIds = new ArrayList<>();
            for (IetmRoleauth auth : list) {
                cmIds.add(auth.getObjId());
            }
            return cmIds;
        }

        return null;
    }

    /**
     * 获取用户的角色ID列表
     * 使用JdbcTemplate直接查询，避免循环依赖
     */
    private List<String> getUserRoleIds(String userId) {
        String sql = "SELECT role_id FROM sys_user_role WHERE user_id = ?";
        return jdbcTemplate.queryForList(sql, String.class, userId);
    }
}
