package org.jeecg.modules.ietm.projectconfigurationmanagement.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.TreeModel;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectGxTreeVo;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.system.vo.LoginUser;
import org.apache.shiro.SecurityUtils;
import org.jeecg.modules.ietm.ietmroleauth.service.IIetmAuthCheckService;

 /**
 * @Description: 项目管理-项目构型管理
 * @Author: jeecg-boot
 * @Date:   2026-02-10
 * @Version: V1.0
 */
@Api(tags="项目管理-项目构型管理")
@RestController
@RequestMapping("/projectconfigurationmanagement/ietmProjectConfigurationManagement")
@Slf4j
public class IetmProjectConfigurationManagementController extends JeecgController<IetmProjectConfigurationManagement, IIetmProjectConfigurationManagementService>{
	@Autowired
	private IIetmProjectConfigurationManagementService ietmProjectConfigurationManagementService;
	@Autowired
	private IIetmAuthCheckService authCheckService;

	/**
	 * 分页列表查询
	 *
	 * @param ietmProjectConfigurationManagement
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "项目管理-项目构型管理-分页列表查询")
	@ApiOperation(value="项目管理-项目构型管理-分页列表查询", notes="项目管理-项目构型管理-分页列表查询")
	@GetMapping(value = "/rootList")
	public Result<IPage<IetmProjectConfigurationManagement>> queryPageList(IetmProjectConfigurationManagement ietmProjectConfigurationManagement,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		// 获取项目ID参数
		String projectId = req.getParameter("projectId");
		log.info("查询构型列表，projectId={}", projectId);

		// 如果没有项目ID，返回空列表
		if (oConvertUtils.isEmpty(projectId)) {
			log.warn("未提供projectId参数，返回空列表");
			IPage<IetmProjectConfigurationManagement> pageList = new Page<>(1, 10, 0);
			pageList.setRecords(new ArrayList<>());
			return Result.OK(pageList);
		}

		String hasQuery = req.getParameter("hasQuery");
        if(hasQuery != null && "true".equals(hasQuery)){
            QueryWrapper<IetmProjectConfigurationManagement> queryWrapper =  QueryGenerator.initQueryWrapper(ietmProjectConfigurationManagement, req.getParameterMap());
            // 添加项目ID过滤条件
            queryWrapper.eq("project_id", projectId);
            // ✅ 修复：只查询根节点 (pid='0')
            queryWrapper.eq("pid", "0");
            List<IetmProjectConfigurationManagement> list = ietmProjectConfigurationManagementService.queryTreeListNoPage(queryWrapper);
            // 填充层级信息
            ietmProjectConfigurationManagementService.fillNodeLevels(list);
            IPage<IetmProjectConfigurationManagement> pageList = new Page<>(1, 10, list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        }else{
            String parentId = ietmProjectConfigurationManagement.getPid();
            if (oConvertUtils.isEmpty(parentId)) {
                parentId = "0";
            }
            ietmProjectConfigurationManagement.setPid(null);
            QueryWrapper<IetmProjectConfigurationManagement> queryWrapper = QueryGenerator.initQueryWrapper(ietmProjectConfigurationManagement, req.getParameterMap());
            // 使用 eq 防止模糊查询
            queryWrapper.eq("pid", parentId);
            // 添加项目ID过滤条件
            queryWrapper.eq("project_id", projectId);
            Page<IetmProjectConfigurationManagement> page = new Page<IetmProjectConfigurationManagement>(pageNo, pageSize);
            IPage<IetmProjectConfigurationManagement> pageList = ietmProjectConfigurationManagementService.page(page, queryWrapper);
            // 填充层级信息
            ietmProjectConfigurationManagementService.fillNodeLevels(pageList.getRecords());
            return Result.OK(pageList);
        }
	}

	 /**
	  * 根据projectId查询构型树结构数据
	  * @param projectId
	  * @param req
	  * @return
	  */
	 @GetMapping(value = "/getTreeList")
	 public Result<List<IetmProjectGxTreeVo>> getTreeList(@RequestParam(value = "projectId", required = true) String projectId, HttpServletRequest req){
	 	log.info("=== 查询项目构型树 ===");
		log.info("projectId: {}", projectId);

		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
		log.info("当前用户: userId={}, username={}", sysUser.getId(), sysUser.getUsername());

		// 检查是否为管理员
		boolean isAdmin = authCheckService.isAdmin(sysUser.getId());
		log.info("是否为管理员: {}", isAdmin);

		// 检查授权类型
		String authType = authCheckService.getAuthType();
		log.info("当前授权类型: {} (0=不限制, 1=项目按角色授权, 2=构型按角色授权)", authType);

		QueryWrapper queryWrapper = new QueryWrapper();
		queryWrapper.eq("project_id", projectId);
		List<IetmProjectConfigurationManagement> list = ietmProjectConfigurationManagementService.list(queryWrapper);
		log.info("查询到构型数据数量: {}", list.size());

		// 根据授权类型过滤构型列表
		List<String> authorizedCmIds = authCheckService.getUserAuthorizedCmIds(sysUser.getId(), projectId);
		log.info("用户授权的构型ID列表: {}", authorizedCmIds);
		if (authorizedCmIds != null) {
			if (authorizedCmIds.isEmpty()) {
				// 用户无任何构型权限，返回空列表
				log.warn("用户无任何构型权限，返回空列表");
				return Result.OK(new ArrayList<>());
			}
			// 保留被授权的构型节点，以及它们的所有父节点路径
			List<IetmProjectConfigurationManagement> filteredList = new ArrayList<>();
			for (IetmProjectConfigurationManagement cm : list) {
				if (authorizedCmIds.contains(cm.getId())) {
					// 这是被授权的节点，保留它
					filteredList.add(cm);
					// 递归查找并添加所有父节点
					addParentNodes(cm.getPid(), list, filteredList);
				}
			}
			list = filteredList;
			log.info("过滤后构型数据数量（含父节点路径）: {}", list.size());
		}
		// authorizedCmIds == null 表示授权类型为"不限制"或"项目授权"，不做过滤

		if (!list.isEmpty()) {
			log.info("第一条数据: id={}, title={}, pid={}", list.get(0).getId(), list.get(0).getTitle(), list.get(0).getPid());
		}
		List<IetmProjectGxTreeVo> treeList = ietmProjectConfigurationManagementService.buildTree(list, projectId);
		log.info("构建树结构后根节点数量: {}", treeList.size());
		return Result.OK(treeList);
	 }


