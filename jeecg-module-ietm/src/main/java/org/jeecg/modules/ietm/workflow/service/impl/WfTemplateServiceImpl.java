package org.jeecg.modules.ietm.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.workflow.entity.WfTemplate;
import org.jeecg.modules.ietm.workflow.entity.WfTemplateDtl;
import org.jeecg.modules.ietm.workflow.mapper.WfTemplateDtlMapper;
import org.jeecg.modules.ietm.workflow.mapper.WfTemplateMapper;
import org.jeecg.modules.ietm.workflow.service.IWfTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description: 工作流模板Service实现
 * @Author: jeecg-boot
 * @Date: 2026-07-26
 * @Version: V1.0
 */
@Slf4j
@Service
public class WfTemplateServiceImpl extends ServiceImpl<WfTemplateMapper, WfTemplate>
        implements IWfTemplateService {

    @Autowired
    private WfTemplateMapper wfTemplateMapper;

    @Autowired
    private WfTemplateDtlMapper wfTemplateDtlMapper;

    /**
     * 获取已发布的流程模板列表
     *
     * @param tmpltype 模板类型（可选）
     * @return 已发布的模板列表
     */
    @Override
    public List<WfTemplate> getPublishedTemplates(String tmpltype) {
        log.debug("获取已发布的流程模板列表，模板类型：{}", tmpltype);

        LambdaQueryWrapper<WfTemplate> queryWrapper = new LambdaQueryWrapper<>();

        // 只查询已发布的模板（status_='1'）
        queryWrapper.eq(WfTemplate::getStatus, "1");

        // 如果指定了模板类型，添加类型过滤
        if (tmpltype != null && !tmpltype.trim().isEmpty()) {
            queryWrapper.eq(WfTemplate::getTmpltype, tmpltype);
        }

        // 按创建时间倒序
        queryWrapper.orderByDesc(WfTemplate::getCreateTime);

        List<WfTemplate> templates = wfTemplateMapper.selectList(queryWrapper);
        log.debug("查询到 {} 个已发布的流程模板", templates.size());

        return templates;
    }

    /**
     * 获取流程模板的节点明细
     *
     * @param templateId 模板ID
     * @return 模板节点配置列表，按seqno排序
     */
    @Override
    public List<WfTemplateDtl> getTemplateNodes(String templateId) {
        log.debug("获取流程模板节点明细，模板ID：{}", templateId);

        if (templateId == null || templateId.trim().isEmpty()) {
            throw new IllegalArgumentException("模板ID不能为空");
        }

        // 使用Mapper自定义查询，按seqno排序
        List<WfTemplateDtl> nodes = wfTemplateDtlMapper.selectByTemplateIdOrderBySeqno(templateId);

        if (nodes == null || nodes.isEmpty()) {
            log.warn("模板【{}】未配置节点", templateId);
        } else {
            log.debug("模板【{}】共有 {} 个节点", templateId, nodes.size());
        }

        return nodes;
    }
}
