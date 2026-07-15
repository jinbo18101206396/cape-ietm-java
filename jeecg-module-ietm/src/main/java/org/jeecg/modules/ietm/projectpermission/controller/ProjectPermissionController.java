package org.jeecg.modules.ietm.projectpermission.controller;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.ietm.ietmprojectcompany.entity.IetmProjectCompany;
import org.jeecg.modules.ietm.ietmprojectcompany.service.IIetmProjectCompanyService;
import org.jeecg.modules.ietm.projectmanagement.service.IIetmProjectService;
import org.jeecg.modules.ietm.projectpermission.entity.ProjectPermission;
import org.jeecg.modules.ietm.projectpermission.service.IProjectPermissionService;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.jeecg.common.system.vo.LoginUser;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProjectParams;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProject;
import org.jeecg.modules.ietm.projectmanagement.vo.IetmProjectPage;
import org.jeecg.modules.ietm.projectmanagement.service.IIetmProjectParamsService;
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
 * @Description: 授权管理-项目手册授权
 * @Author: jeecg-boot
 * @Date:   2026-01-09
 * @Version: V1.0
 */
@Api(tags="授权管理-项目手册授权")
@RestController
@RequestMapping("/projectpermission/projectPermission")
@Slf4j
public class ProjectPermissionController  extends JeecgController<ProjectPermission, IProjectPermissionService> {
	@Autowired
	private IProjectPermissionService projectPermissionService;
	@Autowired
	private IIetmProjectService projectService;

	/**
	 * 分页列表查询
	 *
	 * @param projectPermission
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@GetMapping(value = "/list")
	public Result<IPage<ProjectPermission>> queryPageList(ProjectPermission projectPermission,
													@RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
													@RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
													HttpServletRequest req) {
		QueryWrapper<ProjectPermission> queryWrapper = QueryGenerator.initQueryWrapper(projectPermission, req.getParameterMap());
		Page<ProjectPermission> page = new Page<ProjectPermission>(pageNo, pageSize);
		IPage<ProjectPermission> pageList = projectPermissionService.page(page, queryWrapper);
		return Result.OK(pageList);
	}

	@GetMapping(value="/listProject")
	public Result<?> queryProjectList(HttpServletRequest req){
		QueryWrapper qw = new QueryWrapper();
		qw.orderByDesc("create_time");
		List<IetmProject> list = projectService.list(qw);
		if(CollectionUtils.isNotEmpty(list)){
			List<Map<String, Object>> result = list.stream().map(project -> {
				Map<String, Object> map = new HashMap<>();
				map.put("id", project.getId());
				map.put("title", project.getName());
				map.put("children", "");
				map.put("parentId", "");
				map.put("key", project.getId());
				map.put("value", project.getId());
				return map;
			}).collect(Collectors.toList());
			return Result.OK(result);
		}
		return Result.OK("");
	}

	@GetMapping(value="/listUserByTargetId")
	public Result<IPage<LoginUser>> listUserByTargetId(HttpServletRequest req,
													  @RequestParam(name="targetId") String targetId,
													  @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
													  @RequestParam(name="pageSize", defaultValue="10") Integer pageSize){
		Map<String, Object> paramMap = new HashMap<>();

		String username = req.getParameter("username");
		String realname = req.getParameter("realname");

		paramMap.put("targetId", targetId);
		paramMap.put("username", username);
		paramMap.put("realname", realname);

		Page<LoginUser> page = new Page<LoginUser>(pageNo, pageSize);

		IPage<LoginUser> userList = projectPermissionService.listUserByTargetId(page, paramMap);

		return Result.OK(userList);
	}

	@PostMapping(value="/addUserByTargetId")
	public Result<String> addUserByTargetId(@RequestBody JSONObject params){
		String permissionType = params.getString("permissionType");
		String targetId = params.getString("targetId");
		String userIds = params.getString("userIds");
		String result = projectPermissionService.addUserByTargetId(permissionType, targetId, userIds);

		return Result.ok(result);
	}
	/**
	 *   通过id删除
	 *
	 * @return
	 */
	//@RequiresPermissions("ietmproject:ietm_project:delete")
	@DeleteMapping(value = "/deleteUserByTargetId")
	public Result<String> deleteUserByTargetId(@RequestParam(name="targetId",required=true) String targetId, @RequestParam(name="userIds") String userIds) {
		String[] userIdArray = userIds.split(",");
		QueryWrapper deleteWrapper = new QueryWrapper();
		for(String userId : userIdArray){
			deleteWrapper.clear();
			deleteWrapper.eq("target_id", targetId);
			deleteWrapper.eq("user_id", userId);
			projectPermissionService.remove(deleteWrapper);
		}
		return Result.OK("删除成功!");
	}




	/**
	 *   添加
	 *
	 * @param projectPermission
	 * @return
	 */
    //@RequiresPermissions("ietmproject:ietm_project:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody ProjectPermission projectPermission) {

		projectPermissionService.save(projectPermission);
		return Result.OK("添加成功！");
	}

	/**
	 *  编辑
	 *
	 * @param projectPermission
	 * @return
	 */
    //@RequiresPermissions("ietmproject:ietm_project:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody ProjectPermission projectPermission) {

		projectPermissionService.updateById(projectPermission);

		return Result.OK("编辑成功!");
	}

	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
    //@RequiresPermissions("ietmproject:ietm_project:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		projectPermissionService.removeById(id);
		return Result.OK("删除成功!");
	}

	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
    //@RequiresPermissions("ietmproject:ietm_project:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.projectPermissionService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}

	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@GetMapping(value = "/queryById")
	public Result<ProjectPermission> queryById(@RequestParam(name="id",required=true) String id) {
		ProjectPermission projectPermission = projectPermissionService.getById(id);
		if(projectPermission==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(projectPermission);

	}

	/**
	 * 导出excel
	 *
	 * @param request
	 * @param projectPermission
	 */
	//@RequiresPermissions("ietmprojectcompany:ietm_project_company:exportXls")
	@RequestMapping(value = "/exportXls")
	public ModelAndView exportXls(HttpServletRequest request, ProjectPermission projectPermission) {
		return super.exportXls(request, projectPermission, ProjectPermission.class, "手册管理-单位信息");
	}

	/**
	 * 通过excel导入数据
	 *
	 * @param request
	 * @param response
	 * @return
	 */
	//@RequiresPermissions("ietmprojectcompany:ietm_project_company:importExcel")
	@RequestMapping(value = "/importExcel", method = RequestMethod.POST)
	public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
		return super.importExcel(request, response, ProjectPermission.class);
	}

}
