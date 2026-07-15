package org.jeecg.modules.ietm.icnmanage.service.impl;

import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 项目管理-项目实体管理
 * @Author: jeecg-boot
 * @Date:   2026-02-06
 * @Version: V1.0
 */
@Service
@Primary
public class IetmIcnManageServiceImpl extends ServiceImpl<IetmIcnManageMapper, IetmIcnManage> implements IIetmIcnManageService {

}
