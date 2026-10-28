package org.jeecg.modules.ietm.ietmdatamodulemanagement.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.LevelledParaXmlUtil;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.LevelledParaValidateVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LevelledPara 设计器后端接口
 * 提供 XML 解析、验证、模板生成等服务
 */
@Slf4j
@Api(tags = "LevelledPara设计器")
@RestController
@RequestMapping("/ietm/levelledpara")
public class LevelledParaController {

    /**
     * 解析 levelledPara XML 为树形结构
     * 前端已实现完整的解析逻辑，此接口仅供服务端校验使用
     */
    @AutoLog(value = "LevelledPara-解析XML")
    @ApiOperation("解析 levelledPara XML")
    @PostMapping("/parse")
    public Result<Map<String, Object>> parseXml(@RequestBody Map<String, String> params) {
        try {
            String xml = params.get("xml");
            if (xml == null || xml.trim().isEmpty()) {
                return Result.error("XML内容不能为空");
            }

            Map<String, Object> result = LevelledParaXmlUtil.parseXmlToTree(xml);
            return Result.OK(result);
        } catch (Exception e) {
            log.error("解析 levelledPara XML 失败", e);
            return Result.error("解析失败: " + e.getMessage());
        }
    }

    /**
     * 验证 levelledPara 数据结构
     */
    @AutoLog(value = "LevelledPara-验证数据")
    @ApiOperation("验证 levelledPara 数据结构")
    @PostMapping("/validate")
    public Result<Map<String, Object>> validate(@Valid @RequestBody LevelledParaValidateVO vo) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 验证 XML 格式
            if (vo.getXml() != null && !vo.getXml().trim().isEmpty()) {
                List<String> errors = LevelledParaXmlUtil.validateXml(vo.getXml());

                if (errors.isEmpty()) {
                    result.put("valid", true);
                    result.put("message", "验证通过");
                } else {
                    result.put("valid", false);
                    result.put("errors", errors);
                }
            } else {
                result.put("valid", false);
                List<String> errors = new ArrayList<>();
                errors.add("XML内容不能为空");
                result.put("errors", errors);
            }

            return Result.OK(result);
        } catch (Exception e) {
            log.error("验证 levelledPara 失败", e);
            return Result.error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 获取 levelledPara 节点模板
     * @param type 节点类型: para/warning/caution/note/figure/multimedia/table
     */
    @AutoLog(value = "LevelledPara-获取模板")
    @ApiOperation("获取节点模板")
    @GetMapping("/template/{type}")
    public Result<Map<String, Object>> getTemplate(@PathVariable String type) {
        try {
            String template = LevelledParaXmlUtil.getNodeTemplate(type);

            if (template == null) {
                return Result.error("不支持的节点类型: " + type);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("type", type);
            result.put("template", template);

            return Result.OK(result);
        } catch (Exception e) {
            log.error("获取节点模板失败 type={}", type, e);
            return Result.error("获取模板失败: " + e.getMessage());
        }
    }

    /**
     * 格式化 levelledPara XML
     */
    @AutoLog(value = "LevelledPara-格式化XML")
    @ApiOperation("格式化 XML")
    @PostMapping("/format")
    public Result<Map<String, Object>> formatXml(@RequestBody Map<String, Object> params) {
        try {
            String xml = (String) params.get("xml");
            Integer indent = params.containsKey("indent") ? (Integer) params.get("indent") : 2;

            if (xml == null || xml.trim().isEmpty()) {
                return Result.error("XML内容不能为空");
            }

            String formatted = LevelledParaXmlUtil.formatXml(xml, indent);

            Map<String, Object> result = new HashMap<>();
            result.put("xml", formatted);

            return Result.OK(result);
        } catch (Exception e) {
            log.error("格式化 XML 失败", e);
            return Result.error("格式化失败: " + e.getMessage());
        }
    }
}
