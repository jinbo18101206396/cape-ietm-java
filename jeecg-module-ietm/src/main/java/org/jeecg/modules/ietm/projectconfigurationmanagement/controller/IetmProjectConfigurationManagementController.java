package org.jeecg.modules.ietm.projectconfigurationmanagement.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
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
		String hasQuery = req.getParameter("hasQuery");
        if(hasQuery != null && "true".equals(hasQuery)){
            QueryWrapper<IetmProjectConfigurationManagement> queryWrapper =  QueryGenerator.initQueryWrapper(ietmProjectConfigurationManagement, req.getParameterMap());
            List<IetmProjectConfigurationManagement> list = ietmProjectConfigurationManagementService.queryTreeListNoPage(queryWrapper);
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
            Page<IetmProjectConfigurationManagement> page = new Page<IetmProjectConfigurationManagement>(pageNo, pageSize);
            IPage<IetmProjectConfigurationManagement> pageList = ietmProjectConfigurationManagementService.page(page, queryWrapper);
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
		QueryWrapper queryWrapper = new QueryWrapper();
		queryWrapper.eq("project_id", projectId);
		List<IetmProjectConfigurationManagement> list = ietmProjectConfigurationManagementService.list(queryWrapper);
		log.info("查询到构型数据数量: {}", list.size());
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
		List<IetmProjectConfigurationManagement> list = ietmProjectConfigurationManagementService.list(queryWrapper);
		IPage<IetmProjectConfigurationManagement> pageList = new Page<>(1, 10, list.size());
        pageList.setRecords(list);
		return Result.OK(pageList);
	}

    /**
      * 批量查询子节点
      * @param parentIds 父ID（多个采用半角逗号分割）
      * @return 返回 IPage
      * @param parentIds
      * @return
      */
	//@AutoLog(value = "项目管理-项目构型管理-批量获取子数据")
    @ApiOperation(value="项目管理-项目构型管理-批量获取子数据", notes="项目管理-项目构型管理-批量获取子数据")
    @GetMapping("/getChildListBatch")
    public Result getChildListBatch(@RequestParam("parentIds") String parentIds) {
        try {
            QueryWrapper<IetmProjectConfigurationManagement> queryWrapper = new QueryWrapper<>();
            List<String> parentIdList = Arrays.asList(parentIds.split(","));
            queryWrapper.in("pid", parentIdList);
            List<IetmProjectConfigurationManagement> list = ietmProjectConfigurationManagementService.list(queryWrapper);
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

}
