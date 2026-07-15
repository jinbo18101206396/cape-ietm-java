package org.jeecg.modules.ietm.ietmdatamodulemanagement.controller;

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
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

 /**
 * @Description: 项目管理-项目数据模块管理
 * @Author: jeecg-boot
 * @Date:   2026-03-10
 * @Version: V1.0
 */
@Api(tags="项目管理-项目数据模块管理")
@RestController
@RequestMapping("/itemprojectdatamodule/ietmDataModule")
@Slf4j
public class IetmDataModuleController extends JeecgController<IetmDataModule, IIetmDataModuleService> {
	@Autowired
	private IIetmDataModuleService ietmDataModuleService;

	/**
	 * 分页列表查询
	 *
	 * @param ietmDataModule
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "项目管理-项目数据模块管理-分页列表查询")
	@ApiOperation(value="项目管理-项目数据模块管理-分页列表查询", notes="项目管理-项目数据模块管理-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<IetmDataModule>> queryPageList(IetmDataModule ietmDataModule,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<IetmDataModule> queryWrapper = QueryGenerator.initQueryWrapper(ietmDataModule, req.getParameterMap());
		Page<IetmDataModule> page = new Page<IetmDataModule>(pageNo, pageSize);
		IPage<IetmDataModule> pageList = ietmDataModuleService.page(page, queryWrapper);
		return Result.OK(pageList);
	}

	/**
	 *   添加
	 *
	 * @param ietmDataModule
	 * @return
	 */
	@AutoLog(value = "项目管理-项目数据模块管理-添加")
	@ApiOperation(value="项目管理-项目数据模块管理-添加", notes="项目管理-项目数据模块管理-添加")
	//@RequiresPermissions("itemprojectdatamodule:ietm_data_module:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody IetmDataModule ietmDataModule) {
		ietmDataModuleService.save(ietmDataModule);
		return Result.OK("添加成功！");
	}

	/**
	 *  编辑
	 *
	 * @param ietmDataModule
	 * @return
	 */
	@AutoLog(value = "项目管理-项目数据模块管理-编辑")
	@ApiOperation(value="项目管理-项目数据模块管理-编辑", notes="项目管理-项目数据模块管理-编辑")
	//@RequiresPermissions("itemprojectdatamodule:ietm_data_module:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody IetmDataModule ietmDataModule) {
		ietmDataModuleService.updateById(ietmDataModule);
		return Result.OK("编辑成功!");
	}

	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "项目管理-项目数据模块管理-通过id删除")
	@ApiOperation(value="项目管理-项目数据模块管理-通过id删除", notes="项目管理-项目数据模块管理-通过id删除")
	//@RequiresPermissions("itemprojectdatamodule:ietm_data_module:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		ietmDataModuleService.removeById(id);
		return Result.OK("删除成功!");
	}

	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "项目管理-项目数据模块管理-批量删除")
	@ApiOperation(value="项目管理-项目数据模块管理-批量删除", notes="项目管理-项目数据模块管理-批量删除")
	//@RequiresPermissions("itemprojectdatamodule:ietm_data_module:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.ietmDataModuleService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}

	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "项目管理-项目数据模块管理-通过id查询")
	@ApiOperation(value="项目管理-项目数据模块管理-通过id查询", notes="项目管理-项目数据模块管理-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<IetmDataModule> queryById(@RequestParam(name="id",required=true) String id) {
		IetmDataModule ietmDataModule = ietmDataModuleService.getById(id);
		if(ietmDataModule==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(ietmDataModule);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param ietmDataModule
    */
    //@RequiresPermissions("itemprojectdatamodule:ietm_data_module:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, IetmDataModule ietmDataModule) {
        return super.exportXls(request, ietmDataModule, IetmDataModule.class, "项目管理-项目数据模块管理");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    //@RequiresPermissions("itemprojectdatamodule:ietm_data_module:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, IetmDataModule.class);
    }

}
