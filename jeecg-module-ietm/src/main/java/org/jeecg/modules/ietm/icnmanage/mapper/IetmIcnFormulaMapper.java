package org.jeecg.modules.ietm.icnmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula;

/**
 * IETM公式ICN Mapper
 *
 * @author IETM Team
 * @date 2026-10-14
 */
public interface IetmIcnFormulaMapper extends BaseMapper<IetmIcnFormula> {

    /**
     * 根据ICN编码查询公式内容（含CLOB字段）
     *
     * 注意：MyBatis-Plus默认的selectOne()不会查询CLOB字段，
     * 必须显式指定查询filecontent字段才能获取公式HTML内容
     *
     * @param icn ICN完整编码
     * @return 公式实体（含filecontent CLOB字段）
     */
    @Select("SELECT id, icn, sns, rpc, originator, unique_id, variant_code, " +
            "security_classification, issue_no, security, cm_node_id, " +
            "filecontent, icn_type, version, ispublished, isdeleted, " +
            "create_by, create_time, update_by, update_time, sys_org_code " +
            "FROM ietm_icn_formula " +
            "WHERE icn = #{icn} AND isdeleted = '0' " +
            "ORDER BY create_time DESC " +
            "FETCH FIRST 1 ROWS ONLY")
    IetmIcnFormula getByIcnWithContent(@Param("icn") String icn);
}
