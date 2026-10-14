package org.jeecg.modules.ietm.icnmanage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnFormulaMapper;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnFormulaService;
import org.springframework.stereotype.Service;

/**
 * IETM公式ICN Service实现
 *
 * @author IETM Team
 * @date 2026-10-14
 */
@Service
public class IetmIcnFormulaServiceImpl extends ServiceImpl<IetmIcnFormulaMapper, IetmIcnFormula> implements IIetmIcnFormulaService {

    @Override
    public IetmIcnFormula getByIcnWithContent(String icn) {
        return baseMapper.getByIcnWithContent(icn);
    }
}
