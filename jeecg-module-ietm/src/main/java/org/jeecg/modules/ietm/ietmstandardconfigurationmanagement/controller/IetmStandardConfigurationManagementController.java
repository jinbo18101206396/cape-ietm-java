package org.jeecg.modules.ietm.ietmstandardconfigurationmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.ietm.ietmstandardconfigurationmanagement.entity.IetmStandardConfigurationManagement;
import org.jeecg.modules.ietm.ietmstandardconfigurationmanagement.service.IIetmStandardConfigurationManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

 /**
 * @Description: 预制模板-构型管理
 * @Author: jeecg-boot
 * @Date:   2026-01-07
 * @Version: V1.0
 */
@Api(tags="预制模板-构型管理")
@RestController
@RequestMapping("/ietmstandardconfigurationmanagement/ietmStandardConfigurationManagement")
@Slf4j
public class IetmStandardConfigurationManagementController extends JeecgController<IetmStandardConfigurationManagement, IIetmStandardConfigurationManagementService>{
	@Autowired
	private IIetmStandardConfigurationManagementService ietmStandardConfigurationManagementService;

	/**
	 * 分页列表查询
	 *
	 * @param ietmStandardConfigurationManagement
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "预制模板-构型管理-分页列表查询")
	@ApiOperation(value="预制模板-构型管理-分页列表查询", notes="预制模板-构型管理-分页列表查询")
	@GetMapping(value = "/rootList")
	public Result<IPage<IetmStandardConfigurationManagement>> queryPageList(IetmStandardConfigurationManagement ietmStandardConfigurationManagement,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		String hasQuery = req.getParameter("hasQuery");
        if(hasQuery != null && "true".equals(hasQuery)){
            QueryWrapper<IetmStandardConfigurationManagement> queryWrapper =  QueryGenerator.initQueryWrapper(ietmStandardConfigurationManagement, req.getParameterMap());
            List<IetmStandardConfigurationManagement> list = ietmStandardConfigurationManagementService.queryTreeListNoPage(queryWrapper);
			ietmStandardConfigurationManagementService.setWay(list);
            IPage<IetmStandardConfigurationManagement> pageList = new Page<>(1, 10, list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        }else{
            String parentId = ietmStandardConfigurationManagement.getPid();
            if (oConvertUtils.isEmpty(parentId)) {
                parentId = "0";
            }
            ietmStandardConfigurationManagement.setPid(null);
            QueryWrapper<IetmStandardConfigurationManagement> queryWrapper = QueryGenerator.initQueryWrapper(ietmStandardConfigurationManagement, req.getParameterMap());
            // 使用 eq 防止模糊查询
            queryWrapper.eq("pid", parentId);
            Page<IetmStandardConfigurationManagement> page = new Page<IetmStandardConfigurationManagement>(pageNo, pageSize);
            IPage<IetmStandardConfigurationManagement> pageList = ietmStandardConfigurationManagementService.page(page, queryWrapper);
			ietmStandardConfigurationManagementService.setWay(pageList.getRecords());
            return Result.OK(pageList);
        }
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
			 List<SelectTreeModel> ls = ietmStandardConfigurationManagementService.queryListByPid(pid);
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
			 List<SelectTreeModel> ls = ietmStandardConfigurationManagementService.queryListByCode(pcode);
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
			 List<SelectTreeModel> temp = ietmStandardConfigurationManagementService.queryListByPid(tsm.getKey());
			 if (temp != null && temp.size() > 0) {
				 tsm.setChildren(temp);
				 loadAllChildren(temp);
			 }
		 }
	 }

	 /**
      * 获取子数据
      * @param ietmStandardConfigurationManagement
      * @param req
      * @return
      */
	//@AutoLog(value = "预制模板-构型管理-获取子数据")
	@ApiOperation(value="预制模板-构型管理-获取子数据", notes="预制模板-构型管理-获取子数据")
	@GetMapping(value = "/childList")
	public Result<IPage<IetmStandardConfigurationManagement>> queryPageList(IetmStandardConfigurationManagement ietmStandardConfigurationManagement,HttpServletRequest req) {
		QueryWrapper<IetmStandardConfigurationManagement> queryWrapper = QueryGenerator.initQueryWrapper(ietmStandardConfigurationManagement, req.getParameterMap());
		List<IetmStandardConfigurationManagement> list = ietmStandardConfigurationManagementService.list(queryWrapper);
		ietmStandardConfigurationManagementService.setWay(list);
		IPage<IetmStandardConfigurationManagement> pageList = new Page<>(1, 10, list.size());
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
	//@AutoLog(value = "预制模板-构型管理-批量获取子数据")
    @ApiOperation(value="预制模板-构型管理-批量获取子数据", notes="预制模板-构型管理-批量获取子数据")
    @GetMapping("/getChildListBatch")
    public Result getChildListBatch(@RequestParam("parentIds") String parentIds,
									@RequestParam("isorterColumn") String isorterColumn,
									@RequestParam("isorterOrder") String isorterOrder
									) {
        try {
            QueryWrapper<IetmStandardConfigurationManagement> queryWrapper = new QueryWrapper<>();
            List<String> parentIdList = Arrays.asList(parentIds.split(","));
            queryWrapper.in("pid", parentIdList);
			if ("asc".equals(isorterOrder)) {
				queryWrapper.orderByAsc(isorterColumn);
			}else if ("desc".equals(isorterOrder)) {
				queryWrapper.orderByDesc(isorterColumn);
			}
            List<IetmStandardConfigurationManagement> list = ietmStandardConfigurationManagementService.list(queryWrapper);
			ietmStandardConfigurationManagementService.setWay(list);
            IPage<IetmStandardConfigurationManagement> pageList = new Page<>(1, 10, list.size());
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
	 * @param ietmStandardConfigurationManagement
	 * @return
	 */
	@AutoLog(value = "预制模板-构型管理-添加")
	@ApiOperation(value="预制模板-构型管理-添加", notes="预制模板-构型管理-添加")
    //@RequiresPermissions("ietmstandardconfigurationmanagement:ietm_standard_configuration_management:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody IetmStandardConfigurationManagement ietmStandardConfigurationManagement) {
		ietmStandardConfigurationManagementService.addIetmStandardConfigurationManagement(ietmStandardConfigurationManagement);
		return Result.OK("添加成功！");
	}

	/**
	 *  编辑
	 *
	 * @param ietmStandardConfigurationManagement
	 * @return
	 */
	@AutoLog(value = "预制模板-构型管理-编辑")
	@ApiOperation(value="预制模板-构型管理-编辑", notes="预制模板-构型管理-编辑")
    //@RequiresPermissions("ietmstandardconfigurationmanagement:ietm_standard_configuration_management:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody IetmStandardConfigurationManagement ietmStandardConfigurationManagement) {
		ietmStandardConfigurationManagementService.updateIetmStandardConfigurationManagement(ietmStandardConfigurationManagement);
		return Result.OK("编辑成功!");
	}

	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "预制模板-构型管理-通过id删除")
	@ApiOperation(value="预制模板-构型管理-通过id删除", notes="预制模板-构型管理-通过id删除")
    //@RequiresPermissions("ietmstandardconfigurationmanagement:ietm_standard_configuration_management:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		ietmStandardConfigurationManagementService.deleteIetmStandardConfigurationManagement(id);
		return Result.OK("删除成功!");
	}

	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "预制模板-构型管理-批量删除")
	@ApiOperation(value="预制模板-构型管理-批量删除", notes="预制模板-构型管理-批量删除")
    //@RequiresPermissions("ietmstandardconfigurationmanagement:ietm_standard_configuration_management:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.ietmStandardConfigurationManagementService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}

	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "预制模板-构型管理-通过id查询")
	@ApiOperation(value="预制模板-构型管理-通过id查询", notes="预制模板-构型管理-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<IetmStandardConfigurationManagement> queryById(@RequestParam(name="id",required=true) String id) {
		IetmStandardConfigurationManagement ietmStandardConfigurationManagement = ietmStandardConfigurationManagementService.getById(id);
		if(ietmStandardConfigurationManagement==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(ietmStandardConfigurationManagement);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param ietmStandardConfigurationManagement
    */
    //@RequiresPermissions("ietmstandardconfigurationmanagement:ietm_standard_configuration_management:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, IetmStandardConfigurationManagement ietmStandardConfigurationManagement) {
		return super.exportXls(request, ietmStandardConfigurationManagement, IetmStandardConfigurationManagement.class, "预制模板-构型管理");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    //@RequiresPermissions("ietmstandardconfigurationmanagement:ietm_standard_configuration_management:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
		return super.importExcel(request, response, IetmStandardConfigurationManagement.class);
    }


	 /**
	  *
	  * @param pid
	  * @return
	  */
	 @ApiOperation(value="预制模板-构型管理-code示例", notes="预制模板-构型管理-code示例")
	 @GetMapping("/getCodeTemp")
	 public Result getCodeTemp(@RequestParam("pid") String pid){
		Integer num = ietmStandardConfigurationManagementService.getCodeTemp(pid);
		 return Result.OK(num);
	 }
 }
