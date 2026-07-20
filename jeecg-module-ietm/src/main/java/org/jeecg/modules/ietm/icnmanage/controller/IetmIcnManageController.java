package org.jeecg.modules.ietm.icnmanage.controller;

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
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.icnmanage.vo.IcnProjectInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * @Description: 项目管理-项目实体管理Controller
 * @Author: jeecg-boot
 * @Date: 2026-07-19
 * @Version: V2.0
 */
@Api(tags = "项目管理-项目实体管理")
@RestController
@RequestMapping("/icnmanage/ietmIcnManage")
@Slf4j
public class IetmIcnManageController extends JeecgController<IetmIcnManage, IIetmIcnManageService> {

    @Autowired
    private IIetmIcnManageService ietmIcnManageService;

    /**
     * 分页列表查询
     */
    @AutoLog(value = "项目实体管理-分页列表查询")
    @ApiOperation(value = "分页列表查询", notes = "分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<IetmIcnManage>> queryPageList(
            IetmIcnManage ietmIcnManage,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            HttpServletRequest req) {

        QueryWrapper<IetmIcnManage> queryWrapper = QueryGenerator.initQueryWrapper(ietmIcnManage, req.getParameterMap());
        Page<IetmIcnManage> page = new Page<>(pageNo, pageSize);
        IPage<IetmIcnManage> pageList = ietmIcnManageService.page(page, queryWrapper);

        return Result.OK(pageList);
    }

    /**
     * 查询ICN列表（含附件信息）
     */
    @ApiOperation(value = "查询ICN列表（含附件）", notes = "查询ICN列表（含附件）")
    @GetMapping(value = "/listWithAttachments")
    public Result<List<IetmIcnManage>> listWithAttachments(
            @RequestParam(name = "cmnodeId") String cmnodeId,
            @RequestParam(name = "includeChildren", defaultValue = "0") String includeChildren) {
        List<IetmIcnManage> list = ietmIcnManageService.listWithAttachments(cmnodeId, includeChildren);
        return Result.OK(list);
    }

