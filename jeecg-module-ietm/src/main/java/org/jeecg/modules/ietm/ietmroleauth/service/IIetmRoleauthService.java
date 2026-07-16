package org.jeecg.modules.ietm.ietmroleauth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.ietmroleauth.entity.IetmRoleauth;
import org.jeecg.modules.ietm.ietmroleauth.vo.IetmRoleauthVO;

import java.util.List;

/**
 * @Description: 手册授权管理
 * @Author: jeecg-boot
 * @Date: 2026-07-15
 * @Version: V1.0
 */
public interface IIetmRoleauthService extends IService<IetmRoleauth> {

    /**
     * 查询授权列表（带关联信息）
     * @param objType 对象类型
     * @param objId 对象ID
     * @return
     */
    List<IetmRoleauthVO> getRoleauthWithNames(String objType, String objId);

    /**
     * 查询指定项目的所有授权
     * @param projectId 项目ID
     * @param objType 对象类型
     * @return
     */
    List<IetmRoleauthVO> getByProjectId(String projectId, String objType);

    /**
     * 批量保存授权（覆盖模式）
     * @param roleauthList 授权列表
     * @return
     */
    boolean batchSaveOrUpdateWithOverride(List<IetmRoleauth> roleauthList);

    /**
     * 批量保存授权
     * @param roleauthList 授权列表
     * @return
     */
    boolean batchSaveOrUpdate(List<IetmRoleauth> roleauthList);

    /**
     * 检查角色授权是否重复
     * @param roleId 角色ID
     * @param objType 对象类型
     * @param objId 对象ID
     * @param excludeId 排除的ID
     * @return
     */
    boolean checkDuplicate(String roleId, String objType, String objId, String excludeId);
}
