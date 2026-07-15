package org.jeecg.modules.ietm.agencySelection.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.ietm.agencySelection.entity.Agency;
import org.jeecg.modules.ietm.agencySelection.service.IAgencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 机构遴选-机构管理
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@Api(tags = "机构遴选-机构管理")
@RestController
@RequestMapping("/agencySelection/agency")
@Slf4j
public class AgencyController extends JeecgController<Agency, IAgencyService> {

    @Autowired
    private IAgencyService agencyService;

    @ApiOperation(value = "机构遴选-机构管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<Agency>> queryPageList(Agency agency,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<Agency> queryWrapper = QueryGenerator.initQueryWrapper(agency, req.getParameterMap());
        queryWrapper.eq("del_flag", 0);
        queryWrapper.orderByDesc("create_time");
        Page<Agency> page = new Page<>(pageNo, pageSize);
        IPage<Agency> pageList = agencyService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    @AutoLog(value = "机构遴选-机构管理-添加")
    @ApiOperation(value = "机构遴选-机构管理-添加")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody Agency agency) {
        try {
            agencyService.saveAgency(agency);
            return Result.OK("添加成功！");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @AutoLog(value = "机构遴选-机构管理-编辑")
    @ApiOperation(value = "机构遴选-机构管理-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody Agency agency) {
        try {
            agencyService.updateAgency(agency);
            return Result.OK("编辑成功!");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @AutoLog(value = "机构遴选-机构管理-通过id删除")
    @ApiOperation(value = "机构遴选-机构管理-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        try {
            agencyService.deleteAgency(id);
            return Result.OK("删除成功!");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @AutoLog(value = "机构遴选-机构管理-批量删除")
    @ApiOperation(value = "机构遴选-机构管理-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        try {
            agencyService.deleteBatchAgency(Arrays.asList(ids.split(",")));
            return Result.OK("批量删除成功!");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation(value = "机构遴选-机构管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<Agency> queryById(@RequestParam(name = "id", required = true) String id) {
        Agency agency = agencyService.getById(id);
        if (agency == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(agency);
    }

    @ApiOperation(value = "机构遴选-机构管理-查询已分配项目数")
    @GetMapping(value = "/assignedCount")
    public Result<Integer> assignedCount(@RequestParam(name = "id", required = true) String id) {
        return Result.OK(agencyService.getAssignedProjectCount(id));
    }

    @ApiOperation(value = "机构遴选-机构管理-重置预设比例")
    @PostMapping(value = "/resetRatios")
    public Result<String> resetRatios() {
        try {
            agencyService.resetAllRatios();
            return Result.OK("所有机构的本次摇号预设比例已重置为0%");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation(value = "机构遴选-机构管理-统计概览")
    @GetMapping(value = "/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.OK(agencyService.getStatistics());
    }

    @ApiOperation(value = "机构遴选-机构管理-导出excel")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, Agency agency) {
        return super.exportXls(request, agency, Agency.class, "机构列表");
    }

    @ApiOperation(value = "机构遴选-机构管理-导入excel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            String msg = agencyService.importExcel(file);
            return Result.OK(msg);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation(value = "机构遴选-机构管理-下载导入模板")
    @GetMapping(value = "/downloadTemplate")
    public void downloadTemplate(HttpServletResponse response) {
        agencyService.downloadTemplate(response);
    }
}
