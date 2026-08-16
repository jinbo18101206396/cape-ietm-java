package org.jeecg.modules.ietm.icnmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnReference;

/**
 * @Description: ICN引用关系Mapper
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 * @Version: V1.0
 */
public interface IetmIcnReferenceMapper extends BaseMapper<IetmIcnReference> {

    /**
     * 检查引用关系是否已存在
     * @param sourceIcnId 源ICN ID
     * @param targetIcnId 目标ICN ID或DM编码
     * @param referenceType 引用类型
     * @return 存在的记录数
     */
    int checkReferenceExists(@Param("sourceIcnId") String sourceIcnId,
                             @Param("targetIcnId") String targetIcnId,
                             @Param("referenceType") String referenceType);
}
