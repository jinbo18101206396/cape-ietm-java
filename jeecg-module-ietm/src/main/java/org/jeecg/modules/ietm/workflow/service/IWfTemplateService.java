package org.jeecg.modules.ietm.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.ietm.workflow.entity.WfTemplate;
import org.jeecg.modules.ietm.workflow.entity.WfTemplateDtl;

import java.util.List;

/**
 * @Description: 工作流模板Service接口
 * @Author: jeecg-boot
 * @Date: 2026-07-26
 * @Version: V1.0
 */
public interface IWfTemplateService extends IService<WfTemplate> {

    /**
     * 获取已发布的流程模板列表
     *
     * @param tmpltype 模板类型（可选过滤条件）
     * @return 已发布的模板列表
     */
    List<WfTemplate> getPublishedTemplates(String tmpltype);

    /**
     * 获取流程模板的节点明细
     *
     * @param templateId 模板ID
     * @return 模板节点配置列表
     */
    List<WfTemplateDtl> getTemplateNodes(String templateId);
}
