package org.jeecg.modules.ietm.ietmattachment.controller;

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
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.service.IIetmAttachmentService;

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
 * @Description: 附件表
 * @Author: jeecg-boot
 * @Date:   2026-03-03
 * @Version: V1.0
 */
@Api(tags="附件表")
@RestController
@RequestMapping("/ietmattachment/ietmAttachment")
@Slf4j
public class IetmAttachmentController extends JeecgController<IetmAttachment, IIetmAttachmentService> {
	@Autowired
	private IIetmAttachmentService ietmAttachmentService;
	
	/**
	 * 分页列表查询
	 *
	 * @param ietmAttachment
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "附件表-分页列表查询")
	@ApiOperation(value="附件表-分页列表查询", notes="附件表-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<IetmAttachment>> queryPageList(IetmAttachment ietmAttachment,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<IetmAttachment> queryWrapper = QueryGenerator.initQueryWrapper(ietmAttachment, req.getParameterMap());
		Page<IetmAttachment> page = new Page<IetmAttachment>(pageNo, pageSize);
		IPage<IetmAttachment> pageList = ietmAttachmentService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param ietmAttachment
	 * @return
	 */
	@AutoLog(value = "附件表-添加")
	@ApiOperation(value="附件表-添加", notes="附件表-添加")
	//@RequiresPermissions("ietmattachment:ietm_attachment:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody IetmAttachment ietmAttachment) {
		ietmAttachmentService.save(ietmAttachment);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param ietmAttachment
	 * @return
	 */
	@AutoLog(value = "附件表-编辑")
	@ApiOperation(value="附件表-编辑", notes="附件表-编辑")
	//@RequiresPermissions("ietmattachment:ietm_attachment:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody IetmAttachment ietmAttachment) {
		ietmAttachmentService.updateById(ietmAttachment);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "附件表-通过id删除")
	@ApiOperation(value="附件表-通过id删除", notes="附件表-通过id删除")
	//@RequiresPermissions("ietmattachment:ietm_attachment:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		ietmAttachmentService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "附件表-批量删除")
	@ApiOperation(value="附件表-批量删除", notes="附件表-批量删除")
	//@RequiresPermissions("ietmattachment:ietm_attachment:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.ietmAttachmentService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "附件表-通过id查询")
	@ApiOperation(value="附件表-通过id查询", notes="附件表-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<IetmAttachment> queryById(@RequestParam(name="id",required=true) String id) {
		IetmAttachment ietmAttachment = ietmAttachmentService.getById(id);
		if(ietmAttachment==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(ietmAttachment);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param ietmAttachment
    */
    //@RequiresPermissions("ietmattachment:ietm_attachment:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, IetmAttachment ietmAttachment) {
        return super.exportXls(request, ietmAttachment, IetmAttachment.class, "附件表");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    //@RequiresPermissions("ietmattachment:ietm_attachment:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, IetmAttachment.class);
    }

}
