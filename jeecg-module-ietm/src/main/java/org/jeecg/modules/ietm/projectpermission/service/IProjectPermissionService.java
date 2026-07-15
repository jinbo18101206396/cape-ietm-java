package org.jeecg.modules.ietm.projectpermission.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProjectParams;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.projectpermission.entity.ProjectPermission;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Description: 手册管理-手册项目管理列表
 * @Author: jeecg-boot
 * @Date:   2026-01-09
 * @Version: V1.0
 */
public interface IProjectPermissionService extends IService<ProjectPermission> {


	IPage<LoginUser> listUserByTargetId(Page<LoginUser> page, Map<String, Object> paramMap);

	String addUserByTargetId(String permissionType, String targetId, String userIds);
}
