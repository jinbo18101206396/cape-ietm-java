package org.jeecg.modules.ietm.ietmddn.controller;

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
import org.jeecg.modules.ietm.ietmddn.entity.IetmDdn;
import org.jeecg.modules.ietm.ietmddn.service.IIetmDdnExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @Description: 数据交换记录管理
 * @Author: jeecg-boot
 * @Date: 2026-09-15
 * @Version: V1.0
 */
@Api(tags = "数据交换记录管理")
@RestController
@RequestMapping("/ietmddn/ietmDdnExchange")
@Slf4j
public class IetmDdnExchangeController extends JeecgController<IetmDdn, IIetmDdnExchangeService> {

    @Autowired
    private IIetmDdnExchangeService ietmDdnExchangeService;

    /**
     * 分页列表查询
     *
     * @param ietmDdn
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "数据交换记录-分页列表查询")
    @ApiOperation(value = "数据交换记录-分页列表查询", notes = "数据交换记录-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<IetmDdn>> queryPageList(
            IetmDdn ietmDdn,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<IetmDdn> queryWrapper = QueryGenerator.initQueryWrapper(ietmDdn, req.getParameterMap());
        queryWrapper.eq("status", "1");
        queryWrapper.orderByDesc("create_time");
        Page<IetmDdn> page = new Page<>(pageNo, pageSize);
        IPage<IetmDdn> pageList = ietmDdnExchangeService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param ietmDdn
     * @return
     */
    @AutoLog(value = "数据交换记录-添加")
    @ApiOperation(value = "数据交换记录-添加", notes = "数据交换记录-添加")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody IetmDdn ietmDdn) {
        ietmDdn.setStatus("1");
        ietmDdnExchangeService.save(ietmDdn);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param ietmDdn
     * @return
     */
    @AutoLog(value = "数据交换记录-编辑")
    @ApiOperation(value = "数据交换记录-编辑", notes = "数据交换记录-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody IetmDdn ietmDdn) {
        ietmDdnExchangeService.updateById(ietmDdn);
        return Result.OK("编辑成功！");
    }

    /**
     * 通过id删除（逻辑删除）
     *
     * @param id
     * @return
     */
    @AutoLog(value = "数据交换记录-通过id删除")
    @ApiOperation(value = "数据交换记录-通过id删除", notes = "数据交换记录-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        IetmDdn ietmDdn = ietmDdnExchangeService.getById(id);
        if (ietmDdn == null) {
            return Result.error("未找到对应数据");
        }
        ietmDdn.setStatus("0");
        ietmDdnExchangeService.updateById(ietmDdn);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除（逻辑删除）
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "数据交换记录-批量删除")
    @ApiOperation(value = "数据交换记录-批量删除", notes = "数据交换记录-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            IetmDdn ietmDdn = ietmDdnExchangeService.getById(id);
            if (ietmDdn != null) {
                ietmDdn.setStatus("0");
                ietmDdnExchangeService.updateById(ietmDdn);
            }
        }
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "数据交换记录-通过id查询")
    @ApiOperation(value = "数据交换记录-通过id查询", notes = "数据交换记录-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<IetmDdn> queryById(@RequestParam(name = "id", required = true) String id) {
        IetmDdn ietmDdn = ietmDdnExchangeService.getById(id);
        if (ietmDdn == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(ietmDdn);
    }

    /**
     * 查看DDN文件内容
     *
     * @param id
     * @return
     */
    @AutoLog(value = "数据交换记录-查看DDN文件内容")
    @ApiOperation(value = "数据交换记录-查看DDN文件内容", notes = "数据交换记录-查看DDN文件内容")
    @GetMapping(value = "/getDdnFileContent")
    public Result<Map<String, Object>> getDdnFileContent(@RequestParam(name = "id", required = true) String id) {
        try {
            Map<String, Object> result = ietmDdnExchangeService.getDdnFileContent(id);
            return Result.OK(result);
        } catch (Exception e) {
            log.error("查看DDN文件内容失败", e);
            return Result.error("查看DDN文件内容失败：" + e.getMessage());
        }
    }
}