	 /**
	  * 【vue3专用】加载节点的子数据
	  *
	  * @param pid
	  * @return
	  */
	 @RequestMapping(value = "/loadTreeChildren", method = RequestMethod.GET)
	 public Result<List<SelectTreeModel>> loadTreeChildren(@RequestParam(name = "pid") String pid) {
		 Result<List<SelectTreeModel>> result = new Result<>();
		 try {
			 List<SelectTreeModel> ls = ietmProjectConfigurationManagementService.queryListByPid(pid);
			 result.setResult(ls);
			 result.setSuccess(true);
		 } catch (Exception e) {
			 e.printStackTrace();
			 result.setMessage(e.getMessage());
			 result.setSuccess(false);
		 }
		 return result;
	 }

	 /**
	  * 【vue3专用】加载一级节点/如果是同步 则所有数据
	  *
	  * @param async
	  * @param pcode
	  * @return
	  */
	 @RequestMapping(value = "/loadTreeRoot", method = RequestMethod.GET)
	 public Result<List<SelectTreeModel>> loadTreeRoot(@RequestParam(name = "async") Boolean async, @RequestParam(name = "pcode") String pcode) {
		 Result<List<SelectTreeModel>> result = new Result<>();
		 try {
			 List<SelectTreeModel> ls = ietmProjectConfigurationManagementService.queryListByCode(pcode);
			 if (!async) {
				 loadAllChildren(ls);
			 }
			 result.setResult(ls);
			 result.setSuccess(true);
		 } catch (Exception e) {
			 e.printStackTrace();
			 result.setMessage(e.getMessage());
			 result.setSuccess(false);
		 }
		 return result;
	 }

