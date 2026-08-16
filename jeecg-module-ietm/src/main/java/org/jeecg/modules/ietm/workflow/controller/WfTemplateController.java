package org.jeecg.modules.ietm.workflow.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.ietm.workflow.entity.WfTemplate;
import org.jeecg.modules.ietm.workflow.entity.WfTemplateDtl;
import org.jeecg.modules.ietm.workflow.service.IWfTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description: 工作流模板Controller
 * @Author: jeecg-boot
 * @Date: 2026-07-26
 * @Version: V1.0
 */
@Api(tags = "工作流模板管理")
@RestController
@RequestMapping("/ietm/workflow/template")
@Slf4j
public class WfTemplateController {

    @Autowired
    private IWfTemplateService wfTemplateService;

    /**
     * 获取已发布的可用流程模板列表
     *
     * 前端调用场景：
     * 1. 批量启动流程弹框打开时自动调用
     * 2. 用户需要从模板中选择流程配置
     *
     * @param tmpltype 模板类型（可选）
     * @return 模板列表，包含 id/tmplname/stagenames 字段
     */
    @AutoLog(value = "工作流模板-获取已发布模板列表")
    @ApiOperation(value = "获取已发布的流程模板列表", notes = "返回已发布状态的流程模板，用于批量启动流程时选择")
    @GetMapping(value = "/getPubOwnWfTemplates")
    public Result<List<WfTemplate>> getPubOwnWfTemplates(
            @RequestParam(value = "tmpltype", required = false) String tmpltype) {
        try {
            List<WfTemplate> templates = wfTemplateService.getPublishedTemplates(tmpltype);
            return Result.OK(templates);
        } catch (Exception e) {
            log.error("获取流程模板列表失败", e);
            return Result.error("获取模板列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取流程模板节点明细
     *
     * 前端调用场景：
     * 1. 用户在批量启动流程弹框中选择模板
     * 2. 点击"确定模板"按钮后调用
     * 3. 或者自动匹配模板后自动调用
     *
     * @param templateId 模板ID
     * @return 模板节点配置列表
     */
    @AutoLog(value = "工作流模板-获取模板节点明细")
    @ApiOperation(value = "获取流程模板节点明细", notes = "根据模板ID获取该模板下的所有节点配置")
    @GetMapping(value = "/getTemplateDtl/{templateId}")
    public Result<List<WfTemplateDtl>> getTemplateDtl(@PathVariable("templateId") String templateId) {
        try {
            List<WfTemplateDtl> nodes = wfTemplateService.getTemplateNodes(templateId);
            return Result.OK(nodes);
        } catch (Exception e) {
            log.error("获取模板节点明细失败，模板ID：{}", templateId, e);
            return Result.error("获取模板节点失败：" + e.getMessage());
        }
    }
}
