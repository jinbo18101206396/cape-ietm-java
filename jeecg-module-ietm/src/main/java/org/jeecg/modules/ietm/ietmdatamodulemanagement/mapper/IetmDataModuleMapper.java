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
     * 查询同一DMC的所有历史版本（轻量列，不含dm_content），按版本号倒序
     * @param projectId 项目ID（可选）
     * @param sns SNS编号
     * @param infoCode 信息代码
     * @param infoCodeVariant 信息代码变体（可为空）
     * @param ietmLocationCode 位置代码（可选）
     * @param onlyPublished true=仅发布版本(version_type='1')；false/null=全部有效版本
     */
    List<IetmDataModule> selectHistoryVersions(@Param("projectId") String projectId,
                                               @Param("sns") String sns,
                                               @Param("infoCode") String infoCode,
                                               @Param("infoCodeVariant") String infoCodeVariant,
                                               @Param("ietmLocationCode") String ietmLocationCode,
                                               @Param("onlyPublished") Boolean onlyPublished);

    /**
     * 按ID单取XML内容（对比/查看时按需加载）
     */
    IetmDataModule selectContentById(@Param("id") String id);

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

    /**
     * 查询单条数据模块（JOIN流程视图，获取动态流程步骤 workflowStep / 待办人 workflowHandler）
     * <p>用于签出等需实时流程节点判断的场景（方案A：workflow_step 不回写基表，从 v_wf_instance 视图动态取）。</p>
     * @param id 数据模块ID
     * @return 含视图字段的数据模块实体，不存在返回 null
     */
    IetmDataModule selectByIdWithFlow(@Param("id") String id);

    /**
     * 引用DM弹窗-分页查询DM列表（§14.5）
     * @param page            分页参数
     * @param cmNodeId        构型节点ID
     * @param cmNodePath      构型节点路径（含子节点查询时用于 LIKE 匹配）
     * @param includeChildren 是否包含子节点
     * @param onlyIssued      true=仅最新发行版（引用指定版本页签）false=最新版（引用最新版页签）
     * @param dmc             DMC模糊查询
     * @param techName        技术名称模糊查询
     * @param infoName        信息名称模糊查询
     * @param dmTypeName      DM类型模糊查询
     */
    IPage<IetmDataModule> selectPageForDialog(Page<IetmDataModule> page,
                                              @Param("cmNodeId") String cmNodeId,
                                              @Param("cmNodePath") String cmNodePath,
                                              @Param("includeChildren") Boolean includeChildren,
                                              @Param("onlyIssued") Boolean onlyIssued,
                                              @Param("dmc") String dmc,
                                              @Param("techName") String techName,
                                              @Param("infoName") String infoName,
                                              @Param("dmTypeName") String dmTypeName);

    /**
     * 根据ID查询dmcontent字段（列表页预览专用）
     * 独立查询避免大字段随列表加载，复用编辑器预览的查询模式
     * @param id DM主键ID
     * @return XML内容
     */
    String getDmcontentById(@Param("id") String id);
}
