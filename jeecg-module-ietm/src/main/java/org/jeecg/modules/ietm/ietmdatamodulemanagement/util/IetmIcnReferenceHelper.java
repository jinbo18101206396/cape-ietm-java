package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnReference;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnReferenceMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.constants.IetmDataModuleConstants;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefExtractItemVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ICN引用关系同步工具类
 * <p>
 * 提供统一的ICN引用同步逻辑，避免代码重复
 * 用于DM保存和计算引用两个场景
 * </p>
 *
 * @author IETM Team
 * @since 2026-08-31
 */
@Slf4j
public class IetmIcnReferenceHelper {

    /**
     * 同步ICN引用关系到 ietm_icn_reference 表（幂等操作）
     * <p>
     * 核心逻辑：
     * 1. 从XML中提取 graphic/multimedia/symbol 标签的 infoEntityIdent 属性
     * 2. 查询这些ICN是否存在于 ietm_icn_manage 表
     * 3. 批量查询已存在的引用关系（避免N+1查询）
     * 4. 为不存在引用关系的ICN创建新记录（处理并发冲突）
     * </p>
     *
     * @param dmId              DM的ID（主键，非DMC编码）
     * @param xmlContent        DM的XML内容
     * @param username          操作用户名
     * @param remark            备注信息（用于区分调用来源，如"DM保存时自动创建"或"计算引用时自动创建"）
     * @param icnManageMapper   ICN管理表Mapper
     * @param icnReferenceMapper ICN引用表Mapper
     * @throws Exception XML解析失败或数据库操作失败时抛出
     */
    public static void syncIcnReferences(
            String dmId,
            String xmlContent,
            String username,
            String remark,
            IetmIcnManageMapper icnManageMapper,
            IetmIcnReferenceMapper icnReferenceMapper
    ) throws Exception {
        if (!StringUtils.hasText(xmlContent)) {
            log.debug("syncIcnReferences: XML内容为空，跳过 dmId={}", dmId);
            return;
        }

        // 1. 使用 DmXmlHelper 提取所有引用（包括 graphic、multimedia 和 symbol）
        List<DmRefExtractItemVO> extracted;
        try {
            extracted = DmXmlHelper.extractReferencesFromXml(xmlContent);
        } catch (Exception e) {
            log.error("syncIcnReferences: XML解析失败 dmId={} error={}", dmId, e.getMessage(), e);
            throw new JeecgBootException("XML解析失败，无法同步ICN引用：" + e.getMessage());
        }

        if (extracted == null || extracted.isEmpty()) {
            log.debug("syncIcnReferences: 未提取到任何引用 dmId={}", dmId);
            return;
        }

        // 2. 过滤出 graphic、multimedia 和 symbol 类型的引用（使用Set自动去重）
        Set<String> icnCodeSet = new HashSet<>();
        for (DmRefExtractItemVO item : extracted) {
            String refType = item.getRefType();
            if (IetmDataModuleConstants.ICN_TAG_GRAPHIC.equals(refType)
                    || IetmDataModuleConstants.ICN_TAG_MULTIMEDIA.equals(refType)
                    || IetmDataModuleConstants.ICN_TAG_SYMBOL.equals(refType)) {
                String icnCode = item.getTargetDmc(); // 对于ICN，targetDmc字段存储的是infoEntityIdent
                if (StringUtils.hasText(icnCode)) {
                    icnCodeSet.add(icnCode.trim());
                }
            }
        }

        if (icnCodeSet.isEmpty()) {
            log.debug("syncIcnReferences: 未找到graphic/multimedia/symbol引用 dmId={}", dmId);
            return;
        }

        List<String> icnCodes = new ArrayList<>(icnCodeSet);
        log.info("syncIcnReferences: 提取到{}个唯一ICN引用 dmId={} icnCodes={}", icnCodes.size(), dmId, icnCodes);

        // 3. 查询这些 ICN 是否存在于 ietm_icn_manage 表（isdeleted='0'表示未删除）
        List<IetmIcnManage> icnList = icnManageMapper.selectList(
                new LambdaQueryWrapper<IetmIcnManage>()
                        .in(IetmIcnManage::getIcn, icnCodes)
                        .eq(IetmIcnManage::getIsdeleted, IetmDataModuleConstants.ISDELETED_NO)
        );

        if (icnList == null || icnList.isEmpty()) {
            log.warn("syncIcnReferences: 未找到任何ICN记录 dmId={} icnCodes={}", dmId, icnCodes);
            return;
        }

        // 记录哪些ICN未找到（用于日志警告）
        Set<String> foundIcnCodes = icnList.stream()
                .map(IetmIcnManage::getIcn)
                .collect(Collectors.toSet());

        Set<String> missingIcnCodes = new HashSet<>(icnCodes);
        missingIcnCodes.removeAll(foundIcnCodes);

        if (!missingIcnCodes.isEmpty()) {
            log.warn("syncIcnReferences: 部分ICN不存在 dmId={} 缺失ICN={}", dmId, missingIcnCodes);
        }

        log.info("syncIcnReferences: 找到{}个ICN记录（共{}个引用） dmId={}",
                icnList.size(), icnCodes.size(), dmId);

        // 4. 批量查询已存在的引用关系（避免N+1查询）
        List<String> icnIds = icnList.stream()
                .map(IetmIcnManage::getId)
                .collect(Collectors.toList());

        Set<String> existingIcnIds = new HashSet<>();
        if (!icnIds.isEmpty()) {
            List<IetmIcnReference> existingRefs = icnReferenceMapper.selectList(
                    new LambdaQueryWrapper<IetmIcnReference>()
                            .in(IetmIcnReference::getSourceIcnId, icnIds)
                            .eq(IetmIcnReference::getDmCode, dmId)
                            .eq(IetmIcnReference::getReferenceType, IetmDataModuleConstants.REF_TYPE_ICN_TO_DM)
            );

            existingIcnIds = existingRefs.stream()
                    .map(IetmIcnReference::getSourceIcnId)
                    .collect(Collectors.toSet());
        }

        // 5. 批量创建引用记录
        // 注意：这里不会删除XML中已移除的ICN引用记录，保留历史追踪
        Date now = new Date();
        int skipCount = existingIcnIds.size();
        int insertCount = 0;

        for (IetmIcnManage icn : icnList) {
            if (existingIcnIds.contains(icn.getId())) {
                log.debug("syncIcnReferences: 引用关系已存在，跳过 icnId={} dmId={}", icn.getId(), dmId);
                continue;
            }

            // 创建新的引用记录
            IetmIcnReference ref = new IetmIcnReference();
            ref.setSourceIcnId(icn.getId());
            ref.setDmCode(dmId);  // 注意：dmCode字段实际存储的是DM的ID，不是DMC编码
            ref.setReferenceType(IetmDataModuleConstants.REF_TYPE_ICN_TO_DM);
            ref.setRemark(remark);  // 使用传入的备注（区分调用来源）
            ref.setCreateBy(username);
            ref.setCreateTime(now);

            // 处理并发插入的唯一约束冲突
            try {
                icnReferenceMapper.insert(ref);
                insertCount++;
                // 修复P2-1：将单条插入日志从INFO改为DEBUG，避免高频场景日志膨胀
                log.debug("syncIcnReferences: 创建引用记录 icnId={} icn={} dmId={}",
                        icn.getId(), icn.getIcn(), dmId);
            } catch (DuplicateKeyException e) {
                // 并发插入导致重复，跳过
                skipCount++;
                log.debug("syncIcnReferences: 并发插入冲突，跳过 icnId={} dmId={}", icn.getId(), dmId);
            }
        }

        log.info("syncIcnReferences: 完成 dmId={} 新增={} 跳过={} 总ICN数={} 备注={}",
                dmId, insertCount, skipCount, icnList.size(), remark);
    }
}
