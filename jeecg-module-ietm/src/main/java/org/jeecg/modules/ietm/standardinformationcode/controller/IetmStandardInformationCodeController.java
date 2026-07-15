package org.jeecg.modules.ietm.standardinformationcode.controller;

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
import org.jeecg.modules.ietm.standardinformationcode.entity.IetmStandardInformationCode;
import org.jeecg.modules.ietm.standardinformationcode.service.IIetmStandardInformationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * @Description: 手册预置模板-信息码管理
 * @Author: jeecg-boot
 * @Date: 2026-07-13
 * @Version: V1.0
 */
@Api(tags = "手册预置模板-信息码管理")
@RestController
@RequestMapping("/standardinformationcode/ietmStandardInformationCode")
@Slf4j
public class IetmStandardInformationCodeController extends JeecgController<IetmStandardInformationCode, IIetmStandardInformationCodeService> {
    
    @Autowired
    private IIetmStandardInformationCodeService ietmStandardInformationCodeService;

    /**
     * 分页列表查询
     *
     * @param ietmStandardInformationCode
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "手册预置模板-信息码管理-分页列表查询")
    @ApiOperation(value = "手册预置模板-信息码管理-分页列表查询", notes = "手册预置模板-信息码管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<IetmStandardInformationCode>> queryPageList(IetmStandardInformationCode ietmStandardInformationCode,
                                                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                                      HttpServletRequest req) {
        QueryWrapper<IetmStandardInformationCode> queryWrapper = QueryGenerator.initQueryWrapper(ietmStandardInformationCode, req.getParameterMap());
        Page<IetmStandardInformationCode> page = new Page<>(pageNo, pageSize);
        IPage<IetmStandardInformationCode> pageList = ietmStandardInformationCodeService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param ietmStandardInformationCode
     * @return
     */
    @AutoLog(value = "手册预置模板-信息码管理-添加")
    @ApiOperation(value = "手册预置模板-信息码管理-添加", notes = "手册预置模板-信息码管理-添加")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody IetmStandardInformationCode ietmStandardInformationCode) {
        ietmStandardInformationCodeService.save(ietmStandardInformationCode);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param ietmStandardInformationCode
     * @return
     */
    @AutoLog(value = "手册预置模板-信息码管理-编辑")
    @ApiOperation(value = "手册预置模板-信息码管理-编辑", notes = "手册预置模板-信息码管理-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody IetmStandardInformationCode ietmStandardInformationCode) {
        ietmStandardInformationCodeService.updateById(ietmStandardInformationCode);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "手册预置模板-信息码管理-通过id删除")
    @ApiOperation(value = "手册预置模板-信息码管理-通过id删除", notes = "手册预置模板-信息码管理-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        ietmStandardInformationCodeService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "手册预置模板-信息码管理-批量删除")
    @ApiOperation(value = "手册预置模板-信息码管理-批量删除", notes = "手册预置模板-信息码管理-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.ietmStandardInformationCodeService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "手册预置模板-信息码管理-通过id查询")
    @ApiOperation(value = "手册预置模板-信息码管理-通过id查询", notes = "手册预置模板-信息码管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<IetmStandardInformationCode> queryById(@RequestParam(name = "id", required = true) String id) {
        IetmStandardInformationCode ietmStandardInformationCode = ietmStandardInformationCodeService.getById(id);
        if (ietmStandardInformationCode == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(ietmStandardInformationCode);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param ietmStandardInformationCode
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, IetmStandardInformationCode ietmStandardInformationCode) {
        return super.exportXls(request, ietmStandardInformationCode, IetmStandardInformationCode.class, "手册预置模板-信息码管理");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, IetmStandardInformationCode.class);
    }

}
