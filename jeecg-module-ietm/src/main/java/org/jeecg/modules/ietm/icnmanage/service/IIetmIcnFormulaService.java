package org.jeecg.modules.ietm.icnmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula;

/**
 * IETM公式ICN Service
 *
 * @author IETM Team
 * @date 2026-10-14
 */
public interface IIetmIcnFormulaService extends IService<IetmIcnFormula> {

    /**
     * 根据ICN编码查询公式内容（含CLOB字段）
     *
     * @param icn ICN完整编码
     * @return 公式实体（含filecontent CLOB字段）
     */
    IetmIcnFormula getByIcnWithContent(String icn);
}
