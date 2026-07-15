package org.jeecg.modules.ietm.test.controller;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.ietm.test.entity.TestAgency;
import org.jeecg.modules.ietm.test.service.ITestAgencyService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;

/**
 * @Description: 测试-机构管理
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@Api(tags="测试-机构管理")
@RestController
@RequestMapping("/test/agency")
@Slf4j
public class TestAgencyController extends JeecgController<TestAgency, ITestAgencyService> {
    @Autowired
    private ITestAgencyService testAgencyService;

    @ApiOperation(value="测试-机构管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<TestAgency>> queryPageList(TestAgency testAgency,
            @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
            @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<TestAgency> queryWrapper = QueryGenerator.initQueryWrapper(testAgency, req.getParameterMap());
        queryWrapper.eq("del_flag", 0);
        queryWrapper.orderByDesc("create_time");
        Page<TestAgency> page = new Page<>(pageNo, pageSize);
        IPage<TestAgency> pageList = testAgencyService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    @AutoLog(value = "测试-机构管理-添加")
    @ApiOperation(value="测试-机构管理-添加")
    @RequiresPermissions("test:test_agency:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody TestAgency testAgency) {
        testAgencyService.save(testAgency);
        return Result.OK("添加成功！");
    }

    @AutoLog(value = "测试-机构管理-编辑")
    @ApiOperation(value="测试-机构管理-编辑")
    @RequiresPermissions("test:test_agency:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
    public Result<String> edit(@RequestBody TestAgency testAgency) {
        testAgencyService.updateById(testAgency);
        return Result.OK("编辑成功!");
    }

    @AutoLog(value = "测试-机构管理-通过id删除")
    @ApiOperation(value="测试-机构管理-通过id删除")
    @RequiresPermissions("test:test_agency:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name="id",required=true) String id) {
        testAgencyService.removeById(id);
        return Result.OK("删除成功!");
    }

    @AutoLog(value = "测试-机构管理-批量删除")
    @ApiOperation(value="测试-机构管理-批量删除")
    @RequiresPermissions("test:test_agency:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
        this.testAgencyService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    @ApiOperation(value="测试-机构管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<TestAgency> queryById(@RequestParam(name="id",required=true) String id) {
        TestAgency testAgency = testAgencyService.getById(id);
        if(testAgency==null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(testAgency);
    }

    @RequiresPermissions("test:test_agency:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, TestAgency testAgency) {
        return super.exportXls(request, testAgency, TestAgency.class, "测试-机构管理");
    }

    @RequiresPermissions("test:test_agency:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, TestAgency.class);
    }
}