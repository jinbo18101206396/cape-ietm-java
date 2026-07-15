package org.jeecg.modules.ietm.projectinformationcode.controller;

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
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.ietm.projectinformationcode.entity.IetmProjectInformationCode;
import org.jeecg.modules.ietm.projectinformationcode.service.IIetmProjectInformationCodeService;

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
 * @Description: 项目管理-项目信息码管理
 * @Author: jeecg-boot
 * @Date:   2026-01-12
 * @Version: V1.0
 */
@Api(tags="项目管理-项目信息码管理")
@RestController
@RequestMapping("/projectinformationcode/ietmProjectInformationCode")
@Slf4j
public class IetmProjectInformationCodeController extends JeecgController<IetmProjectInformationCode, IIetmProjectInformationCodeService> {
	@Autowired
	private IIetmProjectInformationCodeService ietmProjectInformationCodeService;
	
	/**
	 * 分页列表查询
	 *
	 * @param ietmProjectInformationCode
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "项目管理-项目信息码管理-分页列表查询")
	@ApiOperation(value="项目管理-项目信息码管理-分页列表查询", notes="项目管理-项目信息码管理-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<IetmProjectInformationCode>> queryPageList(IetmProjectInformationCode ietmProjectInformationCode,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<IetmProjectInformationCode> queryWrapper = QueryGenerator.initQueryWrapper(ietmProjectInformationCode, req.getParameterMap());
		Page<IetmProjectInformationCode> page = new Page<IetmProjectInformationCode>(pageNo, pageSize);
		IPage<IetmProjectInformationCode> pageList = ietmProjectInformationCodeService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param ietmProjectInformationCode
	 * @return
	 */
	@AutoLog(value = "项目管理-项目信息码管理-添加")
	@ApiOperation(value="项目管理-项目信息码管理-添加", notes="项目管理-项目信息码管理-添加")
	//@RequiresPermissions("projectinformationcode:ietm_project_information_code:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody IetmProjectInformationCode ietmProjectInformationCode) {
		ietmProjectInformationCodeService.save(ietmProjectInformationCode);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param ietmProjectInformationCode
	 * @return
	 */
	@AutoLog(value = "项目管理-项目信息码管理-编辑")
	@ApiOperation(value="项目管理-项目信息码管理-编辑", notes="项目管理-项目信息码管理-编辑")
	//@RequiresPermissions("projectinformationcode:ietm_project_information_code:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody IetmProjectInformationCode ietmProjectInformationCode) {
		ietmProjectInformationCodeService.updateById(ietmProjectInformationCode);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "项目管理-项目信息码管理-通过id删除")
	@ApiOperation(value="项目管理-项目信息码管理-通过id删除", notes="项目管理-项目信息码管理-通过id删除")
	//@RequiresPermissions("projectinformationcode:ietm_project_information_code:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		ietmProjectInformationCodeService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "项目管理-项目信息码管理-批量删除")
	@ApiOperation(value="项目管理-项目信息码管理-批量删除", notes="项目管理-项目信息码管理-批量删除")
	//@RequiresPermissions("projectinformationcode:ietm_project_information_code:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.ietmProjectInformationCodeService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "项目管理-项目信息码管理-通过id查询")
	@ApiOperation(value="项目管理-项目信息码管理-通过id查询", notes="项目管理-项目信息码管理-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<IetmProjectInformationCode> queryById(@RequestParam(name="id",required=true) String id) {
		IetmProjectInformationCode ietmProjectInformationCode = ietmProjectInformationCodeService.getById(id);
		if(ietmProjectInformationCode==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(ietmProjectInformationCode);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param ietmProjectInformationCode
    */
    //@RequiresPermissions("projectinformationcode:ietm_project_information_code:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, IetmProjectInformationCode ietmProjectInformationCode) {
        return super.exportXls(request, ietmProjectInformationCode, IetmProjectInformationCode.class, "项目管理-项目信息码管理");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    //@RequiresPermissions("projectinformationcode:ietm_project_information_code:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, IetmProjectInformationCode.class);
    }

}
