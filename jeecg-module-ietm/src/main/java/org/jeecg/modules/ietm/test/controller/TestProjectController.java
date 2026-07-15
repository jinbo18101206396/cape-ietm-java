package org.jeecg.modules.ietm.test.controller;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.ietm.test.entity.TestProject;
import org.jeecg.modules.ietm.test.service.ITestProjectService;

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
 * @Description: 测试-项目管理
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@Api(tags="测试-项目管理")
@RestController
@RequestMapping("/test/project")
@Slf4j
public class TestProjectController extends JeecgController<TestProject, ITestProjectService> {
    @Autowired
    private ITestProjectService testProjectService;

    @ApiOperation(value="测试-项目管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<TestProject>> queryPageList(TestProject testProject,
            @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
            @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<TestProject> queryWrapper = QueryGenerator.initQueryWrapper(testProject, req.getParameterMap());
        queryWrapper.eq("del_flag", 0);
        queryWrapper.orderByAsc("status");
        queryWrapper.orderByDesc("create_time");
        Page<TestProject> page = new Page<>(pageNo, pageSize);
        IPage<TestProject> pageList = testProjectService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    @AutoLog(value = "测试-项目管理-添加")
    @ApiOperation(value="测试-项目管理-添加")
    @RequiresPermissions("test:test_project:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody TestProject testProject) {
        testProjectService.save(testProject);
        return Result.OK("添加成功！");
    }

    @AutoLog(value = "测试-项目管理-编辑")
    @ApiOperation(value="测试-项目管理-编辑")
    @RequiresPermissions("test:test_project:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
    public Result<String> edit(@RequestBody TestProject testProject) {
        testProjectService.updateById(testProject);
        return Result.OK("编辑成功!");
    }

    @AutoLog(value = "测试-项目管理-通过id删除")
    @ApiOperation(value="测试-项目管理-通过id删除")
    @RequiresPermissions("test:test_project:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name="id",required=true) String id) {
        testProjectService.removeById(id);
        return Result.OK("删除成功!");
    }

    @AutoLog(value = "测试-项目管理-批量删除")
    @ApiOperation(value="测试-项目管理-批量删除")
    @RequiresPermissions("test:test_project:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
        this.testProjectService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    @ApiOperation(value="测试-项目管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<TestProject> queryById(@RequestParam(name="id",required=true) String id) {
        TestProject testProject = testProjectService.getById(id);
        if(testProject==null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(testProject);
    }

    @RequiresPermissions("test:test_project:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, TestProject testProject) {
        return super.exportXls(request, testProject, TestProject.class, "测试-项目管理");
    }

    @RequiresPermissions("test:test_project:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, TestProject.class);
    }
}