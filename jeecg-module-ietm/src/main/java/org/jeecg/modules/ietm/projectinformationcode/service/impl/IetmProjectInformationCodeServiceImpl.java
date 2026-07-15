package org.jeecg.modules.ietm.projectinformationcode.service.impl;

import org.jeecg.modules.ietm.projectinformationcode.entity.IetmProjectInformationCode;
import org.jeecg.modules.ietm.projectinformationcode.mapper.IetmProjectInformationCodeMapper;
import org.jeecg.modules.ietm.projectinformationcode.service.IIetmProjectInformationCodeService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 项目管理-项目信息码管理
 * @Author: jeecg-boot
 * @Date:   2026-01-12
 * @Version: V1.0
 */
@Service
@Primary
public class IetmProjectInformationCodeServiceImpl extends ServiceImpl<IetmProjectInformationCodeMapper, IetmProjectInformationCode> implements IIetmProjectInformationCodeService {

}
