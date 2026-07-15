package org.jeecg.modules.ietm.standardinformationcode.service.impl;

import org.jeecg.modules.ietm.standardinformationcode.entity.IetmStandardInformationCode;
import org.jeecg.modules.ietm.standardinformationcode.mapper.IetmStandardInformationCodeMapper;
import org.jeecg.modules.ietm.standardinformationcode.service.IIetmStandardInformationCodeService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 预制模板-信息码管理
 * @Author: jeecg-boot
 * @Date:   2026-01-12
 * @Version: V1.0
 */
@Service
@Primary
public class IetmStandardInformationCodeServiceImpl extends ServiceImpl<IetmStandardInformationCodeMapper, IetmStandardInformationCode> implements IIetmStandardInformationCodeService {

}
