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
        try {
            String err = contentService.saveContent(id, vo.getContent(), vo.getVersion(), getUsername(req));
            return err == null ? Result.OK("保存成功") : Result.error(err);
        } catch (Exception e) {
            log.error("保存DM内容失败 dmId={} error={}", id, e.getMessage(), e);
            return Result.error("保存失败：" + e.getMessage());
        }
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

    /**
     * 带配置的DM预览
     * 支持用户自定义字体、主题、显示选项等
     */
    @AutoLog(value = "DM内容编辑器-配置预览")
    @ApiOperation("带配置的DM预览")
    @PostMapping("/preview-with-config")
    public Result<Map<String, Object>> previewWithConfig(
        @RequestBody Map<String, Object> params,
        HttpServletRequest request) {

        try {
            // 1. 提取参数
            String content = (String) params.get("content");
            if (content == null || content.trim().isEmpty()) {
                Map<String, Object> ret = new HashMap<>();
                ret.put("flag", "null");
                return Result.OK(ret);
            }

            // 2. 解析配置
            org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.PreviewConfigVO config = null;
            if (params.containsKey("config")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configMap = (Map<String, Object>) params.get("config");
                config = parsePreviewConfig(configMap);
            }

            // 3. 生成HTML
            String contextPath = request.getContextPath();
            String html = DmXmlHelper.renderHtml(content, contextPath);

            if (html == null || html.isEmpty()) {
                Map<String, Object> ret = new HashMap<>();
                ret.put("flag", "noxsl");
                return Result.OK(ret);
            }

            // 4. 应用配置
            if (config != null) {
                config.validate(); // 验证配置
                html = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.PreviewCssGenerator
                    .applyConfigToHtml(html, config);
            }

            // 5. 返回结果
            Map<String, Object> ret = new HashMap<>();
            ret.put("flag", "success");
            ret.put("html", html);
            ret.put("appliedConfig", config);

            return Result.OK(ret);

        } catch (IllegalArgumentException e) {
            log.warn("预览配置验证失败: {}", e.getMessage());
            return Result.error("配置验证失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("配置预览失败", e);
            return Result.error("预览失败: " + e.getMessage());
        }
    }

    /**
     * 解析预览配置
     */
    private org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.PreviewConfigVO parsePreviewConfig(
        Map<String, Object> configMap) {

        org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.PreviewConfigVO config =
            new org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.PreviewConfigVO();

        if (configMap.containsKey("fontSize")) {
            config.setFontSize((String) configMap.get("fontSize"));
        }
        if (configMap.containsKey("theme")) {
            config.setTheme((String) configMap.get("theme"));
        }
        if (configMap.containsKey("lineHeight")) {
            config.setLineHeight((Integer) configMap.get("lineHeight"));
        }
        if (configMap.containsKey("pageWidth")) {
            config.setPageWidth((String) configMap.get("pageWidth"));
        }
        if (configMap.containsKey("showDmCode")) {
            config.setShowDmCode((Boolean) configMap.get("showDmCode"));
        }
        if (configMap.containsKey("showRevision")) {
            config.setShowRevision((Boolean) configMap.get("showRevision"));
        }
        if (configMap.containsKey("showSafetyIcons")) {
            config.setShowSafetyIcons((Boolean) configMap.get("showSafetyIcons"));
        }
        if (configMap.containsKey("graphicScale")) {
            config.setGraphicScale((Integer) configMap.get("graphicScale"));
        }
        if (configMap.containsKey("printMode")) {
            config.setPrintMode((Boolean) configMap.get("printMode"));
        }
        if (configMap.containsKey("showHeader")) {
            config.setShowHeader((Boolean) configMap.get("showHeader"));
        }
        if (configMap.containsKey("showFooter")) {
            config.setShowFooter((Boolean) configMap.get("showFooter"));
        }
        if (configMap.containsKey("pageBreak")) {
            config.setPageBreak((String) configMap.get("pageBreak"));
        }

        return config;
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

    /**
     * 获取预览性能统计
     * 用于监控和分析DM预览性能
     */
    @AutoLog(value = "DM内容编辑器-性能统计")
    @ApiOperation("获取预览性能统计")
    @GetMapping("/performance-stats")
    public Result<Map<String, Object>> getPerformanceStats() {
        try {
            String globalStats = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.PreviewPerformanceMonitor.getGlobalStatistics();

            Map<String, Object> result = new HashMap<>();
            result.put("statistics", globalStats);
            result.put("timestamp", new Date());

            return Result.OK(result);
        } catch (Exception e) {
            log.error("获取性能统计失败", e);
            return Result.error("获取性能统计失败: " + e.getMessage());
        }
    }

    /**
     * 重置性能统计
     * 用于清空历史统计数据，重新开始统计
     */
    @AutoLog(value = "DM内容编辑器-重置性能统计")
    @ApiOperation("重置预览性能统计")
    @PostMapping("/performance-stats/reset")
    public Result<String> resetPerformanceStats() {
        try {
            org.jeecg.modules.ietm.ietmdatamodulemanagement.util.PreviewPerformanceMonitor.resetStatistics();
            return Result.OK("性能统计已重置");
        } catch (Exception e) {
            log.error("重置性能统计失败", e);
            return Result.error("重置性能统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取ICN缓存统计
     * 用于监控ICN加载缓存的使用情况
     */
    @AutoLog(value = "DM内容编辑器-ICN缓存统计")
    @ApiOperation("获取ICN缓存统计")
    @GetMapping("/icn-cache/stats")
    public Result<Map<String, Object>> getIcnCacheStats() {
        try {
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("disabled", true);
            stats.put("message", "IcnLoader temporarily disabled");
            stats.put("timestamp", new Date());
            return Result.OK(stats);
        } catch (Exception e) {
            log.error("获取ICN缓存统计失败", e);
            return Result.error("获取ICN缓存统计失败: " + e.getMessage());
        }
    }

    /**
     * 清空ICN缓存
     * 用于释放内存或重新加载ICN
     */
    @AutoLog(value = "DM内容编辑器-清空ICN缓存")
    @ApiOperation("清空ICN缓存")
    @PostMapping("/icn-cache/clear")
    public Result<String> clearIcnCache() {
        try {
            // TODO: IcnLoader临时移除，待恢复
            // org.jeecg.modules.ietm.ietmdatamodulemanagement.util.IcnLoader.clearCache();
            return Result.OK("ICN缓存已清空");
        } catch (Exception e) {
            log.error("清空ICN缓存失败", e);
            return Result.error("清空ICN缓存失败: " + e.getMessage());
        }
    }

    /**
     * 预加载常用ICN
     * 用于提前加载常用的ICN到缓存，提升预览性能
     */
    @AutoLog(value = "DM内容编辑器-预加载ICN")
    @ApiOperation("预加载常用ICN")
    @PostMapping("/icn-cache/preload")
    public Result<String> preloadIcns(@RequestBody List<String> icnCodes) {
        try {
            if (icnCodes == null || icnCodes.isEmpty()) {
                return Result.error("ICN编码列表不能为空");
            }
            // TODO: IcnLoader临时移除，待恢复
            // org.jeecg.modules.ietm.ietmdatamodulemanagement.util.IcnLoader.preloadCommonIcns(icnCodes);
            return Result.OK("已启动预加载，共" + icnCodes.size() + "个ICN (临时禁用)");
        } catch (Exception e) {
            log.error("预加载ICN失败", e);
            return Result.error("预加载ICN失败: " + e.getMessage());
        }
    }

    /**
     * 导出单个DM为PDF
     */
    @AutoLog(value = "DM内容编辑器-导出PDF")
    @ApiOperation("导出单个DM为PDF")
    @PostMapping("/export-pdf")
    public void exportPdf(@RequestBody Map<String, Object> params,
                         javax.servlet.http.HttpServletResponse response) {
        try {
            // 1. 提取参数
            String content = (String) params.get("content");
            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("内容不能为空");
            }

            String title = (String) params.getOrDefault("title", "数据模块");
            String author = (String) params.getOrDefault("author", "IETM系统");
            String fileName = (String) params.get("fileName");

            // 2. 生成HTML
            String html = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper
                .renderHtml(content, "/ietm");

            // 3. 配置PDF
            org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmPdfExporter.PdfConfig config =
                new org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmPdfExporter.PdfConfig();
            config.setTitle(title);
            config.setAuthor(author);

            // 4. 导出PDF
            byte[] pdfBytes = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmPdfExporter
                .exportToPdf(html, config);

            // 5. 生成文件名
            if (fileName == null || fileName.isEmpty()) {
                fileName = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmPdfExporter
                    .generateFileName(title);
            }
            if (!fileName.endsWith(".pdf")) {
                fileName += ".pdf";
            }

            // 6. 返回PDF文件
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                "attachment; filename=\"" + new String(fileName.getBytes("UTF-8"), "ISO-8859-1") + "\"");
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();

            log.info("PDF导出成功: fileName={}, size={}KB", fileName, pdfBytes.length / 1024);

        } catch (Exception e) {
            log.error("PDF导出失败", e);
            try {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("返回错误信息失败", ex);
            }
        }
    }

    /**
     * 批量导出DM为PDF
     */
    @AutoLog(value = "DM内容编辑器-批量导出PDF")
    @ApiOperation("批量导出DM为PDF")
    @PostMapping("/export-batch-pdf")
    public void exportBatchPdf(@RequestBody Map<String, Object> params,
                              javax.servlet.http.HttpServletResponse response) {
        try {
            // 1. 提取参数
            @SuppressWarnings("unchecked")
            List<String> dmIds = (List<String>) params.get("dmIds");
            if (dmIds == null || dmIds.isEmpty()) {
                throw new RuntimeException("DM ID列表不能为空");
            }

            String fileName = (String) params.getOrDefault("fileName", "数据模块合集");

            // 2. 批量生成HTML
            List<String> htmlList = new ArrayList<>();
            List<String> titles = new ArrayList<>();

            for (String dmId : dmIds) {
                // TODO: 从数据库加载DM
                // IetmDataModule dm = dmService.getById(dmId);
                // String html = DmXmlHelper.renderHtml(dm.getDmContent(), "/ietm");
                // htmlList.add(html);
                // titles.add(dm.getTechName() + " - " + dm.getInfoName());

                // 临时实现：返回占位符
                htmlList.add("<div><h1>数据模块 " + dmId + "</h1><p>内容占位符</p></div>");
                titles.add("数据模块 " + dmId);
            }

            // 3. 配置PDF
            org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmPdfExporter.PdfConfig config =
                new org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmPdfExporter.PdfConfig();
            config.setTitle(fileName);
            config.setAuthor("IETM系统");
            config.setShowToc(true);

            // 4. 批量导出PDF
            byte[] pdfBytes = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmPdfExporter
                .exportBatchToPdf(htmlList, titles, config);

            // 5. 生成文件名
            fileName = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmPdfExporter
                .generateFileName(fileName);

            // 6. 返回PDF文件
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                "attachment; filename=\"" + new String(fileName.getBytes("UTF-8"), "ISO-8859-1") + "\"");
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();

            log.info("批量PDF导出成功: fileName={}, dmCount={}, size={}KB",
                fileName, dmIds.size(), pdfBytes.length / 1024);

        } catch (Exception e) {
            log.error("批量PDF导出失败", e);
            try {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("返回错误信息失败", ex);
            }
        }
    }

    /**
     * Para设计器 - para2html转换
     */
    @AutoLog(value = "Para设计器-para2html转换")
    @ApiOperation("para2html转换（XML→HTML）")
    @PostMapping("/para2html")
    public Result<String> para2html(@Valid @RequestBody ParaConvertVO vo) {
        try {
            String html = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.ParaConverter.para2html(vo.getXml());
            return Result.OK(html);
        } catch (Exception e) {
            log.error("para2html转换失败", e);
            return Result.error("转换失败：" + e.getMessage());
        }
    }

    /**
     * Para设计器 - html2para转换
     */
    @AutoLog(value = "Para设计器-html2para转换")
    @ApiOperation("html2para转换（HTML→XML）")
    @PostMapping("/html2para")
    public Result<String> html2para(@Valid @RequestBody ParaConvertVO vo) {
        try {
            String xml = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.ParaConverter.html2para(vo.getXml());
            return Result.OK(xml);
        } catch (Exception e) {
            log.error("html2para转换失败", e);
            return Result.error("转换失败：" + e.getMessage());
        }
    }

    /**
     * Para设计器 - 分配uniqueid
     * 使用SELECT FOR UPDATE行级锁防止并发冲突
     */
    @AutoLog(value = "Para设计器-分配uniqueid")
    @ApiOperation("分配uniqueid")
    @PostMapping("/allocateUniqueids")
    public Result<Map<String, Object>> allocateUniqueids(@RequestBody Map<String, Object> params) {
        try {
            String dmId = (String) params.get("dmId");
            Integer count = (Integer) params.get("count");

            if (dmId == null || count == null || count <= 0) {
                return Result.error("参数错误");
            }

            // 调用Service分配uniqueid
            int startId = contentService.allocateUniqueids(dmId, count);

            Map<String, Object> result = new HashMap<>();
            result.put("start", startId);
            result.put("count", count);

            return Result.OK(result);
        } catch (Exception e) {
            log.error("分配uniqueid失败", e);
            return Result.error("分配失败：" + e.getMessage());
        }
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
