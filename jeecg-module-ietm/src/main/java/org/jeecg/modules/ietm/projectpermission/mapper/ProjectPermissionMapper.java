package org.jeecg.modules.ietm.projectpermission.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.projectpermission.entity.ProjectPermission;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @Description: 手册管理-手册项目管理列表
 * @Author: jeecg-boot
 * @Date:   2026-01-09
 * @Version: V1.0
 */
@Resource
public interface ProjectPermissionMapper extends BaseMapper<ProjectPermission> {

    IPage<LoginUser> getUserPage(IPage<LoginUser> page, @Param("param") Map<String, Object> paramMap);
}
