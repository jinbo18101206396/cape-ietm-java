package org.jeecg.modules.ietm.ietmddn.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.ietmddn.entity.IetmDdn;
import org.jeecg.modules.ietm.ietmddn.service.IIetmDdnService;
import org.jeecg.modules.ietm.ietmddn.vo.*;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @Description: DDN数据交换凭证Controller
 * @Author: jeecg-boot
 * @Date: 2026-09-01
 * @Version: V1.0
 */
@Api(tags = "数据交换-导出数据模块（DDN）")
@RestController
@RequestMapping("/ietm/ddn")
@Slf4j
public class IetmDdnController extends JeecgController<IetmDdn, IIetmDdnService> {

    @Autowired
    private IIetmDdnService ietmDdnService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成DDN数据包
     */
    @AutoLog(value = "数据交换-生成DDN数据包")
    @ApiOperation(value = "生成DDN数据包", notes = "生成DDN数据包并返回下载地址")
    @PostMapping(value = "/generate")
    public Result<DdnGenerateResultVO> generateDdn(@RequestBody @Validated DdnGenerateVO params) {
        try {
            // 1. 获取当前项目上下文（从Redis）
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String redisKey = "ietm:current_project:" + loginUser.getId();
            Object projectObj = redisTemplate.opsForValue().get(redisKey);
            if (projectObj == null) {
                return Result.error("未打开项目，请先打开项目后再导出DDN数据包");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> projectInfo = (Map<String, Object>) projectObj;

            // 2. 调用Service生成DDN
            DdnGenerateResultVO result = ietmDdnService.generateDdn(params, projectInfo);

            return Result.OK(result);

        } catch (Exception e) {
            log.error("生成DDN数据包失败", e);
            return Result.error("生成DDN数据包失败：" + e.getMessage());
        }
    }

    /**
     * 生成ICN专用DDN数据包（导出实体功能）
     */
    @AutoLog(value = "数据交换-导出实体（ICN）")
    @ApiOperation(value = "导出实体（ICN）", notes = "生成ICN专用DDN数据包并返回下载地址")
    @PostMapping(value = "/generateIcn")
    public Result<DdnGenerateResultVO> generateIcnDdn(@RequestBody @Validated IcnExportVO params) {
        try {
            // 1. 获取当前项目上下文（从Redis）
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String redisKey = "ietm:current_project:" + loginUser.getId();
            Object projectObj = redisTemplate.opsForValue().get(redisKey);
            if (projectObj == null) {
                return Result.error("未打开项目，请先打开项目后再导出实体");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> projectInfo = (Map<String, Object>) projectObj;

            // 2. 调用Service生成ICN DDN
            DdnGenerateResultVO result = ietmDdnService.generateIcnDdn(params, projectInfo);

            return Result.OK(result);

        } catch (Exception e) {
            log.error("导出实体（ICN）失败", e);
            return Result.error("导出实体失败：" + e.getMessage());
        }
    }

    /**
     * 下载DDN数据包
     */
    @AutoLog(value = "数据交换-下载DDN数据包")
    @ApiOperation(value = "下载DDN数据包", notes = "根据DDN编码下载ZIP文件")
    @GetMapping(value = "/download")
    public void downloadDdn(@RequestParam(name = "ddnCode", required = true) String ddnCode,
                            HttpServletResponse response) {
        try {
            ietmDdnService.downloadDdnPackage(ddnCode, response);
        } catch (Exception e) {
            log.error("下载DDN数据包失败：ddnCode={}", ddnCode, e);
            throw new RuntimeException("下载失败：" + e.getMessage());
        }
    }
}
