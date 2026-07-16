package org.jeecg.modules.ietm.projectmanagement.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.data.redis.core.RedisTemplate;
import org.jeecg.common.system.vo.LoginUser;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProjectParams;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProject;
import org.jeecg.modules.ietm.projectmanagement.vo.IetmProjectPage;
import org.jeecg.modules.ietm.projectmanagement.service.IIetmProjectService;
import org.jeecg.modules.ietm.projectmanagement.service.IIetmProjectParamsService;
import org.jeecg.modules.ietm.ietmroleauth.service.IIetmAuthCheckService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.common.aspect.annotation.AutoLog;


/**
 * @Description: 手册管理-手册项目管理列表
 * @Author: jeecg-boot
 * @Date:   2026-01-09
 * @Version: V1.0
 */
@Api(tags="手册管理-手册项目管理列表")
@RestController
@RequestMapping("/ietmproject/ietmProject")
@Slf4j
public class IetmProjectController {
	@Autowired
	private IIetmProjectService ietmProjectService;
	@Autowired
	private IIetmProjectParamsService ietmProjectParamsService;
	@Autowired
	private IIetmAuthCheckService authCheckService;
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	/**
	 * 分页列表查询
	 *
	 * @param ietmProject
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "手册管理-手册项目管理列表-分页列表查询")
	@ApiOperation(value="手册管理-手册项目管理列表-分页列表查询", notes="手册管理-手册项目管理列表-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<IetmProject>> queryPageList(IetmProject ietmProject,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 构建查询条件
		QueryWrapper<IetmProject> queryWrapper = QueryGenerator.initQueryWrapper(ietmProject, req.getParameterMap());

		// 根据授权类型过滤项目列表
		List<String> authorizedProjectIds = authCheckService.getUserAuthorizedProjectIds(sysUser.getId());
		if (authorizedProjectIds != null) {
			if (authorizedProjectIds.isEmpty()) {
				// 用户无任何项目权限，返回空列表
				Page<IetmProject> page = new Page<IetmProject>(pageNo, pageSize);
				return Result.OK(page);
			}
			// 只查询用户有权限的项目
			queryWrapper.in("id", authorizedProjectIds);
		}
		// authorizedProjectIds == null 表示授权类型为"不限制"，不做过滤

		Page<IetmProject> page = new Page<IetmProject>(pageNo, pageSize);
		IPage<IetmProject> pageList = ietmProjectService.page(page, queryWrapper);
		return Result.OK(pageList);
	}

	/**
	 * 列表查询-不分页
	 *
	 * @param ietmProject
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "手册管理-手册项目管理列表-分页列表查询")
	@ApiOperation(value="手册管理-手册项目管理列表-列表查询", notes="手册管理-手册项目管理列表-列表查询")
	@GetMapping(value = "/listData")
	public Result<List<IetmProject>> listData(IetmProject ietmProject, HttpServletRequest req) {
		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 构建查询条件
		QueryWrapper<IetmProject> queryWrapper = QueryGenerator.initQueryWrapper(ietmProject, req.getParameterMap());

		// 根据授权类型过滤项目列表
		List<String> authorizedProjectIds = authCheckService.getUserAuthorizedProjectIds(sysUser.getId());
		if (authorizedProjectIds != null) {
			if (authorizedProjectIds.isEmpty()) {
				// 用户无任何项目权限，返回空列表
				return Result.OK(new ArrayList<>());
			}
			// 只查询用户有权限的项目
			queryWrapper.in("id", authorizedProjectIds);
		}
		// authorizedProjectIds == null 表示授权类型为"不限制"，不做过滤

		List<IetmProject> list = ietmProjectService.list(queryWrapper);
		return Result.OK(list);
	}

	/**
	 *   添加
	 *
	 * @param ietmProjectPage
	 * @return
	 */
	@AutoLog(value = "手册管理-手册项目管理列表-添加")
	@ApiOperation(value="手册管理-手册项目管理列表-添加", notes="手册管理-手册项目管理列表-添加")
    //@RequiresPermissions("ietmproject:ietm_project:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody IetmProjectPage ietmProjectPage) {
		IetmProject ietmProject = new IetmProject();
		BeanUtils.copyProperties(ietmProjectPage, ietmProject);
		ietmProjectService.saveMain(ietmProject, ietmProjectPage.getProjectParam());
		return Result.OK("添加成功！");
	}

	/**
	 *  编辑
	 *
	 * @param ietmProjectPage
	 * @return
	 */
	@AutoLog(value = "手册管理-手册项目管理列表-编辑")
	@ApiOperation(value="手册管理-手册项目管理列表-编辑", notes="手册管理-手册项目管理列表-编辑")
    //@RequiresPermissions("ietmproject:ietm_project:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody IetmProjectPage ietmProjectPage) {
		IetmProject ietmProject = new IetmProject();
		BeanUtils.copyProperties(ietmProjectPage, ietmProject);
		IetmProject ietmProjectEntity = ietmProjectService.getById(ietmProject.getId());
		if(ietmProjectEntity==null) {
			return Result.error("未找到对应数据");
		}

		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 权限校验1：基于授权配置的编辑权限
		if (!authCheckService.hasProjectEditAuth(sysUser.getId(), ietmProject.getId())) {
			return Result.error("无权限编辑该项目！请联系管理员授权。");
		}

		// 权限校验2：仅能编辑自己创建的项目
		if(!ietmProjectEntity.getCreateBy().equals(sysUser.getUsername())) {
			return Result.error("无权限编辑该项目！只能编辑自己创建的项目。");
		}

		ietmProjectService.updateMain(ietmProject);
		return Result.OK("编辑成功!");
	}

	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "手册管理-手册项目管理列表-通过id删除")
	@ApiOperation(value="手册管理-手册项目管理列表-通过id删除", notes="手册管理-手册项目管理列表-通过id删除")
    //@RequiresPermissions("ietmproject:ietm_project:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		IetmProject ietmProject = ietmProjectService.getById(id);

		if(ietmProject == null) {
			return Result.error("未找到对应数据");
		}

		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 权限校验1：基于授权配置的编辑权限
		if (!authCheckService.hasProjectEditAuth(sysUser.getId(), id)) {
			return Result.error("无权限删除该项目！请联系管理员授权。");
		}

		// 权限校验2：仅能删除自己创建的项目
		if(!ietmProject.getCreateBy().equals(sysUser.getUsername())) {
			return Result.error("无权限删除该项目！只能删除自己创建的项目。");
		}

		ietmProjectService.delMain(id);
		return Result.OK("删除成功!");
	}

	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "手册管理-手册项目管理列表-批量删除")
	@ApiOperation(value="手册管理-手册项目管理列表-批量删除", notes="手册管理-手册项目管理列表-批量删除")
    //@RequiresPermissions("ietmproject:ietm_project:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.ietmProjectService.delBatchMain(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}

	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "手册管理-手册项目管理列表-通过id查询")
	@ApiOperation(value="手册管理-手册项目管理列表-通过id查询", notes="手册管理-手册项目管理列表-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<IetmProject> queryById(@RequestParam(name="id",required=true) String id) {
		IetmProject ietmProject = ietmProjectService.getById(id);
		if(ietmProject==null) {
			return Result.error("未找到对应数据");
		}

		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 权限校验：检查用户是否有该项目的浏览权限
		if (!authCheckService.hasProjectReadAuth(sysUser.getId(), id)) {
			return Result.error("无权限查看该项目！请联系管理员授权。");
		}

		return Result.OK(ietmProject);
	}

	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "项目管理-项目参数通过主表ID查询")
	@ApiOperation(value="项目管理-项目参数主表ID查询", notes="项目管理-项目参数-通主表ID查询")
	@GetMapping(value = "/queryIetmProjectParamsByMainId")
	public Result<List<IetmProjectParams>> queryIetmProjectParamsListByMainId(@RequestParam(name="id",required=true) String id) {
		List<IetmProjectParams> ietmProjectParamsList = ietmProjectParamsService.selectByMainId(id);
		return Result.OK(ietmProjectParamsList);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param ietmProject
    */
    //@RequiresPermissions("ietmproject:ietm_project:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, IetmProject ietmProject) {
      // Step.1 组装查询条件查询数据
      QueryWrapper<IetmProject> queryWrapper = QueryGenerator.initQueryWrapper(ietmProject, request.getParameterMap());
      LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

      //配置选中数据查询条件
      String selections = request.getParameter("selections");
      if(oConvertUtils.isNotEmpty(selections)) {
         List<String> selectionList = Arrays.asList(selections.split(","));
         queryWrapper.in("id",selectionList);
      }
      //Step.2 获取导出数据
      List<IetmProject> ietmProjectList = ietmProjectService.list(queryWrapper);

      // Step.3 组装pageList
      List<IetmProjectPage> pageList = new ArrayList<IetmProjectPage>();
      for (IetmProject main : ietmProjectList) {
          IetmProjectPage vo = new IetmProjectPage();
          BeanUtils.copyProperties(main, vo);
          List<IetmProjectParams> ietmProjectParamsList = ietmProjectParamsService.selectByMainId(main.getId());
//          vo.setIetmProjectParamsList(ietmProjectParamsList);
          pageList.add(vo);
      }

      // Step.4 AutoPoi 导出Excel
      ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
      mv.addObject(NormalExcelConstants.FILE_NAME, "手册管理-手册项目管理列表列表");
      mv.addObject(NormalExcelConstants.CLASS, IetmProjectPage.class);
      mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("手册管理-手册项目管理列表数据", "导出人:"+sysUser.getRealname(), "手册管理-手册项目管理列表"));
      mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
      return mv;
    }

    /**
    * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    //@RequiresPermissions("ietmproject:ietm_project:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
      MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
      Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
      for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
          // 获取上传文件对象
          MultipartFile file = entity.getValue();
          ImportParams params = new ImportParams();
          params.setTitleRows(2);
          params.setHeadRows(1);
          params.setNeedSave(true);
          try {
              List<IetmProjectPage> list = ExcelImportUtil.importExcel(file.getInputStream(), IetmProjectPage.class, params);
              for (IetmProjectPage page : list) {
                  IetmProject po = new IetmProject();
                  BeanUtils.copyProperties(page, po);
//                  ietmProjectService.saveMain(po, page.getIetmProjectParamsList());
              }
              return Result.OK("文件导入成功！数据行数:" + list.size());
          } catch (Exception e) {
              log.error(e.getMessage(),e);
              return Result.error("文件导入失败:"+e.getMessage());
          } finally {
              try {
                  file.getInputStream().close();
              } catch (IOException e) {
                  e.printStackTrace();
              }
          }
      }
      return Result.OK("文件导入失败！");
    }

	/**
	 * 改变项目状态
	 *
	 * @param params
	 * @return
	 */
	@AutoLog(value = "手册管理-手册项目管理列表-改变状态")
	@ApiOperation(value="手册管理-手册项目管理列表-改变状态", notes="手册管理-手册项目管理列表-改变状态")
	@PutMapping(value = "/changeStatus")
	public Result<String> changeStatus(@RequestBody Map<String, Object> params) {
		String id = (String) params.get("id");
		Integer status = (Integer) params.get("status");

		if (id == null || status == null) {
			return Result.error("参数不完整！");
		}

		IetmProject ietmProject = ietmProjectService.getById(id);
		if (ietmProject == null) {
			return Result.error("未找到对应的项目！");
		}

		ietmProject.setStatus(status);
		boolean success = ietmProjectService.updateById(ietmProject);

		if (success) {
			return Result.OK("状态修改成功！");
		} else {
			return Result.error("状态修改失败！");
		}
	}

	/**
	 * 打开项目
	 *
	 * @param params
	 * @return
	 */
	@AutoLog(value = "手册管理-手册项目管理列表-打开项目")
	@ApiOperation(value="手册管理-手册项目管理列表-打开项目", notes="手册管理-手册项目管理列表-打开项目")
	@PostMapping(value = "/openProject")
	public Result<Map<String, Object>> openProject(@RequestBody Map<String, String> params) {
		String projectId = params.get("projectId");
		if (projectId == null || projectId.trim().isEmpty()) {
			return Result.error("项目ID不能为空！");
		}

		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 查询项目信息
		IetmProject project = ietmProjectService.getById(projectId);
		if (project == null) {
			return Result.error("项目不存在！");
		}

		// 检查项目状态
		if (project.getStatus() != 1) {
			return Result.error("该项目已被禁用，无法打开！");
		}

		// 权限校验
		if (!authCheckService.hasProjectReadAuth(sysUser.getId(), projectId)) {
			return Result.error("无权限打开该项目！请联系管理员授权。");
		}

		// 保存当前项目到Redis
		String redisKey = "ietm:current_project:" + sysUser.getId();
		Map<String, Object> projectInfo = new HashMap<>();
		projectInfo.put("projectId", project.getId());
		projectInfo.put("projectName", project.getName());
		projectInfo.put("equipmentCode", project.getEquipmentCode());
		projectInfo.put("ietmStandard", project.getIetmStandard());
		projectInfo.put("openTime", System.currentTimeMillis());

		redisTemplate.opsForValue().set(redisKey, projectInfo, 6, TimeUnit.HOURS);

		log.info("用户[{}]打开项目[{}]", sysUser.getUsername(), project.getName());

		return Result.OK(projectInfo);
	}

	/**
	 * 关闭项目
	 *
	 * @return
	 */
	@AutoLog(value = "手册管理-手册项目管理列表-关闭项目")
	@ApiOperation(value="手册管理-手册项目管理列表-关闭项目", notes="手册管理-手册项目管理列表-关闭项目")
	@PostMapping(value = "/closeProject")
	public Result<String> closeProject() {
		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 从Redis删除当前项目
		String redisKey = "ietm:current_project:" + sysUser.getId();
		redisTemplate.delete(redisKey);

		log.info("用户[{}]关闭项目", sysUser.getUsername());

		return Result.OK("项目已关闭");
	}

	/**
	 * 获取当前打开的项目
	 *
	 * @return
	 */
	@ApiOperation(value="手册管理-手册项目管理列表-获取当前项目", notes="手册管理-手册项目管理列表-获取当前项目")
	@GetMapping(value = "/getCurrentProject")
	public Result<Map<String, Object>> getCurrentProject() {
		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 从Redis获取当前项目
		String redisKey = "ietm:current_project:" + sysUser.getId();
		Object obj = redisTemplate.opsForValue().get(redisKey);

		if (obj == null) {
			return Result.OK(null);
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> projectInfo = (Map<String, Object>) obj;

		return Result.OK(projectInfo);
	}

}
