package org.jeecg.modules.ietm.agencySelection.controller;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.ietm.agencySelection.entity.Project;
import org.jeecg.modules.ietm.agencySelection.service.IProjectService;
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
 * @Description: 机构遴选-项目管理
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@Api(tags = "机构遴选-项目管理")
@RestController
@RequestMapping("/agencySelection/project")
@Slf4j
public class ProjectController extends JeecgController<Project, IProjectService> {

    @Autowired
    private IProjectService projectService;

    @ApiOperation(value = "机构遴选-项目管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<Project>> queryPageList(Project project,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<Project> queryWrapper = QueryGenerator.initQueryWrapper(project, req.getParameterMap());
        queryWrapper.eq("del_flag", 0);

        String extractTime_begin = req.getParameter("extractTime_begin");
        String extractTime_end = req.getParameter("extractTime_end");
        if (oConvertUtils.isNotEmpty(extractTime_begin)) {
            queryWrapper.ge("extract_time", extractTime_begin);
        }
        if (oConvertUtils.isNotEmpty(extractTime_end)) {
            queryWrapper.le("extract_time", extractTime_end);
        }

        queryWrapper.last("ORDER BY CASE WHEN status='待分配' THEN 0 ELSE 1 END, create_time DESC");
        Page<Project> page = new Page<>(pageNo, pageSize);
        IPage<Project> pageList = projectService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    @AutoLog(value = "机构遴选-项目管理-添加")
    @ApiOperation(value = "机构遴选-项目管理-添加")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody Project project) {
        try {
            projectService.saveProject(project);
            return Result.OK("添加成功！");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @AutoLog(value = "机构遴选-项目管理-编辑")
    @ApiOperation(value = "机构遴选-项目管理-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody Project project) {
        try {
            projectService.updateProject(project);
            return Result.OK("编辑成功!");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @AutoLog(value = "机构遴选-项目管理-通过id删除")
    @ApiOperation(value = "机构遴选-项目管理-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        try {
            projectService.deleteProject(id);
            return Result.OK("删除成功!");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @AutoLog(value = "机构遴选-项目管理-批量删除")
    @ApiOperation(value = "机构遴选-项目管理-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        try {
            projectService.deleteBatchProject(Arrays.asList(ids.split(",")));
            return Result.OK("批量删除成功!");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation(value = "机构遴选-项目管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<Project> queryById(@RequestParam(name = "id", required = true) String id) {
        Project project = projectService.getById(id);
        if (project == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(project);
    }

    @ApiOperation(value = "机构遴选-项目管理-导出excel")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, Project project) {
        return super.exportXls(request, project, Project.class, "项目列表");
    }

    @ApiOperation(value = "机构遴选-项目管理-导入excel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            String msg = projectService.importExcel(file);
            return Result.OK(msg);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation(value = "机构遴选-项目管理-下载导入模板")
    @GetMapping(value = "/downloadTemplate")
    public void downloadTemplate(HttpServletResponse response) {
        projectService.downloadTemplate(response);
    }
}
