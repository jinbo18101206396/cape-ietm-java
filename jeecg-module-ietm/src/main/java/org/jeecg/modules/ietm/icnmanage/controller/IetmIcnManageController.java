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
import org.jeecg.modules.ietm.common.util.MimeTypeDetector;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.icnmanage.vo.IcnProjectInfoVO;
import org.jeecg.modules.ietm.icnmanage.vo.MultimediaInfoVO;
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

    @Autowired
    private org.jeecg.modules.ietm.icnmanage.service.IIetmIcnFormulaService icnFormulaService;

    // 🔧 2026-10-16 修复：注入配置的文件存储路径，统一与 viewFile 接口的路径配置
    // 使用通用路径配置（fileKey 中已包含 icn\ 前缀）
    @org.springframework.beans.factory.annotation.Value("${accessFile.location}")
    private String fileStorageLocation;

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
            @RequestParam(name = "cmNodeId") String cmNodeId,
            @RequestParam(name = "includeChildren", defaultValue = "0") String includeChildren) {
        List<IetmIcnManage> list = ietmIcnManageService.listWithAttachments(cmNodeId, includeChildren);
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
            @RequestParam(value = "cmNodeId") @NotBlank(message = "构型节点不能为空") String cmNodeId,
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
            icnManage.setCmNodeId(cmNodeId);
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
            log.info("新增ICN成功 - cmNodeId: {}, sns: {}, 文件数: {}",
                    cmNodeId, sns, files != null ? files.length : 0);

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
     * 通过ICN编码查询（用于导出实体功能）
     */
    @ApiOperation(value = "通过ICN编码查询", notes = "通过ICN编码查询ICN详情")
    @GetMapping(value = "/queryByIcnCode")
    public Result<IetmIcnManage> queryByIcnCode(@RequestParam(name = "icn") @NotBlank(message = "ICN编码不能为空") String icn) {
        // 🔧 2026-10-14 修复P0: 公式ICN查询
        // 根因：公式存储在独立表ietm_icn_formula，queryByIcnCode只查ietm_icn_manage导致"ICN不存在"

        // 1. 优先查询公式表（Schema B独立表）
        org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula formulaEntity =
            icnFormulaService.getByIcnWithContent(icn);

        if (formulaEntity != null) {
            // 🔧 P1优化：使用BeanUtils消除代码重复（20行→3行）
            IetmIcnManage adapted = convertFormulaToManage(formulaEntity);
            log.info("[queryByIcnCode] 公式ICN查询成功 - ICN: {}, 来源: ietm_icn_formula", icn);
            return Result.OK(adapted);
        }

        // 2. Fallback: 查询普通ICN（图符/图像）
        // 使用Lambda查询
        // 🔧 修复P0：改用.list()而非.one()，避免多条记录时抛MybatisPlusException
        List<IetmIcnManage> icnList = ietmIcnManageService.lambdaQuery()
                .eq(IetmIcnManage::getIcn, icn)
                .orderByDesc(IetmIcnManage::getCreateTime)
                .last("LIMIT 1")
                .list();

        if (icnList == null || icnList.isEmpty()) {
            log.warn("[queryByIcnCode] ICN不存在（两表都未找到） - ICN: {}", icn);
            return Result.error("未找到ICN编码为 " + icn + " 的实体");
        }

        IetmIcnManage icnManage = icnList.get(0);
        // 🔧 修复：加载附件信息，确保返回的ICN包含ietmAttachment
        boolean loaded = ietmIcnManageService.loadAttachment(icnManage);

        // 调试日志
        log.info("[queryByIcnCode] 普通ICN查询成功 - ICN: {}, loadAttachment: {}, hasAttachment: {}, fileKey: {}",
            icn, loaded,
            icnManage.getIetmAttachment() != null,
            icnManage.getIetmAttachment() != null ? icnManage.getIetmAttachment().getFileKey() : "null");

        return Result.OK(icnManage);
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
    public Result<IcnProjectInfoVO> getProjectInfo(@RequestParam(name = "cmNodeId") String cmNodeId) {
        try {
            IcnProjectInfoVO projectInfo = ietmIcnManageService.getProjectInfo(cmNodeId);
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
    public Result<String> getNextUniqueId(@RequestParam(name = "cmNodeId") String cmNodeId) {
        String uniqueId = ietmIcnManageService.getNextUniqueId(cmNodeId);
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

            String cmNodeId = template.getCmNodeId();
            if (cmNodeId == null || cmNodeId.isEmpty()) {
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
            "cmNodeId", "sns", "uniqueId", "variantCode", "issueNo",
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

    /**
     * 图符弹窗-分页查询图符类ICN
     */
    @AutoLog(value = "图符弹窗-分页查询图符类ICN")
    @ApiOperation(value = "图符弹窗-分页查询图符类ICN", notes = "用于编辑器图符弹窗，只返回cgm/bmp/jpg/png/gif/tif/svg等图符文件")
    @GetMapping(value = "/listSymbolsForDialog")
    public Result<IPage<IetmIcnManage>> listSymbolsForDialog(
            @RequestParam(name = "cmNodeId") @NotBlank(message = "请先选择构型节点") String cmNodeId,
            @RequestParam(name = "includeChildren", defaultValue = "0") String includeChildren,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

        IPage<IetmIcnManage> page = ietmIcnManageService.listSymbolsForDialog(
                cmNodeId, includeChildren, pageNo, pageSize);
        return Result.OK(page);
    }

    /**
     * 多媒体弹窗-分页查询多媒体类ICN（排除普通位图图像）
     */
    @AutoLog(value = "多媒体弹窗-分页查询多媒体类ICN")
    @ApiOperation(value = "多媒体弹窗-分页查询多媒体类ICN", notes = "用于编辑器多媒体弹窗，排除普通位图图像（bmp/jpg/jpeg/png/gif/tif/tiff），保留视频/音频/CGM/SVG/3D模型/动画等所有其他文件")
    @GetMapping(value = "/listMultimediaForDialog")
    public Result<IPage<IetmIcnManage>> listMultimediaForDialog(
            @RequestParam(name = "cmNodeId") @NotBlank(message = "请先选择构型节点") String cmNodeId,
            @RequestParam(name = "includeChildren", defaultValue = "0") String includeChildren,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

        IPage<IetmIcnManage> page = ietmIcnManageService.listMultimediaForDialog(
                cmNodeId, includeChildren, pageNo, pageSize);
        return Result.OK(page);
    }

    /**
     * 预览-查看ICN图片（用于DM预览中的图形显示）
     *
     * @param icnCode ICN完整编码（如：ICN-ZB1-A-02-60101-30101-00001-A-001-01）
     */
    /**
     * 查看ICN图片/视频/音频 - 编辑器预览专用接口
     *
     * 使用场景：DM编辑器的预览弹窗（用户主动点击"预览"按钮）
     * 调用路径：DmPreviewModal.vue → /ietm/icn/view/{icnCode}
     *
     * 设计特性：
     * - 用户体验优先：返回占位图避免显示破损图标
     * - 错误友好：捕获所有异常
     * - 基础缓存：Cache-Control 1天
     * - Range请求支持：视频拖动播放必需（HTTP 206 Partial Content）
     *
     * 🔧 2026-09-18 增强：
     * - 支持HTTP Range请求（视频/音频流式传输）
     * - 自动检测Range头，按需返回文件片段
     * - 大幅降低内存占用和网络传输
     */
    @ApiOperation(value = "查看ICN图片/视频/音频", notes = "用于DM预览中显示图形/视频/音频，根据ICN编码返回文件流，支持Range请求（视频拖动）")
    @GetMapping(value = "/view/{icnCode}")
    public void viewIcn(@PathVariable("icnCode") String icnCode,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        try {
            // 🔧 2026-10-16 修复：统一处理公式和图符，简化前端逻辑
            // 先查询普通图符表
            QueryWrapper<IetmIcnManage> qw = new QueryWrapper<>();
            qw.eq("icn", icnCode);
            qw.orderByDesc("create_time");
            qw.orderByDesc("id");
            qw.last("LIMIT 1");
            List<IetmIcnManage> icnList = ietmIcnManageService.list(qw);

            IetmIcnManage icn = null;
            boolean isFormula = false;
            String formulaHtml = null;

            if (icnList != null && !icnList.isEmpty()) {
                // 普通图符
                icn = icnList.get(0);
                log.debug("找到普通图符: icnCode={}, id={}", icnCode, icn.getId());
            } else {
                // 查询公式表
                QueryWrapper<IetmIcnFormula> formulaQw = new QueryWrapper<>();
                formulaQw.eq("icn", icnCode);
                IetmIcnFormula formula = icnFormulaService.getOne(formulaQw);

                if (formula != null) {
                    log.debug("找到公式: icnCode={}, id={}", icnCode, formula.getId());
                    isFormula = true;

                    // 读取公式 HTML（从 BLOB 字段）
                    byte[] formulaBytes = formula.getFilecontent();
                    if (formulaBytes != null && formulaBytes.length > 0) {
                        formulaHtml = new String(formulaBytes, java.nio.charset.StandardCharsets.UTF_8);
                        log.debug("公式HTML长度: {} bytes", formulaHtml.length());
                    } else {
                        log.warn("公式内容为空: icnCode={}", icnCode);
                        sendPlaceholderImage(response, "公式内容为空");
                        return;
                    }

                    // 返回公式 HTML
                    response.setContentType("text/html; charset=UTF-8");
                    response.getWriter().write(formulaHtml);
                    return;
                }
            }

            if (icn == null && !isFormula) {
                log.warn("ICN不存在: {}", icnCode);
                sendPlaceholderImage(response, "ICN不存在");
                return;
            }

            // 以下处理普通图符
            log.debug("处理普通图符: icnCode={}, id={}", icnCode, icn.getId());

            // 加载附件信息
            if (!ietmIcnManageService.loadAttachment(icn)) {  // 🔧 修复P3：检查加载结果
                log.warn("ICN附件未找到: icnCode={}, icnId={}", icnCode, icn.getId());
                sendPlaceholderImage(response, "图片文件缺失");
                return;
            }

            if (icn.getIetmAttachment().getFileKey() == null) {
                log.warn("附件FileKey为null: icnCode={}, attachmentId={}", icnCode, icn.getIetmAttachment().getId());
                sendPlaceholderImage(response, "图片文件缺失");
                return;
            }

            // 获取文件Key和扩展名
            String fileKey = icn.getIetmAttachment().getFileKey();
            String fileName = icn.getIetmAttachment().getFileName();
            String fileExtension = extractFileExtension(fileName);

            // 🔧 2026-10-17 修复深度2：支持嵌套项目结构中的ICN文件
            // fileKey 可能包含目录结构（如 project/projectId/icn/filename.jpg）
            // 需要将正斜杠转换为系统路径分隔符以支持跨平台路径
            String normalizedFileKey = fileKey.replace("/", java.io.File.separator);
            java.io.File file = new java.io.File(fileStorageLocation, normalizedFileKey);

            if (!file.exists()) {
                log.warn("ICN文件不存在: fileKey={}, path={}", fileKey, file.getAbsolutePath());
                sendPlaceholderImage(response, "文件不存在");
                return;
            }

            log.info("ICN文件存在，准备读取: fileKey={}, path={}, size={}",
                     fileKey, file.getAbsolutePath(), file.length());

            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());

            // 🔧 2026-09-18 增强：使用魔数检测MIME类型（支持视频/音频）
            String mimeType = MimeTypeDetector.detect(fileContent);
            log.debug("ICN: {}, 文件名: {}, 检测到MIME类型: {}, 文件大小: {} bytes",
                     icnCode, fileName, mimeType, fileContent.length);

            // 🔧 2026-09-18 新增：检测Range请求头
            String rangeHeader = request.getHeader("Range");

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // 处理Range请求（视频拖动场景）
                handleRangeRequest(fileContent, mimeType, fileName, rangeHeader, response);
            } else {
                // 处理普通请求（图片场景）
                handleFullRequest(fileContent, mimeType, fileName, response);
            }

        } catch (Exception e) {
            log.error("查看ICN图片失败: icnCode={}", icnCode, e);
            try {
                // 临时诊断：返回错误信息而不是占位符
                String errorMsg = "ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage();
                sendPlaceholderImage(response, errorMsg.substring(0, Math.min(50, errorMsg.length())));
            } catch (Exception ex) {
                log.error("发送占位图失败", ex);
            }
        }
    }

    /**
     * 处理普通请求（完整文件）
     *
     * 适用场景：图片、小文件
     * 返回状态码：200 OK
     *
     * @param fileContent 文件内容
     * @param mimeType MIME类型
     * @param fileName 文件名
     * @param response HTTP响应
     */
    private void handleFullRequest(byte[] fileContent, String mimeType, String fileName,
                                    HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(mimeType);
        response.setContentLength(fileContent.length);
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "public, max-age=86400");

        response.getOutputStream().write(fileContent);
        response.getOutputStream().flush();

        log.debug("完整文件请求: {} size={}KB", fileName, fileContent.length / 1024);
    }

    /**
     * 处理Range请求（视频拖动场景）
     *
     * Range格式示例：
     * - bytes=0-1023      : 请求前1024字节
     * - bytes=1024-       : 请求从1024到文件末尾
     * - bytes=-1024       : 请求最后1024字节
     *
     * 适用场景：
     * - 视频拖动进度条（浏览器自动发起Range请求）
     * - 音频快进/快退
     * - 大文件断点续传
     *
     * 返回状态码：206 Partial Content
     *
     * @param fileContent 文件内容
     * @param mimeType MIME类型
     * @param fileName 文件名
     * @param rangeHeader Range请求头（如：bytes=0-1023）
     * @param response HTTP响应
     */
    private void handleRangeRequest(byte[] fileContent, String mimeType, String fileName,
                                     String rangeHeader, HttpServletResponse response) throws Exception {
        long fileLength = fileContent.length;

        // 解析Range头（格式：bytes=start-end）
        String rangeValue = rangeHeader.replace("bytes=", "");
        String[] ranges = rangeValue.split("-");

        long start = 0;
        long end = fileLength - 1;

        try {
            if (ranges[0].isEmpty()) {
                // bytes=-1024：最后N字节
                start = fileLength - Long.parseLong(ranges[1]);
            } else {
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
            }

            // 边界校验
            if (start < 0) start = 0;
            if (end >= fileLength) end = fileLength - 1;
            if (start > end) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileLength);
                log.warn("无效的Range范围: start={}, end={}, fileLength={}", start, end, fileLength);
                return;
            }

        } catch (NumberFormatException e) {
            log.warn("无效的Range头格式: {}", rangeHeader);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        long contentLength = end - start + 1;

        // 设置206 Partial Content响应
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setContentType(mimeType);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        response.setHeader("Content-Length", String.valueOf(contentLength));
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "public, max-age=86400");

        // 写入指定范围的数据
        response.getOutputStream().write(fileContent, (int) start, (int) contentLength);
        response.getOutputStream().flush();

        log.debug("Range请求: {} bytes={}-{}/{} ({}KB)", fileName, start, end, fileLength, contentLength / 1024);
    }

    /**
     * 从文件名提取扩展名
     */
    private String extractFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 根据文件扩展名获取MIME类型（已废弃，使用魔数检测）
     * @deprecated 使用 detectMimeType(byte[]) 替代
     */
    @Deprecated
    private String getContentType(String fileExtension) {
        if (fileExtension == null) {
            return "application/octet-stream";
        }

        switch (fileExtension.toLowerCase()) {
            case "cgm":
                return "image/cgm";
            case "svg":
                return "image/svg+xml";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "tif":
            case "tiff":
                return "image/tiff";
            // 视频格式
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            // 音频格式
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "aac":
                return "audio/aac";
            case "flac":
                return "audio/flac";
            default:
                return "application/octet-stream";
        }
    }


    /**
     * 发送占位图（当ICN不存在或加载失败时）
     */
    private void sendPlaceholderImage(HttpServletResponse response, String message) throws Exception {
        response.setContentType("image/svg+xml");
        response.setCharacterEncoding("UTF-8");

        // 生成一个简单的SVG占位图
        String svg = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"200\" viewBox=\"0 0 300 200\">" +
            "  <rect width=\"300\" height=\"200\" fill=\"#f0f0f0\" stroke=\"#ccc\" stroke-width=\"2\"/>" +
            "  <text x=\"150\" y=\"100\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"14\" fill=\"#666\">" +
            "    %s" +
            "  </text>" +
            "</svg>",
            escapeXml(message)
        );

        response.getOutputStream().write(svg.getBytes("UTF-8"));
        response.getOutputStream().flush();
    }

    /**
     * XML转义（用于SVG文本）
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * 🔧 P1优化：公式实体转换为图符实体（消除代码重复）
     *
     * @param formulaEntity 公式实体（来自ietm_icn_formula表）
     * @return IetmIcnManage 适配对象（前端期望的结构）
     */
    private IetmIcnManage convertFormulaToManage(org.jeecg.modules.ietm.icnmanage.entity.IetmIcnFormula formulaEntity) {
        IetmIcnManage adapted = new IetmIcnManage();

        // 使用Spring BeanUtils复制同名字段（避免逐一赋值）
        org.springframework.beans.BeanUtils.copyProperties(formulaEntity, adapted);

        // ✅ 关键：标记类型为公式（前端依赖此字段判断）
        adapted.setIcnType("formula");

        // 构造虚拟附件对象（保持API兼容性，实际不使用）
        // 公式内容已存储在filecontent CLOB字段中，不需要真实附件
        if (formulaEntity.getFilecontent() != null && formulaEntity.getFilecontent().length > 0) {
            org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment attachment =
                new org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment();
            attachment.setFileName("formula.html");
            attachment.setFileSize(new java.math.BigDecimal(formulaEntity.getFilecontent().length));
            // 注意：此fileKey为虚拟标识，前端实际通过getIcnContent端点获取公式内容
            attachment.setFileKey("formula-" + formulaEntity.getId());
            adapted.setIetmAttachment(attachment);
        }

        return adapted;
    }

}

