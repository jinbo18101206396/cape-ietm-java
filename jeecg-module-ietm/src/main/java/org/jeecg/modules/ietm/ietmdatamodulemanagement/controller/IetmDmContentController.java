package org.jeecg.modules.ietm.ietmdatamodulemanagement.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDmContentService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXsltTransformer;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.*;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

/**
 * DM 内容编辑器专用端点（浏览或编辑DM内容，需求 §9/§15/§17/§18）
 * 与现有 IetmDataModuleController 解耦，不影响既有37个端点。
 */
@Slf4j
@Api(tags = "DM内容编辑器")
@RestController
@RequestMapping("/ietm/dm-content")
public class IetmDmContentController {

    @Resource
    private IIetmDmContentService contentService;

    /**
     * 加载编辑器数据（§9）
     * 支持历史版本加载：通过 historyId 参数指定历史版本ID
     */
    @AutoLog(value = "DM内容编辑器-加载")
    @ApiOperation("加载DM编辑器数据")
    @GetMapping("/load/{id}")
    public Result<DmEditorLoadVO> load(@PathVariable String id,
                                       @RequestParam(required = false) String historyId) {
        DmEditorLoadVO vo = contentService.loadEditorData(id, historyId);
        if (!"success".equals(vo.getFlag())) {
            return Result.error(vo.getMessage() == null ? "加载失败" : vo.getMessage());
        }
        return Result.OK(vo);
    }

    /** 保存DM正文（§15） */
    @AutoLog(value = "DM内容编辑器-保存正文")
    @ApiOperation("保存DM正文")
    @PostMapping("/save/{id}")
    public Result<String> save(@PathVariable String id,
                               @Valid @RequestBody DmSaveVO vo,
                               HttpServletRequest req) {
        String err = contentService.saveContent(id, vo.getContent(), vo.getVersion(), getUsername(req));
        return err == null ? Result.OK("保存成功") : Result.error(err);
    }

    /**
     * XSD 校验（§17.5）。返回结构对齐 legacy validdm 三态：
     *   flag="0" 内容空 / flag="1" 通过 / flag="error" + errors=[{lineno,info}]
     *
     * 支持两种调用方式（向后兼容，不影响编辑器页面）：
     * 1. 编辑器页面：传入 content（实时校验当前编辑内容）
     * 2. 列表页面：  传入 id（由 Service 从数据库读取 dm_content 后校验）
     */
    @AutoLog(value = "DM内容编辑器-XSD校验")
    @ApiOperation("XSD校验")
    @PostMapping("/validate")
    public Result<Map<String, Object>> validate(@RequestBody DmValidateVO vo) {
        // 列表页调用：只提供id，由Service读库
        if ((vo.getContent() == null || vo.getContent().trim().isEmpty())
                && vo.getId() != null && !vo.getId().trim().isEmpty()) {
            return Result.OK(contentService.validateById(vo.getId()));
        }

        // 编辑器页面调用：content 不为空，走原有逻辑（不改变行为）
        Map<String, Object> ret = new HashMap<>();
        if (vo.getContent() == null || vo.getContent().trim().isEmpty()) {
            ret.put("flag", "0");
            return Result.OK(ret);
        }
        List<DmValidateItemVO> errors = contentService.validateXsd(
                vo.getContent(), vo.getStandard(), vo.getSchema(), null);
        if (errors.isEmpty()) {
            ret.put("flag", "1");
        } else {
            ret.put("flag", "error");
            ret.put("errors", errors);
        }
        return Result.OK(ret);
    }

    /** XML → HTML 预览（§18） */
    @AutoLog(value = "DM内容编辑器-预览")
    @ApiOperation("DM预览")
    @PostMapping("/preview")
    public Result<Map<String, Object>> preview(@RequestBody DmValidateVO vo, HttpServletRequest request) {
        Map<String, Object> ret = new HashMap<>();
        if (vo.getContent() == null || vo.getContent().trim().isEmpty()) {
            ret.put("flag", "null");
            return Result.OK(ret);
        }
        // 二期：传递contextPath给renderHtml，用于ICN图片URL构建
        String contextPath = request.getContextPath();
        String html = DmXmlHelper.renderHtml(vo.getContent(), contextPath);
        if (html == null || html.isEmpty()) {
            ret.put("flag", "noxsl");
        } else {
            ret.put("flag", "success");
            ret.put("html", html);
        }
        return Result.OK(ret);
    }

    /** 清除XSLT模板缓存（开发调试用） */
    @AutoLog(value = "DM内容编辑器-清除缓存")
    @ApiOperation("清除XSLT模板缓存")
    @PostMapping("/clearCache")
    public Result<String> clearCache() {
        DmXsltTransformer.clearCache();
        String info = DmXsltTransformer.getCacheInfo();
        return Result.OK(info);
    }

    /** 提取DM内部可引用片段（§14.5.2③） */
    @AutoLog(value = "DM内容编辑器-引用片段列表")
    @ApiOperation("获取DM内部可引用片段")
    @GetMapping("/getRef/{id}")
    public Result<Map<String, Object>> getRef(@PathVariable String id) {
        Map<String, Object> result = contentService.getRef(id);
        if (!"success".equals(result.get("flag"))) {
            return Result.error((String) result.get("message"));
        }
        return Result.OK(result);
    }

    /** 批量生成 dmRef XML（§14.5.4，dmCode 从目标DM XML权威提取） */
    @AutoLog(value = "DM内容编辑器-生成dmRef")
    @ApiOperation("批量生成dmRef XML")
    @PostMapping("/buildDmRef")
    public Result<Map<String, Object>> buildDmRef(@RequestBody List<DmRefBuildItemVO> items) {
        Map<String, Object> result = contentService.buildDmRef(items);
        if (!"success".equals(result.get("flag"))) {
            return Result.error((String) result.get("message"));
        }
        return Result.OK(result);
    }

    private String getUsername(HttpServletRequest req) {
        try {
            LoginUser u = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            if (u != null && u.getUsername() != null) {
                String name = u.getUsername();
                return name.length() > 50 ? name.substring(0, 50) : name;
            }
        } catch (Exception e) {
            log.warn("获取当前用户失败，使用默认用户system", e);
        }
        return "system";
    }
}
