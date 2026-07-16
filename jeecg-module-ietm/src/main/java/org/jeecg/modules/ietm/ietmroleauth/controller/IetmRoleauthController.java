package org.jeecg.modules.ietm.ietmroleauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.ietm.ietmroleauth.entity.IetmRoleauth;
import org.jeecg.modules.ietm.ietmroleauth.service.IIetmAuthConfigService;
import org.jeecg.modules.ietm.ietmroleauth.service.IIetmRoleauthService;
import org.jeecg.modules.ietm.ietmroleauth.vo.IetmRoleauthVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * @Description: 手册授权管理
 * @Author: jeecg-boot
 * @Date: 2026-07-15
 * @Version: V1.0
 */
@Api(tags="手册授权管理")
@RestController
@RequestMapping("/ietmroleauth/ietmRoleauth")
@Slf4j
public class IetmRoleauthController extends JeecgController<IetmRoleauth, IIetmRoleauthService> {

    @Autowired
    private IIetmRoleauthService ietmRoleauthService;

    @Autowired
    private IIetmAuthConfigService authConfigService;

    /**
     * 分页列表查询
     */
    //@AutoLog(value = "手册授权管理-分页列表查询")
    @ApiOperation(value="手册授权管理-分页列表查询", notes="手册授权管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<IetmRoleauth>> queryPageList(IetmRoleauth ietmRoleauth,
                                   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
                                   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<IetmRoleauth> queryWrapper = QueryGenerator.initQueryWrapper(ietmRoleauth, req.getParameterMap());
        Page<IetmRoleauth> page = new Page<>(pageNo, pageSize);
        IPage<IetmRoleauth> pageList = ietmRoleauthService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 查询授权列表（带关联信息）
     */
    @ApiOperation(value="手册授权管理-查询授权列表", notes="手册授权管理-查询授权列表")
    @GetMapping(value = "/listWithNames")
    public Result<List<IetmRoleauthVO>> queryListWithNames(@RequestParam(required = false) String objType,
                                                             @RequestParam(required = false) String objId) {
        List<IetmRoleauthVO> list = ietmRoleauthService.getRoleauthWithNames(objType, objId);
        return Result.OK(list);
    }

    /**
     * 根据项目ID查询授权列表
     */
    @ApiOperation(value="手册授权管理-根据项目ID查询", notes="手册授权管理-根据项目ID查询")
    @GetMapping(value = "/listByProject")
    public Result<List<IetmRoleauthVO>> queryListByProject(@RequestParam String projectId,
                                                             @RequestParam(required = false) String objType) {
        List<IetmRoleauthVO> list = ietmRoleauthService.getByProjectId(projectId, objType);
        return Result.OK(list);
    }

    /**
     * 添加
     */
    //@AutoLog(value = "手册授权管理-添加")
    @ApiOperation(value="手册授权管理-添加", notes="手册授权管理-添加")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody IetmRoleauth ietmRoleauth) {
        if (ietmRoleauthService.checkDuplicate(ietmRoleauth.getRoleId(),
                ietmRoleauth.getObjType(), ietmRoleauth.getObjId(), null)) {
            return Result.error("该角色已存在授权记录，不可重复添加！");
        }
        ietmRoleauthService.save(ietmRoleauth);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    //@AutoLog(value = "手册授权管理-编辑")
    @ApiOperation(value="手册授权管理-编辑", notes="手册授权管理-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody IetmRoleauth ietmRoleauth) {
        if (ietmRoleauthService.checkDuplicate(ietmRoleauth.getRoleId(),
                ietmRoleauth.getObjType(), ietmRoleauth.getObjId(), ietmRoleauth.getId())) {
            return Result.error("该角色已存在授权记录，不可重复！");
        }
        ietmRoleauthService.updateById(ietmRoleauth);
        return Result.OK("编辑成功！");
    }

    /**
     * 通过id删除
     */
    //@AutoLog(value = "手册授权管理-通过id删除")
    @ApiOperation(value="手册授权管理-通过id删除", notes="手册授权管理-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name="id") String id) {
        ietmRoleauthService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    //@AutoLog(value = "手册授权管理-批量删除")
    @ApiOperation(value="手册授权管理-批量删除", notes="手册授权管理-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name="ids") String ids) {
        this.ietmRoleauthService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }

    /**
     * 批量保存或更新
     */
    //@AutoLog(value = "手册授权管理-批量保存")
    @ApiOperation(value="手册授权管理-批量保存", notes="手册授权管理-批量保存")
    @PostMapping(value = "/batchSave")
    public Result<String> batchSave(@RequestBody List<IetmRoleauth> roleauthList) {
        ietmRoleauthService.batchSaveOrUpdateWithOverride(roleauthList);
        return Result.OK("保存成功！");
    }

    /**
     * 通过id查询
     */
    @ApiOperation(value="手册授权管理-通过id查询", notes="手册授权管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<IetmRoleauth> queryById(@RequestParam(name="id") String id) {
        IetmRoleauth ietmRoleauth = ietmRoleauthService.getById(id);
        if(ietmRoleauth==null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(ietmRoleauth);
    }

    /**
     * 获取授权类型配置
     */
    @ApiOperation(value="获取授权类型配置", notes="获取授权类型配置")
    @GetMapping(value = "/getAuthType")
    public Result<String> getAuthType() {
        String authType = authConfigService.getConfigValue("authtype");
        return Result.OK(authType != null ? authType : "1");
    }

    /**
     * 设置授权类型
     */
    //@AutoLog(value = "设置授权类型")
    @ApiOperation(value="设置授权类型", notes="设置授权类型")
    @PostMapping(value = "/setAuthType")
    public Result<String> setAuthType(@RequestParam String authType) {
        authConfigService.saveOrUpdateConfig("authtype", authType);
        return Result.OK("设置成功！");
    }

    /**
     * 导出excel
     */
    @RequestMapping(value = "/exportXls")
    public org.springframework.web.servlet.ModelAndView exportXls(HttpServletRequest request, IetmRoleauth ietmRoleauth) {
        return super.exportXls(request, ietmRoleauth, IetmRoleauth.class, "手册授权管理");
    }

    /**
     * 通过excel导入数据
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, IetmRoleauth.class);
    }
}
