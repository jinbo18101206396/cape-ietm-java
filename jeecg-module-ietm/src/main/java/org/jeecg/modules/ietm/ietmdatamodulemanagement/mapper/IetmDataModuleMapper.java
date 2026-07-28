package org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;

import java.util.List;
import java.util.Map;

/**
 * @Description: 数据模块管理Mapper接口
 * @Author: jeecg-boot
 * @Date: 2026-07-22
 * @Version: V3.0
 */
public interface IetmDataModuleMapper extends BaseMapper<IetmDataModule> {

    /**
     * 根据项目ID查询数据模块列表
     */
    List<IetmDataModule> selectByProjectId(@Param("projectId") String projectId);

    /**
     * 根据构型节点ID查询数据模块列表
     * @param cmNodeId        构型节点ID
     * @param includeChildren 是否包含子节点（true-包含，false-不包含）
     */
    List<IetmDataModule> selectByCmNodeId(@Param("cmNodeId") String cmNodeId,
                                          @Param("includeChildren") Boolean includeChildren);

    /**
     * DMC唯一性校验查询（基于6个维度确保DMC唯一）
     * @param excludeId 排除的记录ID（更新时传入自身ID）
     */
    IetmDataModule selectByDmcForValidation(@Param("sns") String sns,
                                            @Param("infoCode") String infoCode,
                                            @Param("infoCodeVariant") String infoCodeVariant,
                                            @Param("ietmLocationCode") String ietmLocationCode,
                                            @Param("languageIsoCode") String languageIsoCode,
                                            @Param("countryIsoCode") String countryIsoCode,
                                            @Param("excludeId") String excludeId);

    /**
     * 查询同一DMC（SNS+infoCode+variant）的所有历史版本，按版本号倒序
     */
    List<IetmDataModule> selectHistoryVersions(@Param("sns") String sns,
                                               @Param("infoCode") String infoCode,
                                               @Param("infoCodeVariant") String infoCodeVariant);

    /**
     * 查询引用关系信息
     * @param refType 引用类型（out=出引用，in=入引用）
     */
    List<Map<String, Object>> selectReferenceInfo(@Param("dmId") String dmId,
                                                  @Param("refType") String refType);

    /**
     * 查询数据模块列表（JOIN流程视图，获取动态流程步骤）
     * @param page 分页参数
     * @param projectId 项目ID
     * @param cmNodeId 构型节点ID
     * @param cmNodePath 构型节点路径
     * @param includeChildren 是否包含子节点
     * @return 数据模块列表
     */
    IPage<IetmDataModule> selectPageWithFlow(Page<IetmDataModule> page,
                                              @Param("projectId") String projectId,
                                              @Param("cmNodeId") String cmNodeId,
                                              @Param("cmNodePath") String cmNodePath,
                                              @Param("includeChildren") Boolean includeChildren);
}
