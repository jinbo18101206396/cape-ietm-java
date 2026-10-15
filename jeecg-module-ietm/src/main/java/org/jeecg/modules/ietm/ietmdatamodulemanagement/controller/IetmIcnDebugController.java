/**
 * 公式ICN调试工具
 * 用于排查公式预览"ICN加载失败"问题
 *
 * 问题现象：
 * 1. 插入公式后保存成功
 * 2. 点击预览按钮
 * 3. 返回SVG占位图"ICN不存在"
 *
 * 排查步骤：
 * 1. 检查数据库中是否有该ICN记录
 * 2. 检查icnFormulaService是否被正确注入
 * 3. 检查getByIcnWithContent()是否执行
 * 4. 检查filecontent字段是否为空
 * 5. 检查base64提取正则是否匹配
 */

package org.jeecg.modules.ietm.ietmdatamodulemanagement.controller;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnFormulaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公式ICN调试接口
 */
@RestController
@RequestMapping("/ietm/icn/debug")
@Slf4j
@org.springframework.web.bind.annotation.CrossOrigin
@io.swagger.annotations.Api(tags = "ICN调试工具（开发专用，生产环境应禁用）")
public class IetmIcnDebugController {

    // 🔧 跳过登录验证（仅用于调试）
    // 在类名上方添加：@org.jeecg.common.aspect.annotation.PermissionData(pageComponent = "")
    // 或在方法上添加：@org.jeecg.common.aspect.annotation.IgnoreAuth

    @Autowired
    private IIetmIcnFormulaService icnFormulaService;

    /**
     * 调试公式ICN查询
     *
     * GET /jeecg-boot/ietm/icn/debug/formula/{icnCode}
     *
     * 返回：
     * {
     *   "exists": true/false,
     *   "hasContent": true/false,
     *   "contentLength": 12345,
     *   "contentPreview": "前100字符",
     *   "hasBase64": true/false,
     *   "base64Length": 12345,
     *   "error": "错误信息"
     * }
     */
    @GetMapping("/formula/{icnCode}")
    @org.jeecg.common.aspect.annotation.AutoLog(value = "调试公式ICN")
    @org.jeecg.common.aspect.annotation.PermissionData(pageComponent = "")
    public Result<Map<String, Object>> debugFormula(@PathVariable("icnCode") String icnCode) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("=== 开始调试公式ICN: {} ===", icnCode);

            // 1. 检查Service是否注入
            if (icnFormulaService == null) {
                log.error("❌ icnFormulaService未注入");
                result.put("error", "icnFormulaService未注入");
                return Result.error("Service未注入");
            }
            log.info("✅ icnFormulaService已注入: {}", icnFormulaService.getClass().getName());

            // 2. 查询数据库
            IetmIcnFormula entity = icnFormulaService.getByIcnWithContent(icnCode);

            if (entity == null) {
                log.warn("❌ 数据库中不存在该ICN: {}", icnCode);
                result.put("exists", false);
                result.put("error", "数据库中不存在该ICN");
                return Result.OK(result);
            }

            log.info("✅ 找到公式ICN记录: id={}, icn={}", entity.getId(), entity.getIcn());
            result.put("exists", true);
            result.put("id", entity.getId());
            result.put("icn", entity.getIcn());
            result.put("createTime", entity.getCreateTime());

            // 3. 检查filecontent字段
            if (entity.getFilecontent() == null) {
                log.warn("❌ filecontent字段为null");
                result.put("hasContent", false);
                result.put("error", "filecontent字段为null");
                return Result.OK(result);
            }

            if (entity.getFilecontent().length == 0) {
                log.warn("❌ filecontent字段为空数组");
                result.put("hasContent", false);
                result.put("error", "filecontent字段为空数组");
                return Result.OK(result);
            }

            log.info("✅ filecontent字段存在，长度: {} bytes", entity.getFilecontent().length);
            result.put("hasContent", true);
            result.put("contentLength", entity.getFilecontent().length);

            // 4. 转换为字符串
            String formulaHtml = new String(entity.getFilecontent(), StandardCharsets.UTF_8);
            String preview = formulaHtml.length() > 200 ? formulaHtml.substring(0, 200) + "..." : formulaHtml;
            log.info("✅ HTML内容预览(前200字符): {}", preview);
            result.put("contentPreview", preview);

            // 5. 尝试提取base64
            Pattern pattern = Pattern.compile("src=\"(data:image/png;base64,[^\"]+)\"");
            Matcher matcher = pattern.matcher(formulaHtml);

            if (!matcher.find()) {
                log.warn("❌ 未找到base64图片数据（正则不匹配）");
                result.put("hasBase64", false);
                result.put("error", "未找到base64图片数据");

                // 尝试其他可能的格式
                if (formulaHtml.contains("data:image/png;base64,")) {
                    log.info("⚠️ 包含base64标记，但正则不匹配，可能是格式问题");
                    result.put("note", "包含base64标记但正则不匹配");
                }

                return Result.OK(result);
            }

            String base64Data = matcher.group(1);
            String base64Image = base64Data.substring(base64Data.indexOf(',') + 1);

            log.info("✅ 成功提取base64图片，长度: {} 字符", base64Image.length());
            result.put("hasBase64", true);
            result.put("base64Length", base64Image.length());
            result.put("base64Preview", base64Image.substring(0, Math.min(50, base64Image.length())) + "...");

            // 6. 尝试解码
            try {
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
                log.info("✅ base64解码成功，图片大小: {} KB", imageBytes.length / 1024);
                result.put("imageSizeKB", imageBytes.length / 1024);
                result.put("success", true);
            } catch (IllegalArgumentException e) {
                log.error("❌ base64解码失败: {}", e.getMessage());
                result.put("error", "base64解码失败: " + e.getMessage());
            }

            log.info("=== 调试完成 ===");
            return Result.OK(result);

        } catch (Exception e) {
            log.error("❌ 调试过程异常", e);
            result.put("error", "异常: " + e.getMessage());
            result.put("exception", e.getClass().getName());
            return Result.error("调试失败: " + e.getMessage());
        }
    }

    /**
     * 列出最近的公式ICN
     *
     * GET /jeecg-boot/ietm/icn/debug/list
     */
    @GetMapping("/list")
    @org.jeecg.common.aspect.annotation.AutoLog(value = "列出最近公式ICN")
    @org.jeecg.common.aspect.annotation.PermissionData(pageComponent = "")
    public Result<?> listRecentFormulas() {
        try {
            // 查询最近5条公式ICN（不含CLOB字段，避免性能问题）
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<IetmIcnFormula> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            qw.select("id", "icn", "sns", "rpc", "originator", "unique_id", "create_time", "update_time");
            qw.eq("isdeleted", "0");
            qw.orderByDesc("create_time");
            qw.last("FETCH FIRST 10 ROWS ONLY");

            java.util.List<IetmIcnFormula> list = icnFormulaService.list(qw);

            log.info("查询到 {} 条公式ICN记录", list.size());

            return Result.OK(list);

        } catch (Exception e) {
            log.error("查询公式ICN列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