    /**
     * 新增ICN（带文件上传）
     */
    // @AutoLog(value = "项目实体管理-新增")  // 注释掉，避免序列化MultipartFile导致FastJSON错误
    @ApiOperation(value = "新增ICN", notes = "新增ICN")
    @PostMapping(value = "/add")
    public Result<String> add(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "cmnodeId") String cmnodeId,
            @RequestParam(value = "security") Integer security,
            @RequestParam(value = "uniqueId") String uniqueId,
            @RequestParam(value = "sns") String sns,
            @RequestParam(value = "icnType", required = false) String icnType,
            @RequestParam(value = "variantCode", defaultValue = "A") String variantCode,
            @RequestParam(value = "issueNo", defaultValue = "001") String issueNo,
            @RequestParam(value = "originator") String originator,
            @RequestParam(value = "originatorName") String originatorName,
            @RequestParam(value = "rpc") String rpc,
            @RequestParam(value = "rpcName") String rpcName) {

        try {
            IetmIcnManage icnManage = new IetmIcnManage();
            icnManage.setCmnodeId(cmnodeId);
            icnManage.setSecurity(security);
            icnManage.setUniqueId(uniqueId);
            icnManage.setSns(sns);
            icnManage.setIcnType(icnType);
            icnManage.setVariantCode(variantCode);
            icnManage.setIssueNo(issueNo);
            icnManage.setOriginator(originator);
            icnManage.setOriginatorName(originatorName);
            icnManage.setRpc(rpc);
            icnManage.setRpcName(rpcName);

            ietmIcnManageService.addWithFiles(icnManage, files);

            // 手动记录日志（不包含文件对象，避免序列化问题）
            log.info("新增ICN成功 - cmnodeId: {}, sns: {}, 文件数: {}",
                    cmnodeId, sns, files != null ? files.length : 0);

            return Result.OK("添加成功！");
        } catch (Exception e) {
            log.error("新增ICN失败", e);
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 编辑ICN
     */
    @AutoLog(value = "项目实体管理-编辑")
    @ApiOperation(value = "编辑ICN", notes = "编辑ICN")
    @PutMapping(value = "/edit")
    public Result<String> edit(@RequestBody IetmIcnManage ietmIcnManage) {
        ietmIcnManageService.updateById(ietmIcnManage);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "项目实体管理-通过id删除")
    @ApiOperation(value = "通过id删除", notes = "通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        ietmIcnManageService.removeWithAttachments(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "项目实体管理-批量删除")
    @ApiOperation(value = "批量删除", notes = "批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        ietmIcnManageService.removeBatchWithAttachments(idList);
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @ApiOperation(value = "通过id查询", notes = "通过id查询")
    @GetMapping(value = "/queryById")
    public Result<IetmIcnManage> queryById(@RequestParam(name = "id") String id) {
        IetmIcnManage ietmIcnManage = ietmIcnManageService.getById(id);
        if (ietmIcnManage == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(ietmIcnManage);
    }

    /**
     * 相关文件上传
     */
    // @AutoLog(value = "项目实体管理-相关文件上传")  // 注释掉，避免序列化MultipartFile导致FastJSON错误
    @ApiOperation(value = "相关文件上传", notes = "相关文件上传")
    @PostMapping(value = "/uploadRelatedFiles")
    public Result<String> uploadRelatedFiles(
            @RequestParam(value = "icnId") String icnId,
            @RequestParam(value = "files") MultipartFile[] files) {
        try {
            ietmIcnManageService.uploadRelatedFiles(icnId, files);
            return Result.OK("相关文件上传成功！");
        } catch (Exception e) {
            log.error("相关文件上传失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 差异上传
     */
    // @AutoLog(value = "项目实体管理-差异上传")  // 注释掉，避免序列化MultipartFile导致FastJSON错误
    @ApiOperation(value = "差异上传", notes = "差异上传")
    @PostMapping(value = "/uploadDiffFiles")
    public Result<String> uploadDiffFiles(
            @RequestParam(value = "originalIcnId") String originalIcnId,
            @RequestParam(value = "files") MultipartFile[] files,
            @RequestParam(value = "newUniqueId") String newUniqueId,
            @RequestParam(value = "newVariantCode") String newVariantCode) {
        try {
            ietmIcnManageService.uploadDiffFiles(originalIcnId, files, newUniqueId, newVariantCode);
            return Result.OK("差异上传成功！");
        } catch (Exception e) {
            log.error("差异上传失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 新版上传
     */
    // @AutoLog(value = "项目实体管理-新版上传")  // 注释掉，避免序列化MultipartFile导致FastJSON错误
    @ApiOperation(value = "新版上传", notes = "新版上传")
    @PostMapping(value = "/uploadNewVersion")
    public Result<String> uploadNewVersion(
            @RequestParam(value = "icnId") String icnId,
            @RequestParam(value = "files") MultipartFile[] files) {
        try {
            ietmIcnManageService.uploadNewVersion(icnId, files);
            return Result.OK("新版上传成功！");
        } catch (Exception e) {
            log.error("新版上传失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 获取项目信息和SNS编码
     */
    @ApiOperation(value = "获取项目信息", notes = "获取项目信息")
    @GetMapping(value = "/getProjectInfo")
    public Result<IcnProjectInfoVO> getProjectInfo(@RequestParam(name = "cmnodeId") String cmnodeId) {
        try {
            IcnProjectInfoVO projectInfo = ietmIcnManageService.getProjectInfo(cmnodeId);
            return Result.OK(projectInfo);
        } catch (Exception e) {
            log.error("获取项目信息失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 获取下一个唯一识别码
     */
    @ApiOperation(value = "获取下一个唯一识别码", notes = "获取下一个唯一识别码")
    @GetMapping(value = "/getNextUniqueId")
    public Result<String> getNextUniqueId(@RequestParam(name = "cmnodeId") String cmnodeId) {
        String uniqueId = ietmIcnManageService.getNextUniqueId(cmnodeId);
        return Result.OK(uniqueId);
    }

    /**
     * 批量新增ICN
     */
    @AutoLog(value = "项目实体管理-批量新增")
    @ApiOperation(value = "批量新增ICN", notes = "批量新增ICN")
    @PostMapping(value = "/batchAdd")
    public Result<String> batchAdd(@RequestBody IetmIcnManage template) {
        try {
            Integer count = template.getCount();
            if (count == null || count < 1 || count > 100) {
                return Result.error("新增数量必须在1-100之间");
            }

            String cmnodeId = template.getCmnodeId();
            if (cmnodeId == null || cmnodeId.isEmpty()) {
                return Result.error("构型节点ID不能为空");
            }

            // 调用Service批量创建
            int successCount = ietmIcnManageService.batchAddIcn(template);

            return Result.OK("批量新增成功，共创建 " + successCount + " 条记录");
        } catch (Exception e) {
            log.error("批量新增ICN失败", e);
            return Result.error("批量新增失败：" + e.getMessage());
        }
    }

    /**
     * 获取该ICN引用的其他ICN列表
     */
    @ApiOperation(value = "获取引用列表", notes = "获取该ICN引用的其他ICN")
    @GetMapping(value = "/getReferencedList")
    public Result<List<IetmIcnManage>> getReferencedList(@RequestParam(name = "icnId") String icnId) {
        // TODO: 实现引用关系查询逻辑
        // 需要建立ICN引用关系表（ietm_icn_reference）
        // 字段：id, source_icn_id, target_icn_id, create_time
        return Result.OK(new java.util.ArrayList<>());
    }

    /**
     * 获取引用该ICN的其他ICN列表
     */
    @ApiOperation(value = "获取被引用列表", notes = "获取引用该ICN的其他ICN")
    @GetMapping(value = "/getReferencingList")
    public Result<List<IetmIcnManage>> getReferencingList(@RequestParam(name = "icnId") String icnId) {
        // TODO: 实现被引用关系查询逻辑
        return Result.OK(new java.util.ArrayList<>());
    }

    /**
     * 下载文件
     */
    @ApiOperation(value = "下载文件", notes = "下载文件")
    @GetMapping(value = "/download")
    public void download(
            @RequestParam(name = "fileKey") String fileKey,
            @RequestParam(name = "fileName", required = false) String fileName,
            HttpServletResponse response) {
        try {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));

            ietmIcnManageService.downloadFile(fileKey, response);
        } catch (Exception e) {
            log.error("文件下载失败", e);
        }
    }

    /**
     * 导出excel
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, IetmIcnManage ietmIcnManage) {
        return super.exportXls(request, ietmIcnManage, IetmIcnManage.class, "项目实体管理");
    }

    /**
     * 导出Excel模板
     */
    @AutoLog(value = "项目实体管理-导出模板")
    @ApiOperation(value = "导出Excel导入模板", notes = "导出Excel导入模板")
    @RequestMapping(value = "/exportTemplate")
    public ModelAndView exportTemplate() {
        // 使用父类方法导出空模板
        ModelAndView modelAndView = new ModelAndView("jeecgEntityExcelView");
        modelAndView.addObject("title", "项目实体管理导入模板");
        modelAndView.addObject("exportFields", Arrays.asList(
            "cmnodeId", "sns", "uniqueId", "variantCode", "issueNo",
            "security", "icnType", "originator", "originatorName",
            "rpc", "rpcName"
        ));
        modelAndView.addObject("entity", IetmIcnManage.class);
        modelAndView.addObject("fileName", "ICN导入模板");
        return modelAndView;
    }

    /**
     * 通过excel导入数据
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, IetmIcnManage.class);
    }
}

