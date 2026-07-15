package org.jeecg.modules.ietm.standardmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.ietm.standardmanagement.entity.IetmStandard;
import org.jeecg.modules.ietm.standardmanagement.mapper.IetmStandardDmtypeMapper;
import org.jeecg.modules.ietm.standardmanagement.mapper.IetmStandardMapper;
import org.jeecg.modules.ietm.standardmanagement.service.IIetmStandardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;

/**
 * @Description: 手册管理-标准管理左侧树
 * @Author: jeecg-boot
 * @Date: 2026-01-08
 * @Version: V1.0
 */
@Service
public class IetmStandardServiceImpl extends ServiceImpl<IetmStandardMapper, IetmStandard> implements IIetmStandardService {

    @Autowired
    private IetmStandardMapper ietmStandardMapper;
    @Autowired
    private IetmStandardDmtypeMapper ietmStandardDmtypeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delMain(String id) {
        ietmStandardDmtypeMapper.deleteByMainId(id);
        ietmStandardMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delBatchMain(Collection<? extends Serializable> idList) {
        for (Serializable id : idList) {
            ietmStandardDmtypeMapper.deleteByMainId(id.toString());
            ietmStandardMapper.deleteById(id);
        }
    }

}
