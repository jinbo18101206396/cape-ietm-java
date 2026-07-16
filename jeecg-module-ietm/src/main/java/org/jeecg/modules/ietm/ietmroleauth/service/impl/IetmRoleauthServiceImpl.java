package org.jeecg.modules.ietm.ietmroleauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.ietm.ietmroleauth.entity.IetmRoleauth;
import org.jeecg.modules.ietm.ietmroleauth.mapper.IetmRoleauthMapper;
import org.jeecg.modules.ietm.ietmroleauth.service.IIetmRoleauthService;
import org.jeecg.modules.ietm.ietmroleauth.vo.IetmRoleauthVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Description: 手册授权管理
 * @Author: jeecg-boot
 * @Date: 2026-07-15
 * @Version: V1.0
 */
@Service
public class IetmRoleauthServiceImpl extends ServiceImpl<IetmRoleauthMapper, IetmRoleauth> implements IIetmRoleauthService {

    @Override
    public List<IetmRoleauthVO> getRoleauthWithNames(String objType, String objId) {
        return baseMapper.selectRoleauthWithNames(objType, objId);
    }

    @Override
    public List<IetmRoleauthVO> getByProjectId(String projectId, String objType) {
        return baseMapper.selectByProjectId(projectId, objType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveOrUpdateWithOverride(List<IetmRoleauth> roleauthList) {
        for (IetmRoleauth roleauth : roleauthList) {
            // 查找是否存在相同角色、对象类型、对象ID的记录
            LambdaQueryWrapper<IetmRoleauth> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(IetmRoleauth::getRoleId, roleauth.getRoleId());
            queryWrapper.eq(IetmRoleauth::getObjType, roleauth.getObjType());
            queryWrapper.eq(IetmRoleauth::getObjId, roleauth.getObjId());

            IetmRoleauth existingRecord = this.getOne(queryWrapper);
            if (existingRecord != null) {
                // 存在则更新（覆盖）
                roleauth.setId(existingRecord.getId());
                this.updateById(roleauth);
            } else {
                // 不存在则新增
                this.save(roleauth);
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveOrUpdate(List<IetmRoleauth> roleauthList) {
        return this.saveOrUpdateBatch(roleauthList);
    }

    @Override
    public boolean checkDuplicate(String roleId, String objType, String objId, String excludeId) {
        LambdaQueryWrapper<IetmRoleauth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IetmRoleauth::getRoleId, roleId);
        queryWrapper.eq(IetmRoleauth::getObjType, objType);
        queryWrapper.eq(IetmRoleauth::getObjId, objId);
        if (oConvertUtils.isNotEmpty(excludeId)) {
            queryWrapper.ne(IetmRoleauth::getId, excludeId);
        }
        return this.count(queryWrapper) > 0;
    }
}
