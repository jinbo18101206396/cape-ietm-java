package org.jeecg.modules.ietm.test.controller;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.ietm.test.entity.TestAgencyFile;
import org.jeecg.modules.ietm.test.service.ITestAgencyFileService;

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
 * @Description: 测试-附件管理
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@Api(tags="测试-附件管理")
@RestController
@RequestMapping("/test/file")
@Slf4j
public class TestAgencyFileController extends JeecgController<TestAgencyFile, ITestAgencyFileService> {
    @Autowired
    private ITestAgencyFileService testAgencyFileService;

    @ApiOperation(value="测试-附件管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<TestAgencyFile>> queryPageList(TestAgencyFile testAgencyFile,
            @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
            @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<TestAgencyFile> queryWrapper = QueryGenerator.initQueryWrapper(testAgencyFile, req.getParameterMap());
        queryWrapper.eq("del_flag", 0);
        queryWrapper.orderByDesc("create_time");
        Page<TestAgencyFile> page = new Page<>(pageNo, pageSize);
        IPage<TestAgencyFile> pageList = testAgencyFileService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    @AutoLog(value = "测试-附件管理-添加")
    @ApiOperation(value="测试-附件管理-添加")
    @RequiresPermissions("test:test_agency_file:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody TestAgencyFile testAgencyFile) {
        testAgencyFileService.save(testAgencyFile);
        return Result.OK("添加成功！");
    }

    @AutoLog(value = "测试-附件管理-编辑")
    @ApiOperation(value="测试-附件管理-编辑")
    @RequiresPermissions("test:test_agency_file:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
    public Result<String> edit(@RequestBody TestAgencyFile testAgencyFile) {
        testAgencyFileService.updateById(testAgencyFile);
        return Result.OK("编辑成功!");
    }

    @AutoLog(value = "测试-附件管理-通过id删除")
    @ApiOperation(value="测试-附件管理-通过id删除")
    @RequiresPermissions("test:test_agency_file:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name="id",required=true) String id) {
        testAgencyFileService.removeById(id);
        return Result.OK("删除成功!");
    }

    @AutoLog(value = "测试-附件管理-批量删除")
    @ApiOperation(value="测试-附件管理-批量删除")
    @RequiresPermissions("test:test_agency_file:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
        this.testAgencyFileService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    @ApiOperation(value="测试-附件管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<TestAgencyFile> queryById(@RequestParam(name="id",required=true) String id) {
        TestAgencyFile testAgencyFile = testAgencyFileService.getById(id);
        if(testAgencyFile==null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(testAgencyFile);
    }

    @RequiresPermissions("test:test_agency_file:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, TestAgencyFile testAgencyFile) {
        return super.exportXls(request, testAgencyFile, TestAgencyFile.class, "测试-附件管理");
    }

    @RequiresPermissions("test:test_agency_file:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, TestAgencyFile.class);
    }
}