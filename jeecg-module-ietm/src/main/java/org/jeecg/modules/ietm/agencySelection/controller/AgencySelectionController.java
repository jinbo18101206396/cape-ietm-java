package org.jeecg.modules.ietm.agencySelection.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.ietm.agencySelection.service.IAgencySelectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 机构遴选-遴选操作
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@Api(tags = "机构遴选-遴选操作")
@RestController
@RequestMapping("/agencySelection")
@Slf4j
public class AgencySelectionController {

    @Autowired
    private IAgencySelectionService selectionService;

    @AutoLog(value = "机构遴选-执行遴选并预览")
    @ApiOperation(value = "机构遴选-执行遴选并预览")
    @PostMapping(value = "/preview")
    public Result<Map<String, Object>> preview(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<String> projectIds = (List<String>) params.get("projectIds");
            if (projectIds == null) projectIds = new ArrayList<>();

            List<Map<String, Object>> eligible = new ArrayList<>();
            List<Map<String, Object>> results = new ArrayList<>();
            String version = selectionService.preview(projectIds, eligible, results);

            Map<String, Object> data = new java.util.HashMap<>();
            data.put("version", version);
            data.put("eligibleAgencies", eligible);
            data.put("results", results);
            return Result.OK(data);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @AutoLog(value = "机构遴选-确认遴选结果")
    @ApiOperation(value = "机构遴选-确认遴选结果")
    @PostMapping(value = "/confirm")
    public Result<String> confirm(@RequestBody Map<String, Object> params) {
        try {
            String version = (String) params.get("version");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) params.get("results");
            selectionService.confirm(version, results);
            return Result.OK("遴选成功！");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation(value = "机构遴选-获取符合资格机构列表")
    @GetMapping(value = "/eligibleAgencies")
    public Result<List<Map<String, Object>>> eligibleAgencies() {
        return Result.OK(selectionService.getEligibleAgencies());
    }

    @ApiOperation(value = "机构遴选-校验前置条件")
    @GetMapping(value = "/validatePreconditions")
    public Result<String> validatePreconditions() {
        try {
            selectionService.validatePreconditions();
            return Result.OK("前置条件满足");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }
}
