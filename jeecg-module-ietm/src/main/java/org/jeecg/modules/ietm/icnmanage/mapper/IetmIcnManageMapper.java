package org.jeecg.modules.ietm.icnmanage.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 项目管理-项目实体管理
 * @Author: jeecg-boot
 * @Date: 2026-07-19
 * @Version: V2.0
 */
public interface IetmIcnManageMapper extends BaseMapper<IetmIcnManage> {

    /**
     * 获取指定构型节点下的最大唯一识别码
     * @param cmnodeId 构型节点ID
     * @return 最大唯一识别码
     */
    String getMaxUniqueIdByCmnodeId(@Param("cmnodeId") String cmnodeId);

    /**
     * 批量查询ICN及其附件信息
     * @param cmnodeId 构型节点ID
     * @return ICN列表（含附件信息）
     */
    List<IetmIcnManage> listWithAttachments(@Param("cmnodeId") String cmnodeId);

    /**
     * 批量查询ICN及其附件信息（包含所有子节点）
     * @param cmnodeId 构型节点ID
     * @return ICN列表（含附件信息）
     */
    List<IetmIcnManage> listWithAttachmentsIncludeChildren(@Param("cmnodeId") String cmnodeId);
}