	 /**
	  * 【vue3专用】递归求子节点 同步加载用到
	  *
	  * @param ls
	  */
	 private void loadAllChildren(List<SelectTreeModel> ls) {
		 for (SelectTreeModel tsm : ls) {
			 List<SelectTreeModel> temp = ietmProjectConfigurationManagementService.queryListByPid(tsm.getKey());
			 if (temp != null && temp.size() > 0) {
				 tsm.setChildren(temp);
				 loadAllChildren(temp);
			 }
		 }
	 }

	 /**
      * 获取子数据
      * @param ietmProjectConfigurationManagement
      * @param req
      * @return
      */
	//@AutoLog(value = "项目管理-项目构型管理-获取子数据")
	@ApiOperation(value="项目管理-项目构型管理-获取子数据", notes="项目管理-项目构型管理-获取子数据")
	@GetMapping(value = "/childList")
	public Result<IPage<IetmProjectConfigurationManagement>> queryPageList(IetmProjectConfigurationManagement ietmProjectConfigurationManagement,HttpServletRequest req) {
		QueryWrapper<IetmProjectConfigurationManagement> queryWrapper = QueryGenerator.initQueryWrapper(ietmProjectConfigurationManagement, req.getParameterMap());

		// 添加项目ID过滤条件，防止查询所有项目的数据
		String projectId = req.getParameter("projectId");
		if (oConvertUtils.isNotEmpty(projectId)) {
			queryWrapper.eq("project_id", projectId);
			log.info("查询子节点，pid={}, projectId={}", ietmProjectConfigurationManagement.getPid(), projectId);
		} else {
			log.warn("childList接口未提供projectId参数，可能导致性能问题");
		}

		List<IetmProjectConfigurationManagement> list = ietmProjectConfigurationManagementService.list(queryWrapper);
		// 填充层级信息
		ietmProjectConfigurationManagementService.fillNodeLevels(list);
		IPage<IetmProjectConfigurationManagement> pageList = new Page<>(1, 10, list.size());
        pageList.setRecords(list);
		return Result.OK(pageList);
	}

    /**
      * 批量查询子节点
      * @param parentIds 父ID（多个采用半角逗号分割）
      * @param projectId 项目ID
      * @return 返回 IPage
      */
	//@AutoLog(value = "项目管理-项目构型管理-批量获取子数据")
    @ApiOperation(value="项目管理-项目构型管理-批量获取子数据", notes="项目管理-项目构型管理-批量获取子数据")
    @GetMapping("/getChildListBatch")
    public Result getChildListBatch(@RequestParam("parentIds") String parentIds,
                                     @RequestParam(value = "projectId", required = false) String projectId) {
        try {
            QueryWrapper<IetmProjectConfigurationManagement> queryWrapper = new QueryWrapper<>();
            List<String> parentIdList = Arrays.asList(parentIds.split(","));
            queryWrapper.in("pid", parentIdList);

            // 添加项目ID过滤条件
            if (oConvertUtils.isNotEmpty(projectId)) {
                queryWrapper.eq("project_id", projectId);
                log.info("批量查询子节点，parentIds={}, projectId={}", parentIds, projectId);
            } else {
                log.warn("批量查询子节点未提供projectId参数，parentIds={}", parentIds);
            }

            List<IetmProjectConfigurationManagement> list = ietmProjectConfigurationManagementService.list(queryWrapper);
            // 填充层级信息
            ietmProjectConfigurationManagementService.fillNodeLevels(list);
            IPage<IetmProjectConfigurationManagement> pageList = new Page<>(1, 10, list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error("批量查询子节点失败：" + e.getMessage());
        }
    }

	/**
	 *   添加
	 *
	 * @param ietmProjectConfigurationManagement
	 * @return
	 */
	@AutoLog(value = "项目管理-项目构型管理-添加")
	@ApiOperation(value="项目管理-项目构型管理-添加", notes="项目管理-项目构型管理-添加")
    //@RequiresPermissions("projectconfigurationmanagement:ietm_project_configuration_management:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody IetmProjectConfigurationManagement ietmProjectConfigurationManagement) {
		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 权限校验：检查用户是否有父节点的编辑权限
		String pid = ietmProjectConfigurationManagement.getPid();
		if (pid != null && !"0".equals(pid)) {
			if (!authCheckService.hasCmEditAuth(sysUser.getId(), pid)) {
				return Result.error("无权限在该构型节点下添加子节点！请联系管理员授权。");
			}
		}

		ietmProjectConfigurationManagementService.addIetmProjectConfigurationManagement(ietmProjectConfigurationManagement);
		return Result.OK("添加成功！");
	}

