package org.jeecg.modules.ietm.projectpermission.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.projectpermission.entity.ProjectPermission;
import org.jeecg.modules.ietm.projectpermission.mapper.ProjectPermissionMapper;
import org.jeecg.modules.ietm.projectpermission.service.IProjectPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @Description: 手册管理-手册项目管理列表
 * @Author: jeecg-boot
 * @Date: 2026-01-09
 * @Version: V1.0
 */
@Service
public class ProjectPermissionServiceImpl extends ServiceImpl<ProjectPermissionMapper, ProjectPermission> implements IProjectPermissionService {

    @Autowired
    private ProjectPermissionMapper projectPermissionMapper;


    @Override
    public IPage<LoginUser> listUserByTargetId(Page<LoginUser> page, Map<String, Object> paramMap) {
        return projectPermissionMapper.getUserPage(page, paramMap);
    }

    @Override
    public String addUserByTargetId(String permissionType, String targetId, String userIds) {
        if(StringUtils.isBlank(permissionType) || StringUtils.isBlank(targetId) || StringUtils.isBlank(userIds)){
            return String.format("参数不正确，请重试！[permissionType]:%s  |  [targetId]:%s  |  [userIds]:%s", permissionType, targetId, userIds);
        }
        List<ProjectPermission> permissionList = new ArrayList<>();
        String[] userIdsArray = userIds.split(",");

        for(String userId : userIdsArray){
            ProjectPermission obj = new ProjectPermission();
            obj.setPermissionType(Integer.parseInt(permissionType)) ;
            obj.setTargetId(targetId);
            obj.setUserId(userId);
            permissionList.add(obj);
        }
        this.saveBatch(permissionList);

        return null;
    }
}
