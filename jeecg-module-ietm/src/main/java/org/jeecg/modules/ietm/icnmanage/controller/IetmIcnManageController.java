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
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.icnmanage.vo.IcnProjectInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.*;
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
@Validated
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
            @RequestParam(value = "cmnodeId") @NotBlank(message = "构型节点不能为空") String cmnodeId,
            @RequestParam(value = "security") @NotNull(message = "密级不能为空") @Min(value = 0, message = "密级最小值为0") @Max(value = 5, message = "密级最大值为5") Integer security,
            @RequestParam(value = "uniqueId") String uniqueId,
            @RequestParam(value = "sns") String sns,
            @RequestParam(value = "icnType", required = false) String icnType,
            @RequestParam(value = "variantCode", defaultValue = "A") String variantCode,
            @RequestParam(value = "issueNo", defaultValue = "001") @NotBlank(message = "版本号不能为空") @Pattern(regexp = "^\\d{3}$", message = "版本号必须为3位数字") String issueNo,
            @RequestParam(value = "originator") @NotBlank(message = "责任单位代码不能为空") @Size(max = 50, message = "责任单位代码长度不能超过50") String originator,
            @RequestParam(value = "originatorName") String originatorName,
            @RequestParam(value = "rpc") String rpc,
            @RequestParam(value = "rpcName") String rpcName) {

        try {
            // 校验文件必填
            if (files == null || files.length == 0) {
                return Result.error("实体文件不能为空");
            }

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
     * 通过id查询（含附件信息）
     */
    @ApiOperation(value = "通过id查询（含附件）", notes = "通过id查询（含附件）")
    @GetMapping(value = "/queryByIdWithAttachment")
    public Result<IetmIcnManage> queryByIdWithAttachment(@RequestParam(name = "id") String id) {
        IetmIcnManage ietmIcnManage = ietmIcnManageService.getByIdWithAttachment(id);
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
     * 预览文件（在线查看，支持解密）
     */
    @ApiOperation(value = "预览文件", notes = "在线预览ICN文件，支持图片、视频等")
    @GetMapping(value = "/viewFile")
    public void viewFile(@RequestParam(name = "fileKey") String fileKey, HttpServletResponse response) {
        try {
            ietmIcnManageService.viewFile(fileKey, response);
        } catch (Exception e) {
            log.error("文件预览失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
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

    /**
     * 获取预览信息
     */
    @AutoLog(value = "ICN预览-获取预览信息")
    @ApiOperation(value = "获取预览信息", notes = "获取ICN文件的预览信息")
    @GetMapping(value = "/preview/{id}")
    public Result<org.jeecg.modules.ietm.icnmanage.vo.PreviewInfoVO> getPreviewInfo(@PathVariable("id") String id) {
        try {
            org.jeecg.modules.ietm.icnmanage.vo.PreviewInfoVO vo = ietmIcnManageService.getPreviewInfo(id);
            return Result.OK(vo);
        } catch (Exception e) {
            log.error("获取预览信息失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取引用关系信息
     */
    @AutoLog(value = "ICN引用-获取引用关系")
    @ApiOperation(value = "获取引用关系", notes = "获取ICN的引用关系信息")
    @GetMapping(value = "/reference/{id}")
    public Result<org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO> getReferenceInfo(@PathVariable("id") String id) {
        try {
            org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO vo = ietmIcnManageService.getReferenceInfo(id);
            return Result.OK(vo);
        } catch (Exception e) {
            log.error("获取引用关系失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询ICN引用的其他ICN列表（正向引用）
     */
    @ApiOperation(value = "查询正向引用列表", notes = "查询该ICN引用的其他ICN")
    @GetMapping(value = "/getReferencedList")
    public Result<List<IetmIcnManage>> getReferencedList(@RequestParam("icnId") String icnId) {
        try {
            IetmIcnManageMapper mapper = (IetmIcnManageMapper) ietmIcnManageService.getBaseMapper();
            List<IetmIcnManage> list = mapper.getReferencedIcnList(icnId);
            return Result.OK(list);
        } catch (Exception e) {
            log.error("查询正向引用列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询引用当前ICN的其他ICN列表（反向引用）
     */
    @ApiOperation(value = "查询反向引用列表", notes = "查询引用该ICN的其他ICN")
    @GetMapping(value = "/getReferencingList")
    public Result<List<IetmIcnManage>> getReferencingList(@RequestParam("icnId") String icnId) {
        try {
            IetmIcnManageMapper mapper = (IetmIcnManageMapper) ietmIcnManageService.getBaseMapper();
            List<IetmIcnManage> list = mapper.getReferencingIcnList(icnId);
            return Result.OK(list);
        } catch (Exception e) {
            log.error("查询反向引用列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询引用当前ICN的DM模块列表
     */
    @ApiOperation(value = "查询DM引用列表", notes = "查询引用该ICN的DM模块")
    @GetMapping(value = "/getReferencedByDmList")
    public Result<List<java.util.Map<String, Object>>> getReferencedByDmList(@RequestParam("icnId") String icnId) {
        try {
            IetmIcnManageMapper mapper = (IetmIcnManageMapper) ietmIcnManageService.getBaseMapper();
            List<java.util.Map<String, Object>> list = mapper.getReferencedByDmList(icnId);
            return Result.OK(list);
        } catch (Exception e) {
            log.error("查询DM引用列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 单文件下载
     */
    @AutoLog(value = "ICN下载-单文件下载")
    @ApiOperation(value = "单文件下载", notes = "下载单个ICN文件")
    @GetMapping(value = "/download/single/{id}")
    public void downloadSingle(
            @PathVariable("id") String id,
            @RequestParam(value = "includeRelated", defaultValue = "false") Boolean includeRelated,
            HttpServletResponse response) {
        try {
            ietmIcnManageService.downloadSingle(id, includeRelated, response);
        } catch (Exception e) {
            log.error("单文件下载失败", e);
        }
    }

    /**
     * 批量下载
     */
    @AutoLog(value = "ICN下载-批量下载")
    @ApiOperation(value = "批量下载", notes = "批量下载ICN文件（ZIP格式）")
    @PostMapping(value = "/download/batch")
    public void downloadBatch(
            @RequestBody java.util.Map<String, Object> params,
            HttpServletResponse response) {
        try {
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) params.get("ids");
            Boolean includeRelated = params.get("includeRelated") != null &&
                                    (Boolean) params.get("includeRelated");
            ietmIcnManageService.downloadBatch(ids, includeRelated, response);
        } catch (Exception e) {
            log.error("批量下载失败", e);
        }
    }

    /**
     * 异步批量下载
     */
    @AutoLog(value = "ICN下载-异步批量下载")
    @ApiOperation(value = "异步批量下载", notes = "创建异步批量下载任务")
    @PostMapping(value = "/download/batchAsync")
    public Result<String> downloadBatchAsync(@RequestBody java.util.Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) params.get("ids");
            Boolean includeRelated = params.get("includeRelated") != null &&
                                    (Boolean) params.get("includeRelated");
            String taskId = ietmIcnManageService.downloadBatchAsync(ids, includeRelated);
            return Result.OK("下载任务已创建", taskId);
        } catch (Exception e) {
            log.error("创建异步下载任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询下载任务状态
     */
    @ApiOperation(value = "查询下载任务状态", notes = "查询异步下载任务的状态")
    @GetMapping(value = "/download/task/{taskId}")
    public Result<org.jeecg.modules.ietm.icnmanage.vo.DownloadTaskVO> getDownloadTaskStatus(
            @PathVariable("taskId") String taskId) {
        try {
            org.jeecg.modules.ietm.icnmanage.vo.DownloadTaskVO vo =
                ietmIcnManageService.getDownloadTaskStatus(taskId);
            return Result.OK(vo);
        } catch (Exception e) {
            log.error("查询下载任务状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 添加引用关系
     */
    @AutoLog(value = "ICN引用-添加引用关系")
    @ApiOperation(value = "添加引用关系", notes = "添加ICN引用关系")
    @PostMapping(value = "/addReference")
    public Result<?> addReference(@RequestBody java.util.Map<String, String> params) {
        try {
            String sourceIcnId = params.get("sourceIcnId");
            String targetIcnId = params.get("targetIcnId");
            String referenceType = params.get("referenceType");
            String remark = params.get("remark");
            ietmIcnManageService.addReference(sourceIcnId, targetIcnId, referenceType, remark);
            return Result.OK("添加引用关系成功");
        } catch (Exception e) {
            log.error("添加引用关系失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除引用关系
     */
    @AutoLog(value = "ICN引用-删除引用关系")
    @ApiOperation(value = "删除引用关系", notes = "删除ICN引用关系")
    @DeleteMapping(value = "/deleteReference/{id}")
    public Result<?> deleteReference(@PathVariable("id") String id) {
        try {
            ietmIcnManageService.deleteReference(id);
            return Result.OK("删除引用关系成功");
        } catch (Exception e) {
            log.error("删除引用关系失败", e);
            return Result.error(e.getMessage());
        }
    }
}