	/**
	 *  编辑
	 *
	 * @param ietmProjectConfigurationManagement
	 * @return
	 */
	@AutoLog(value = "项目管理-项目构型管理-编辑")
	@ApiOperation(value="项目管理-项目构型管理-编辑", notes="项目管理-项目构型管理-编辑")
    //@RequiresPermissions("projectconfigurationmanagement:ietm_project_configuration_management:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody IetmProjectConfigurationManagement ietmProjectConfigurationManagement) {
		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 权限校验：检查用户是否有该构型的编辑权限
		if (!authCheckService.hasCmEditAuth(sysUser.getId(), ietmProjectConfigurationManagement.getId())) {
			return Result.error("无权限编辑该构型节点！请联系管理员授权。");
		}

		ietmProjectConfigurationManagementService.updateIetmProjectConfigurationManagement(ietmProjectConfigurationManagement);
		return Result.OK("编辑成功!");
	}

	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "项目管理-项目构型管理-通过id删除")
	@ApiOperation(value="项目管理-项目构型管理-通过id删除", notes="项目管理-项目构型管理-通过id删除")
    //@RequiresPermissions("projectconfigurationmanagement:ietm_project_configuration_management:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 权限校验：检查用户是否有该构型的编辑权限
		if (!authCheckService.hasCmEditAuth(sysUser.getId(), id)) {
			return Result.error("无权限删除该构型节点！请联系管理员授权。");
		}

		ietmProjectConfigurationManagementService.deleteIetmProjectConfigurationManagement(id);
		return Result.OK("删除成功!");
	}

	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "项目管理-项目构型管理-批量删除")
	@ApiOperation(value="项目管理-项目构型管理-批量删除", notes="项目管理-项目构型管理-批量删除")
    //@RequiresPermissions("projectconfigurationmanagement:ietm_project_configuration_management:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.ietmProjectConfigurationManagementService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}

	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "项目管理-项目构型管理-通过id查询")
	@ApiOperation(value="项目管理-项目构型管理-通过id查询", notes="项目管理-项目构型管理-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<IetmProjectConfigurationManagement> queryById(@RequestParam(name="id",required=true) String id) {
		IetmProjectConfigurationManagement ietmProjectConfigurationManagement = ietmProjectConfigurationManagementService.getById(id);
		if(ietmProjectConfigurationManagement==null) {
			return Result.error("未找到对应数据");
		}

		// 获取当前登录用户
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		// 权限校验：检查用户是否有该构型的浏览权限
		if (!authCheckService.hasCmReadAuth(sysUser.getId(), id)) {
			return Result.error("无权限查看该构型节点！请联系管理员授权。");
		}

		return Result.OK(ietmProjectConfigurationManagement);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param ietmProjectConfigurationManagement
    */
    //@RequiresPermissions("projectconfigurationmanagement:ietm_project_configuration_management:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, IetmProjectConfigurationManagement ietmProjectConfigurationManagement) {
		return super.exportXls(request, ietmProjectConfigurationManagement, IetmProjectConfigurationManagement.class, "项目管理-项目构型管理");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    //@RequiresPermissions("projectconfigurationmanagement:ietm_project_configuration_management:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
		return super.importExcel(request, response, IetmProjectConfigurationManagement.class);
    }


	 /**
	  *
	  * 项目数据模块管理左侧树
	  * @return
	  */
	 @GetMapping(value = "/queryTreeList")
	 public Result<List<TreeModel>> queryTreeList() {
		 Result<List<TreeModel>> result = new Result<>();
		 try {
			 List<TreeModel> treeModelList = ietmProjectConfigurationManagementService.queryTreeList();
			 result.setResult(treeModelList);
			 result.setSuccess(true);
		 } catch (Exception e) {
			 e.printStackTrace();
			 result.setMessage(e.getMessage());
			 result.setSuccess(false);
		 }
		 return result;
	 }

	/**
	 * 复制所有构型
	 *
	 * @param params
	 * @return
	 */
	@AutoLog(value = "项目管理-项目构型管理-复制所有构型")
	@ApiOperation(value="项目管理-项目构型管理-复制所有构型", notes="项目管理-项目构型管理-复制所有构型")
	@PostMapping(value = "/copyAll")
	public Result<String> copyAll(@RequestBody Map<String, Object> params) {
		String sourceProjectId = (String) params.get("sourceProjectId");
		String targetProjectId = (String) params.get("targetProjectId");

		if (sourceProjectId == null || targetProjectId == null) {
			return Result.error("参数不完整！");
		}

		try {
			// 检查目标项目是否已存在构型数据
			QueryWrapper<IetmProjectConfigurationManagement> targetQueryWrapper = new QueryWrapper<>();
			targetQueryWrapper.eq("project_id", targetProjectId);
			long targetCount = ietmProjectConfigurationManagementService.count(targetQueryWrapper);

			if (targetCount > 0) {
				return Result.OK("当前项目已存在构型数据，跳过复制操作！");
			}

			// 查询来源项目的所有构型
			QueryWrapper<IetmProjectConfigurationManagement> queryWrapper = new QueryWrapper<>();
			queryWrapper.eq("project_id", sourceProjectId);
			List<IetmProjectConfigurationManagement> sourceConfigs = ietmProjectConfigurationManagementService.list(queryWrapper);

			if (sourceConfigs.isEmpty()) {
				return Result.error("来源项目没有构型数据！");
			}

			// 复制到目标项目（需要保持树形结构）
			// 先复制根节点，再递归复制子节点
			int count = copyConfigTree(sourceConfigs, targetProjectId, "0", "0");

			return Result.OK("成功复制 " + count + " 条构型数据！");
		} catch (Exception e) {
			log.error("复制所有构型失败", e);
			return Result.error("复制所有构型失败：" + e.getMessage());
		}
	}

	/**
	 * 递归添加父节点到结果列表
	 */
	private void addParentNodes(String pid, List<IetmProjectConfigurationManagement> allNodes, List<IetmProjectConfigurationManagement> resultList) {
		if (pid == null || "0".equals(pid)) {
			return;
		}
		// 查找父节点
		for (IetmProjectConfigurationManagement node : allNodes) {
			if (node.getId().equals(pid)) {
				// 避免重复添加
				boolean exists = false;
				for (IetmProjectConfigurationManagement existing : resultList) {
					if (existing.getId().equals(node.getId())) {
						exists = true;
						break;
					}
				}
				if (!exists) {
					resultList.add(node);
					// 继续递归查找上级父节点
					addParentNodes(node.getPid(), allNodes, resultList);
				}
				break;
			}
		}
	}

	/**
	 * 递归复制构型树
	 */
	private int copyConfigTree(List<IetmProjectConfigurationManagement> allConfigs,
	                          String targetProjectId, String sourcePid, String targetPid) {
		int count = 0;
		// 查找当前父节点下的子节点
		List<IetmProjectConfigurationManagement> children = allConfigs.stream()
			.filter(c -> sourcePid.equals(c.getPid()))
			.collect(Collectors.toList());

		for (IetmProjectConfigurationManagement sourceConfig : children) {
			// 复制当前节点
			IetmProjectConfigurationManagement newConfig = new IetmProjectConfigurationManagement();
			org.springframework.beans.BeanUtils.copyProperties(sourceConfig, newConfig);
			String oldId = sourceConfig.getId();
			newConfig.setId(null); // 清空ID，让系统自动生成新ID
			newConfig.setProjectId(targetProjectId);
			newConfig.setPid(targetPid);
			ietmProjectConfigurationManagementService.save(newConfig);
			count++;

			// 递归复制子节点
			count += copyConfigTree(allConfigs, targetProjectId, oldId, newConfig.getId());
		}

		return count;
	}

	/**
	 * 验证编码是否重复
	 *
	 * @param code 编码
	 * @param pid 父节点ID
	 * @param id 当前节点ID（编辑时传入，新增时不传）
	 * @return
	 */
	@ApiOperation(value="项目管理-项目构型管理-验证编码", notes="项目管理-项目构型管理-验证编码")
	@GetMapping(value = "/checkCode")
	public Result<Boolean> checkCode(
			@RequestParam(name="code", required=true) String code,
			@RequestParam(name="pid", required=true) String pid,
			@RequestParam(name="projectId", required=true) String projectId,
			@RequestParam(name="id", required=false) String id) {
		try {
			boolean isDuplicate = ietmProjectConfigurationManagementService.checkCodeDuplicate(code, pid, projectId, id);
			if (isDuplicate) {
				return Result.error("同级节点下编码【" + code + "】已存在！");
			}
			return Result.OK(false);
		} catch (Exception e) {
			log.error("验证编码失败", e);
			return Result.error("验证编码失败：" + e.getMessage());
		}
	}

	/**
	 * 删除前检查
	 *
	 * @param id 节点ID
	 * @return
	 */
	@ApiOperation(value="项目管理-项目构型管理-删除前检查", notes="项目管理-项目构型管理-删除前检查")
	@GetMapping(value = "/checkDelete")
	public Result<String> checkDelete(@RequestParam(name="id", required=true) String id) {
		try {
			String errorMsg = ietmProjectConfigurationManagementService.checkCanDelete(id);
			if (errorMsg != null) {
				return Result.error(errorMsg);
			}
			return Result.OK("可以删除");
		} catch (Exception e) {
			log.error("删除检查失败", e);
			return Result.error("删除检查失败：" + e.getMessage());
		}
	}

	/**
	 * 批量生成路径
	 *
	 * @param projectId 项目ID
	 * @return
	 */
	@AutoLog(value = "项目管理-项目构型管理-批量生成路径")
	@ApiOperation(value="项目管理-项目构型管理-批量生成路径", notes="项目管理-项目构型管理-批量生成路径")
	@PostMapping(value = "/batchGeneratePaths")
	public Result<String> batchGeneratePaths(@RequestParam(name="projectId", required=true) String projectId) {
		try {
			log.info("接收到批量生成路径请求，项目ID: {}", projectId);

			// 获取当前登录用户
			LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

			// 权限校验：检查是否为管理员（批量操作需要管理员权限）
			boolean isAdmin = authCheckService.isAdmin(sysUser.getId());
			if (!isAdmin) {
				return Result.error("此操作需要管理员权限！");
			}

			int updateCount = ietmProjectConfigurationManagementService.batchGeneratePaths(projectId);

			if (updateCount == 0) {
				return Result.OK("该项目下没有需要更新的构型节点");
			}

			return Result.OK("批量生成路径成功，共更新 " + updateCount + " 个节点！");
		} catch (Exception e) {
			log.error("批量生成路径失败", e);
			return Result.error("批量生成路径失败：" + e.getMessage());
		}
	}

	/**
	 * 查询模板构型树
	 */
	@AutoLog(value = "项目构型管理-查询模板树")
	@ApiOperation(value="项目构型管理-查询模板树", notes="项目构型管理-查询模板树")
	@GetMapping(value = "/getTemplateTree")
	public Result<List<IetmProjectConfigurationManagement>> getTemplateTree(
			@RequestParam(name="standard", required=true) String standard,
			@RequestParam(name="equipType", required=true) String equipType) {
		try {
			log.info("查询模板构型树: standard={}, equipType={}", standard, equipType);

			List<IetmProjectConfigurationManagement> templateTree =
				ietmProjectConfigurationManagementService.getTemplateTree(standard, equipType);

			if (templateTree.isEmpty()) {
				return Result.error("未找到模板数据: TEMPLATE_" + standard + "_" + equipType);
			}

			return Result.OK(templateTree);
		} catch (Exception e) {
			log.error("查询模板构型树失败", e);
			return Result.error("查询模板构型树失败：" + e.getMessage());
		}
	}

	/**
	 * 从模板导入构型树
	 */
	@AutoLog(value = "项目构型管理-从模板导入")
	@ApiOperation(value="项目构型管理-从模板导入", notes="项目构型管理-从模板导入")
	@PostMapping(value = "/importFromTemplate")
	public Result<String> importFromTemplate(@RequestBody Map<String, String> params) {
		try {
			String projectId = params.get("projectId");
			String standard = params.get("standard");
			String equipType = params.get("equipType");

			log.info("从模板导入: projectId={}, standard={}, equipType={}", projectId, standard, equipType);

			// 校验参数
			if (projectId == null || projectId.isEmpty()) {
				return Result.error("项目ID不能为空");
			}
			if (standard == null || standard.isEmpty()) {
				return Result.error("IETM标准不能为空");
			}
			if (equipType == null || equipType.isEmpty()) {
				return Result.error("装备类型不能为空");
			}

			// 校验：检查当前项目是否已有构型数据（除了根节点）
			QueryWrapper<IetmProjectConfigurationManagement> checkWrapper = new QueryWrapper<>();
			checkWrapper.eq("project_id", projectId);
			checkWrapper.ne("pid", "0");  // 不是根节点
			long existingCount = ietmProjectConfigurationManagementService.count(checkWrapper);

			if (existingCount > 0) {
				return Result.error("已有构型数据，不能再导入！");
			}

			int importedCount = ietmProjectConfigurationManagementService.importFromTemplate(
				projectId, standard, equipType
			);

			return Result.OK("从模板导入成功，共导入 " + importedCount + " 个节点！");
		} catch (JeecgBootException e) {
			log.error("从模板导入失败", e);
			return Result.error(e.getMessage());
		} catch (Exception e) {
			log.error("从模板导入失败", e);
			return Result.error("从模板导入失败：" + e.getMessage());
		}
	}

	/**
	 * 校验Excel导入数据
	 * @param dataList Excel数据列表
	 * @param projectId 项目ID
	 * @return 校验结果
	 */
	@AutoLog(value = "项目构型管理-校验Excel导入数据")
	@ApiOperation(value="项目构型管理-校验Excel导入数据", notes="项目构型管理-校验Excel导入数据")
	@PostMapping(value = "/validateExcelData")
	public Result<?> validateExcelData(
		@RequestBody List<org.jeecg.modules.ietm.projectconfigurationmanagement.dto.IetmProjectCmExcelDTO> dataList,
		@RequestParam(name = "projectId", required = true) String projectId
	) {
		try {
			log.info("=== 校验Excel导入数据 ===");
			log.info("projectId: {}", projectId);
			log.info("数据条数: {}", dataList.size());

			if (oConvertUtils.isEmpty(projectId)) {
				return Result.error("项目ID不能为空！");
			}

			if (dataList == null || dataList.isEmpty()) {
				return Result.error("导入数据不能为空！");
			}

			// 调用Service进行校验
			List<org.jeecg.modules.ietm.projectconfigurationmanagement.dto.IetmProjectCmExcelDTO> validatedList =
				ietmProjectConfigurationManagementService.validateExcelData(dataList, projectId);

			// 统计校验结果
			long validCount = validatedList.stream().filter(d -> d.isValid()).count();
			long invalidCount = validatedList.size() - validCount;

			log.info("校验完成：有效{}条，无效{}条", validCount, invalidCount);

			Map<String, Object> resultMap = new HashMap<>();
			resultMap.put("dataList", validatedList);
			resultMap.put("validCount", validCount);
			resultMap.put("invalidCount", invalidCount);
			resultMap.put("totalCount", validatedList.size());

			if (invalidCount > 0) {
				return Result.OK("数据校验完成，发现 " + invalidCount + " 条错误数据！", resultMap);
			} else {
				return Result.OK("数据校验通过，可以导入！", resultMap);
			}

		} catch (JeecgBootException e) {
			log.error("校验Excel数据失败", e);
			return Result.error(e.getMessage());
		} catch (Exception e) {
			log.error("校验Excel数据失败", e);
			return Result.error("校验失败：" + e.getMessage());
		}
	}

	/**
	 * 导入Excel数据
	 * @param dataList 校验通过的Excel数据列表
	 * @param projectId 项目ID
	 * @return 导入结果
	 */
	@AutoLog(value = "项目构型管理-导入Excel数据")
	@ApiOperation(value="项目构型管理-导入Excel数据", notes="项目构型管理-导入Excel数据")
	@PostMapping(value = "/importExcelData")
	public Result<?> importExcelData(
		@RequestBody List<org.jeecg.modules.ietm.projectconfigurationmanagement.dto.IetmProjectCmExcelDTO> dataList,
		@RequestParam(name = "projectId", required = true) String projectId,
		@RequestParam(name = "security", required = false) Integer security
	) {
		try {
			log.info("=== 导入Excel数据 ===");
			log.info("projectId: {}, security: {}", projectId, security);
			log.info("数据条数: {}", dataList.size());

			if (oConvertUtils.isEmpty(projectId)) {
				return Result.error("项目ID不能为空！");
			}

			if (dataList == null || dataList.isEmpty()) {
				return Result.error("导入数据不能为空！");
			}

			// 过滤出校验通过的数据
			List<org.jeecg.modules.ietm.projectconfigurationmanagement.dto.IetmProjectCmExcelDTO> validList =
				dataList.stream().filter(d -> d.isValid()).collect(Collectors.toList());

			if (validList.isEmpty()) {
				return Result.error("没有有效数据可以导入！");
			}

			// 调用Service进行导入
			int importedCount = ietmProjectConfigurationManagementService.importExcelData(validList, projectId, security);

			log.info("导入完成，共导入{}条数据", importedCount);

			// 返回详细结果
			Map<String, Object> resultMap = new HashMap<>();
			resultMap.put("successCount", importedCount);
			resultMap.put("totalCount", validList.size());
			resultMap.put("skipCount", validList.size() - importedCount);

			return Result.OK("导入成功，共导入 " + importedCount + " 条数据！", resultMap);

		} catch (JeecgBootException e) {
			log.error("导入Excel数据失败", e);
			return Result.error(e.getMessage());
		} catch (Exception e) {
			log.error("导入Excel数据失败", e);
			return Result.error("导入失败：" + e.getMessage());
		}
	}

}
