package org.jeecg.modules.ietm.standardmanagement.controller;

import org.jeecg.common.system.query.QueryGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.api.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import java.util.Arrays;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.ietm.standardmanagement.entity.IetmStandardDmtype;
import org.jeecg.modules.ietm.standardmanagement.entity.IetmStandard;
import org.jeecg.modules.ietm.standardmanagement.service.IIetmStandardService;
import org.jeecg.modules.ietm.standardmanagement.service.IIetmStandardDmtypeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.shiro.authz.annotation.RequiresPermissions;

 /**
 * @Description: 手册管理-标准管理左侧树
 * @Author: jeecg-boot
 * @Date:   2026-01-08
 * @Version: V1.0
 */
@Api(tags="手册管理-标准管理左侧树")
@RestController
@RequestMapping("/standardmanagement/ietmStandard")
@Slf4j
public class IetmStandardController extends JeecgController<IetmStandard, IIetmStandardService> {

	@Autowired
	private IIetmStandardService ietmStandardService;

	@Autowired
	private IIetmStandardDmtypeService ietmStandardDmtypeService;


	/*---------------------------------主表处理-begin-------------------------------------*/

	/**
	 * 分页列表查询
	 * @param ietmStandard
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "手册管理-标准管理左侧树-分页列表查询")
	@ApiOperation(value="手册管理-标准管理左侧树-分页列表查询", notes="手册管理-标准管理左侧树-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<IetmStandard>> queryPageList(IetmStandard ietmStandard,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<IetmStandard> queryWrapper = QueryGenerator.initQueryWrapper(ietmStandard, req.getParameterMap());
		Page<IetmStandard> page = new Page<IetmStandard>(pageNo, pageSize);
		IPage<IetmStandard> pageList = ietmStandardService.page(page, queryWrapper);
		return Result.OK(pageList);
	}

	/**
     *   添加
     * @param ietmStandard
     * @return
     */
    @AutoLog(value = "手册管理-标准管理左侧树-添加")
    @ApiOperation(value="手册管理-标准管理左侧树-添加", notes="手册管理-标准管理左侧树-添加")
    //@RequiresPermissions("standardmanagement:ietm_standard:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody IetmStandard ietmStandard) {
        ietmStandardService.save(ietmStandard);
        return Result.OK("添加成功！");
    }

    /**
     *  编辑
     * @param ietmStandard
     * @return
     */
    @AutoLog(value = "手册管理-标准管理左侧树-编辑")
    @ApiOperation(value="手册管理-标准管理左侧树-编辑", notes="手册管理-标准管理左侧树-编辑")
    //@RequiresPermissions("standardmanagement:ietm_standard:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
    public Result<String> edit(@RequestBody IetmStandard ietmStandard) {
        ietmStandardService.updateById(ietmStandard);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     * @param id
     * @return
     */
    @AutoLog(value = "手册管理-标准管理左侧树-通过id删除")
    @ApiOperation(value="手册管理-标准管理左侧树-通过id删除", notes="手册管理-标准管理左侧树-通过id删除")
    //@RequiresPermissions("standardmanagement:ietm_standard:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name="id",required=true) String id) {
        ietmStandardService.delMain(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @AutoLog(value = "手册管理-标准管理左侧树-批量删除")
    @ApiOperation(value="手册管理-标准管理左侧树-批量删除", notes="手册管理-标准管理左侧树-批量删除")
    //@RequiresPermissions("standardmanagement:ietm_standard:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
        this.ietmStandardService.delBatchMain(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 导出
     * @return
     */
    //@RequiresPermissions("standardmanagement:ietm_standard:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, IetmStandard ietmStandard) {
        return super.exportXls(request, ietmStandard, IetmStandard.class, "手册管理-标准管理左侧树");
    }

    /**
     * 导入
     * @return
     */
    //@RequiresPermissions("standardmanagement:ietm_standard:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, IetmStandard.class);
    }
	/*---------------------------------主表处理-end-------------------------------------*/
	

    /*--------------------------------子表处理-手册管理-标准管理列表（标准数据模块）-begin----------------------------------------------*/
	/**
	 * 通过主表ID查询
	 * @return
	 */
	//@AutoLog(value = "手册管理-标准管理列表（标准数据模块）-通过主表ID查询")
	@ApiOperation(value="手册管理-标准管理列表（标准数据模块）-通过主表ID查询", notes="手册管理-标准管理列表（标准数据模块）-通过主表ID查询")
	@GetMapping(value = "/listIetmStandardDmtypeByMainId")
    public Result<IPage<IetmStandardDmtype>> listIetmStandardDmtypeByMainId(IetmStandardDmtype ietmStandardDmtype,
                                                    @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                    HttpServletRequest req) {
        QueryWrapper<IetmStandardDmtype> queryWrapper = QueryGenerator.initQueryWrapper(ietmStandardDmtype, req.getParameterMap());
        Page<IetmStandardDmtype> page = new Page<IetmStandardDmtype>(pageNo, pageSize);
        IPage<IetmStandardDmtype> pageList = ietmStandardDmtypeService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

	/**
	 * 添加
	 * @param ietmStandardDmtype
	 * @return
	 */
	@AutoLog(value = "手册管理-标准管理列表（标准数据模块）-添加")
	@ApiOperation(value="手册管理-标准管理列表（标准数据模块）-添加", notes="手册管理-标准管理列表（标准数据模块）-添加")
	@PostMapping(value = "/addIetmStandardDmtype")
	public Result<String> addIetmStandardDmtype(@RequestBody IetmStandardDmtype ietmStandardDmtype) {
		ietmStandardDmtypeService.save(ietmStandardDmtype);
		return Result.OK("添加成功！");
	}

    /**
	 * 编辑
	 * @param ietmStandardDmtype
	 * @return
	 */
	@AutoLog(value = "手册管理-标准管理列表（标准数据模块）-编辑")
	@ApiOperation(value="手册管理-标准管理列表（标准数据模块）-编辑", notes="手册管理-标准管理列表（标准数据模块）-编辑")
	@RequestMapping(value = "/editIetmStandardDmtype", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> editIetmStandardDmtype(@RequestBody IetmStandardDmtype ietmStandardDmtype) {
		ietmStandardDmtypeService.updateById(ietmStandardDmtype);
		return Result.OK("编辑成功!");
	}

	/**
	 * 通过id删除
	 * @param id
	 * @return
	 */
	@AutoLog(value = "手册管理-标准管理列表（标准数据模块）-通过id删除")
	@ApiOperation(value="手册管理-标准管理列表（标准数据模块）-通过id删除", notes="手册管理-标准管理列表（标准数据模块）-通过id删除")
	@DeleteMapping(value = "/deleteIetmStandardDmtype")
	public Result<String> deleteIetmStandardDmtype(@RequestParam(name="id",required=true) String id) {
		ietmStandardDmtypeService.removeById(id);
		return Result.OK("删除成功!");
	}

	/**
	 * 批量删除
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "手册管理-标准管理列表（标准数据模块）-批量删除")
	@ApiOperation(value="手册管理-标准管理列表（标准数据模块）-批量删除", notes="手册管理-标准管理列表（标准数据模块）-批量删除")
	@DeleteMapping(value = "/deleteBatchIetmStandardDmtype")
	public Result<String> deleteBatchIetmStandardDmtype(@RequestParam(name="ids",required=true) String ids) {
	    this.ietmStandardDmtypeService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}

    /**
     * 导出
     * @return
     */
    @RequestMapping(value = "/exportIetmStandardDmtype")
    public ModelAndView exportIetmStandardDmtype(HttpServletRequest request, IetmStandardDmtype ietmStandardDmtype) {
		 // Step.1 组装查询条件
		 QueryWrapper<IetmStandardDmtype> queryWrapper = QueryGenerator.initQueryWrapper(ietmStandardDmtype, request.getParameterMap());
		 LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

		 // Step.2 获取导出数据
		 List<IetmStandardDmtype> pageList = ietmStandardDmtypeService.list(queryWrapper);
		 List<IetmStandardDmtype> exportList = null;

		 // 过滤选中数据
		 String selections = request.getParameter("selections");
		 if (oConvertUtils.isNotEmpty(selections)) {
			 List<String> selectionList = Arrays.asList(selections.split(","));
			 exportList = pageList.stream().filter(item -> selectionList.contains(item.getId())).collect(Collectors.toList());
		 } else {
			 exportList = pageList;
		 }

		 // Step.3 AutoPoi 导出Excel
		 ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
		 //此处设置的filename无效,前端会重更新设置一下
		 mv.addObject(NormalExcelConstants.FILE_NAME, "手册管理-标准管理列表（标准数据模块）");
		 mv.addObject(NormalExcelConstants.CLASS, IetmStandardDmtype.class);
		 mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("手册管理-标准管理列表（标准数据模块）报表", "导出人:" + sysUser.getRealname(), "手册管理-标准管理列表（标准数据模块）"));
		 mv.addObject(NormalExcelConstants.DATA_LIST, exportList);
		 return mv;
    }

    /**
     * 导入
     * @return
     */
    @RequestMapping(value = "/importIetmStandardDmtype/{mainId}")
    public Result<?> importIetmStandardDmtype(HttpServletRequest request, HttpServletResponse response, @PathVariable("mainId") String mainId) {
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
				 List<IetmStandardDmtype> list = ExcelImportUtil.importExcel(file.getInputStream(), IetmStandardDmtype.class, params);
				 for (IetmStandardDmtype temp : list) {
                    temp.setPid(mainId);
				 }
				 long start = System.currentTimeMillis();
				 ietmStandardDmtypeService.saveBatch(list);
				 log.info("消耗时间" + (System.currentTimeMillis() - start) + "毫秒");
				 return Result.OK("文件导入成功！数据行数：" + list.size());
			 } catch (Exception e) {
				 log.error(e.getMessage(), e);
				 return Result.error("文件导入失败:" + e.getMessage());
			 } finally {
				 try {
					 file.getInputStream().close();
				 } catch (IOException e) {
					 e.printStackTrace();
				 }
			 }
		 }
		 return Result.error("文件导入失败！");
    }

    /*--------------------------------子表处理-手册管理-标准管理列表（标准数据模块）-end----------------------------------------------*/




}
