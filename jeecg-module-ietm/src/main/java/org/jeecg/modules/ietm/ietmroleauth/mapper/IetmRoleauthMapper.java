package org.jeecg.modules.ietm.ietmroleauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.ietmroleauth.entity.IetmRoleauth;
import org.jeecg.modules.ietm.ietmroleauth.vo.IetmRoleauthVO;

import java.util.List;

/**
 * @Description: 手册授权管理
 * @Author: jeecg-boot
 * @Date: 2026-07-15
 * @Version: V1.0
 */
public interface IetmRoleauthMapper extends BaseMapper<IetmRoleauth> {

    /**
     * 查询授权列表（带角色名称、项目名称）
     * @param objType 对象类型
     * @param objId 对象ID
     * @return
     */
    List<IetmRoleauthVO> selectRoleauthWithNames(@Param("objType") String objType, @Param("objId") String objId);

    /**
     * 查询指定项目的所有授权
     * @param projectId 项目ID
     * @param objType 对象类型
     * @return
     */
    List<IetmRoleauthVO> selectByProjectId(@Param("projectId") String projectId, @Param("objType") String objType);
}
