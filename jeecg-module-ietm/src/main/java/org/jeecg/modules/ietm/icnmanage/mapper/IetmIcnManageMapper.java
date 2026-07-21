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

    /**
     * 根据ID查询ICN及其附件信息（用于预览）
     * @param id ICN ID
     * @return ICN对象（含附件信息）
     */
    IetmIcnManage getByIdWithAttachment(@Param("id") String id);

    /**
     * 查询ICN引用的其他ICN列表（正向引用）
     * @param icnId ICN ID
     * @return 被引用的ICN列表
     */
    List<IetmIcnManage> getReferencedIcnList(@Param("icnId") String icnId);

    /**
     * 查询引用当前ICN的其他ICN列表（反向引用）
     * @param icnId ICN ID
     * @return 引用方ICN列表
     */
    List<IetmIcnManage> getReferencingIcnList(@Param("icnId") String icnId);

    /**
     * 查询引用当前ICN的DM模块列表
     * @param icnId ICN ID
     * @return DM引用列表（返回Map包含dm_code, dm_title等）
     */
    List<java.util.Map<String, Object>> getReferencedByDmList(@Param("icnId") String icnId);

    /**
     * 批量查询ICN及其附件（用于下载）
     * @param ids ICN ID列表
     * @param includeRelated 是否包含相关文件
     * @return ICN列表（含附件信息）
     */
    List<IetmIcnManage> listByIdsWithAttachments(@Param("ids") List<String> ids,
                                                  @Param("includeRelated") String includeRelated);
}
