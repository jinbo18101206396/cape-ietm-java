package org.jeecg.modules.ietm.ietmdatamodulemanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmFormVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmEditPropVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmProjectInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Api(tags = "数据模块管理")
@RestController
@RequestMapping("/ietm/datamodule")
public class IetmDataModuleController extends JeecgController<IetmDataModule, IIetmDataModuleService> {

    @Autowired
    private IIetmDataModuleService ietmDataModuleService;

    /**
     * 分页查询列表
     */
    @AutoLog(value = "数据模块管理-分页查询")
    @ApiOperation(value = "数据模块管理-分页查询", notes = "数据模块管理-分页查询")
    @GetMapping(value = "/list")
    public Result<IPage<IetmDataModule>> queryPageList(
            IetmDataModule dataModule,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "projectId", required = false) String projectId,
            @RequestParam(name = "cmNodeId", required = false) String cmNodeId,
            @RequestParam(name = "nodePath", required = false) String nodePath,
            @RequestParam(name = "showChildren", required = false) Boolean showChildren,
            HttpServletRequest req) {

        // 参数校验：必须提供projectId或cmNodeId
        if (projectId == null && cmNodeId == null) {
            return Result.error("请先选择项目或构型节点");
        }

        // 使用LambdaQueryWrapper避免字段名映射问题
        LambdaQueryWrapper<IetmDataModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IetmDataModule::getStatus, "1");
        queryWrapper.eq(IetmDataModule::getIsLatest, "1");

        // 根据projectId查询
        if (projectId != null && !projectId.isEmpty()) {
            queryWrapper.eq(IetmDataModule::getProjectId, projectId);
        }

        // 根据cmNodeId查询
        if (cmNodeId != null && !cmNodeId.isEmpty()) {
            if (showChildren != null && showChildren && nodePath != null && !nodePath.isEmpty()) {
                // 查询当前节点及其子节点（通过nodePath前缀匹配）
                queryWrapper.and(wrapper -> wrapper
                    .eq(IetmDataModule::getCmNodeId, cmNodeId)
                    .or()
                    .likeRight(IetmDataModule::getCmNodePath, nodePath + "/")
                );
            } else {
                // 只查询当前节点
                queryWrapper.eq(IetmDataModule::getCmNodeId, cmNodeId);
            }
        }

        queryWrapper.orderByDesc(IetmDataModule::getCreateTime);

        Page<IetmDataModule> page = new Page<>(pageNo, pageSize);
        IPage<IetmDataModule> pageList = ietmDataModuleService.page(page, queryWrapper);

        return Result.OK(pageList);
    }

    /**
     * 根据ID查询
     */
    @AutoLog(value = "数据模块管理-根据ID查询")
    @ApiOperation(value = "数据模块管理-根据ID查询", notes = "数据模块管理-根据ID查询")
    @GetMapping(value = "/queryById")
    public Result<IetmDataModule> queryById(@RequestParam(name = "id", required = true) String id) {
        IetmDataModule dataModule = ietmDataModuleService.queryById(id);
        if (dataModule == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(dataModule);
    }

    /**
     * 获取项目信息（包含SNS编码）
     * 用于新增DM时自动填充SNS字段
     */
    @ApiOperation(value = "数据模块管理-获取项目信息", notes = "根据构型节点ID获取项目信息（包含自动生成的SNS编码）")
    @GetMapping(value = "/getProjectInfo")
    public Result<DmProjectInfoVO> getProjectInfo(@RequestParam(name = "cmNodeId", required = true) String cmNodeId) {
        // 参数校验
        if (cmNodeId == null || cmNodeId.trim().isEmpty()) {
            return Result.error("构型节点ID不能为空");
        }

        try {
            DmProjectInfoVO projectInfo = ietmDataModuleService.getProjectInfo(cmNodeId);
            return Result.OK(projectInfo);
        } catch (Exception e) {
//            log.error("获取项目信息失败，cmNodeId={}", cmNodeId, e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 新增
     */
    @AutoLog(value = "数据模块管理-新增")
    @ApiOperation(value = "数据模块管理-新增", notes = "数据模块管理-新增")
    @PostMapping(value = "/add")
    public Result<String> add(@Valid @RequestBody DmFormVO formVO) {
        IetmDataModule dataModule = new IetmDataModule();

        // 将formVO属性复制到dataModule
        copyFormVOToEntity(formVO, dataModule);

        // 新增时设置默认值
        dataModule.setIsLatest("1");  // 新增默认为最新版本
        dataModule.setStatus("1");    // 状态默认为正常
        if (dataModule.getVersionType() == null) {
            dataModule.setVersionType("0");  // 默认为草稿版本
        }
        if (dataModule.getIssueNo() == null) {
            dataModule.setIssueNo("001");    // 默认发行编号
        }
        if (dataModule.getInWork() == null) {
            dataModule.setInWork("00");      // 默认在编版本号
        }
        if (dataModule.getSchema() == null) {
            dataModule.setSchema("J");       // 默认Schema为J
        }

        boolean success = ietmDataModuleService.saveDm(dataModule);
        if (success) {
            return Result.OK("新增成功！");
        } else {
            return Result.error("新增失败！");
        }
    }

    /**
     * 编辑
     */
    @AutoLog(value = "数据模块管理-编辑")
    @ApiOperation(value = "数据模块管理-编辑", notes = "数据模块管理-编辑")
    @PutMapping(value = "/edit")
    public Result<String> edit(@Valid @RequestBody DmFormVO formVO) {
        if (formVO.getId() == null) {
            return Result.error("编辑时ID不能为空！");
        }

        IetmDataModule dataModule = new IetmDataModule();
        dataModule.setId(formVO.getId());

        // 将formVO属性复制到dataModule
        copyFormVOToEntity(formVO, dataModule);

        boolean success = ietmDataModuleService.updateDm(dataModule);
        if (success) {
            return Result.OK("编辑成功！");
        } else {
            return Result.error("编辑失败！");
        }
    }

    /**
     * 将DmFormVO属性复制到IetmDataModule实体
     * @param formVO 前端表单对象
     * @param entity 数据库实体对象
     */
    private void copyFormVOToEntity(DmFormVO formVO, IetmDataModule entity) {
        // 基础字段
        entity.setProjectId(formVO.getProjectId());
        entity.setCmNodeId(formVO.getCmNodeId());
        entity.setCmNodePath(formVO.getCmNodePath());

        // DMC组成字段
        entity.setSchema(formVO.getSchema());
        entity.setSns(formVO.getSns());
        entity.setInfoCode(formVO.getInfoCode());
        entity.setInfoCodeVariant(formVO.getInfoCodeVariant());
        entity.setIetmLocationCode(formVO.getIetmLocationCode());
        entity.setLearnCode(formVO.getLearnCode());
        entity.setLearnCodeEventCode(formVO.getLearnCodeEventCode());
        entity.setYearOfChange(formVO.getYearOfChange());
        entity.setSeqNo(formVO.getSeqNo());
        entity.setLanguageIsoCode(formVO.getLanguageIsoCode());
        entity.setCountryIsoCode(formVO.getCountryIsoCode());

        // 发行方信息
        entity.setOriginator(formVO.getOriginator());
        entity.setOriginatorName(formVO.getOriginatorName());
        entity.setRpc(formVO.getRpc());
        entity.setRpcName(formVO.getRpcName());

        // 名称信息
        entity.setTechName(formVO.getTechName());
        entity.setInfoName(formVO.getInfoName());
        entity.setTechNameEn(formVO.getTechNameEn());
        entity.setInfoNameEn(formVO.getInfoNameEn());

        // 其他字段
        entity.setDmType(formVO.getDmType());
        entity.setSecurity(formVO.getSecurity());
        entity.setDmContent(formVO.getDmContent());
        entity.setRemark(formVO.getRemark());
        entity.setReason(formVO.getReason());
    }

    /**
     * 删除
     */
    @AutoLog(value = "数据模块管理-删除")
    @ApiOperation(value = "数据模块管理-删除", notes = "数据模块管理-删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        boolean success = ietmDataModuleService.deleteDm(id);
        if (success) {
            return Result.OK("删除成功！");
        } else {
            return Result.error("删除失败！");
        }
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "数据模块管理-批量删除")
    @ApiOperation(value = "数据模块管理-批量删除", notes = "数据模块管理-批量删除")
    @DeleteMapping(value = "/batchDelete")
    public Result<Map<String, Object>> batchDelete(@RequestParam(name = "ids", required = true) List<String> ids) {
        Map<String, Object> result = ietmDataModuleService.batchDelete(ids);
        return Result.OK(result);
    }

    /**
     * 签出
     */
    @AutoLog(value = "数据模块管理-签出")
    @ApiOperation(value = "数据模块管理-签出", notes = "数据模块管理-签出")
    @PostMapping(value = "/checkOut")
    public Result<String> checkOut(
            @ApiParam(value = "DM ID", required = true) @RequestParam String id,
            HttpServletRequest req) {
        String username = getUsername(req);
        boolean success = ietmDataModuleService.checkOut(id, username);
        if (success) {
            return Result.OK("签出成功！");
        } else {
            return Result.error("签出失败！");
        }
    }

    /**
     * 取消签出
     */
    @AutoLog(value = "数据模块管理-取消签出")
    @ApiOperation(value = "数据模块管理-取消签出", notes = "数据模块管理-取消签出")
    @PostMapping(value = "/cancelCheckOut")
    public Result<String> cancelCheckOut(
            @ApiParam(value = "DM ID", required = true) @RequestParam String id,
            HttpServletRequest req) {
        String username = getUsername(req);
        boolean success = ietmDataModuleService.cancelCheckOut(id, username);
        if (success) {
            return Result.OK("取消签出成功！");
        } else {
            return Result.error("取消签出失败！");
        }
    }

    /**
     * 签入
     */
    @AutoLog(value = "数据模块管理-签入")
    @ApiOperation(value = "数据模块管理-签入", notes = "数据模块管理-签入")
    @PostMapping(value = "/checkIn")
    public Result<String> checkIn(
            @ApiParam(value = "DM ID", required = true) @RequestParam String id,
            @ApiParam(value = "签入备注") @RequestParam(required = false) String comment,
            HttpServletRequest req) {
        String username = getUsername(req);
        boolean success = ietmDataModuleService.checkIn(id, username, comment);
        if (success) {
            return Result.OK("签入成功！");
        } else {
            return Result.error("签入失败！");
        }
    }

    /**
     * 发布
     */
    @AutoLog(value = "数据模块管理-发布")
    @ApiOperation(value = "数据模块管理-发布", notes = "数据模块管理-发布")
    @PostMapping(value = "/publish")
    public Result<String> publish(
            @ApiParam(value = "DM ID", required = true) @RequestParam String id,
            HttpServletRequest req) {
        String username = getUsername(req);
        boolean success = ietmDataModuleService.publishDm(id, username);
        if (success) {
            return Result.OK("发布成功！");
        } else {
            return Result.error("发布失败！");
        }
    }

    /**
     * 批量签出
     */
    @AutoLog(value = "数据模块管理-批量签出")
    @ApiOperation(value = "数据模块管理-批量签出", notes = "数据模块管理-批量签出")
    @PostMapping(value = "/batchCheckOut")
    public Result<Map<String, Object>> batchCheckOut(
            @ApiParam(value = "DM ID列表", required = true) @RequestParam List<String> ids,
            HttpServletRequest req) {
        String username = getUsername(req);
        Map<String, Object> result = ietmDataModuleService.batchCheckOut(ids, username);
        return Result.OK(result);
    }

    /**
     * 批量签入
     */
    @AutoLog(value = "数据模块管理-批量签入")
    @ApiOperation(value = "数据模块管理-批量签入", notes = "数据模块管理-批量签入")
    @PostMapping(value = "/batchCheckIn")
    public Result<Map<String, Object>> batchCheckIn(
            @ApiParam(value = "DM ID列表", required = true) @RequestParam List<String> ids,
            @ApiParam(value = "签入备注") @RequestParam(required = false) String comment,
            HttpServletRequest req) {
        String username = getUsername(req);
        Map<String, Object> result = ietmDataModuleService.batchCheckIn(ids, username, comment);
        return Result.OK(result);
    }

    /**
     * 查询历史版本
     */
    @AutoLog(value = "数据模块管理-查询历史版本")
    @ApiOperation(value = "数据模块管理-查询历史版本", notes = "数据模块管理-查询历史版本")
    @GetMapping(value = "/historyVersions")
    public Result<List<IetmDataModule>> queryHistoryVersions(
            @ApiParam(value = "SNS编号", required = true) @RequestParam String sns,
            @ApiParam(value = "信息代码", required = true) @RequestParam String infoCode,
            @ApiParam(value = "信息代码变体") @RequestParam(required = false) String infoCodeVariant) {
        List<IetmDataModule> list = ietmDataModuleService.queryHistoryVersions(sns, infoCode, infoCodeVariant);
        return Result.OK(list);
    }

    /**
     * 查询引用关系树
     */
    @AutoLog(value = "数据模块管理-查询引用关系树")
    @ApiOperation(value = "数据模块管理-查询引用关系树", notes = "数据模块管理-查询引用关系树")
    @GetMapping(value = "/referenceTree")
    public Result<List<Map<String, Object>>> queryReferenceTree(
            @ApiParam(value = "DM ID", required = true) @RequestParam String dmId,
            @ApiParam(value = "引用类型（out-出引用，in-入引用）", required = true) @RequestParam String refType) {
        List<Map<String, Object>> tree = ietmDataModuleService.queryReferenceTree(dmId, refType);
        return Result.OK(tree);
    }

    /**
     * 导入XML文件
     */
    @AutoLog(value = "数据模块管理-导入XML")
    @ApiOperation(value = "数据模块管理-导入XML", notes = "数据模块管理-导入XML")
    @PostMapping(value = "/importXml")
    public Result<Map<String, Object>> importXml(
            @ApiParam(value = "XML文件", required = true) @RequestParam("file") MultipartFile file,
            @ApiParam(value = "项目ID", required = true) @RequestParam String projectId) {
        Map<String, Object> result = ietmDataModuleService.importXml(file, projectId);
        return Result.OK(result);
    }

    /**
     * 导出XML文件
     */
    @AutoLog(value = "数据模块管理-导出XML")
    @ApiOperation(value = "数据模块管理-导出XML", notes = "数据模块管理-导出XML")
    @GetMapping(value = "/exportXml")
    public void exportXml(
            @ApiParam(value = "DM ID", required = true) @RequestParam String id,
            HttpServletResponse response) {
        ietmDataModuleService.exportXml(id, response);
    }

    /**
     * 导入ZIP压缩包
     */
    @AutoLog(value = "数据模块管理-导入ZIP")
    @ApiOperation(value = "数据模块管理-导入ZIP", notes = "数据模块管理-导入ZIP")
    @PostMapping(value = "/importZip")
    public Result<Map<String, Object>> importZip(
            @ApiParam(value = "ZIP文件", required = true) @RequestParam("file") MultipartFile file,
            @ApiParam(value = "项目ID", required = true) @RequestParam String projectId) {
        Map<String, Object> result = ietmDataModuleService.importZip(file, projectId);
        return Result.OK(result);
    }

    /**
     * DMC唯一性校验
     */
    @AutoLog(value = "数据模块管理-DMC校验")
    @ApiOperation(value = "数据模块管理-DMC校验", notes = "数据模块管理-DMC校验")
    @PostMapping(value = "/validateDmc")
    public Result<Boolean> validateDmc(@RequestBody IetmDataModule dataModule) {
        boolean exists = ietmDataModuleService.validateDmc(dataModule);
        if (exists) {
            return Result.OK(true);
        } else {
            return Result.OK(false);
        }
    }

    /**
     * 复制DM
     */
    @AutoLog(value = "数据模块管理-复制")
    @ApiOperation(value = "数据模块管理-复制", notes = "数据模块管理-复制")
    @PostMapping(value = "/copy")
    public Result<String> copyDm(
            @ApiParam(value = "源DM ID", required = true) @RequestParam String id,
            @ApiParam(value = "目标项目ID") @RequestParam(required = false) String targetProjectId,
            @ApiParam(value = "复制类型（0=仅复制属性，1=创建新版本链）", required = true) @RequestParam(defaultValue = "0") Integer copyType,
            HttpServletRequest req) {
        String username = getUsername(req);
        String newId = ietmDataModuleService.copyDm(id, targetProjectId, copyType, username);
        String message = copyType == 1 ? "复制新建成功，新DM ID：" + newId : "复制成功，新DM ID：" + newId;
        return Result.OK(message);
    }

    /**
     * 启动工作流
     */
    @AutoLog(value = "数据模块管理-启动工作流")
    @ApiOperation(value = "数据模块管理-启动工作流", notes = "数据模块管理-启动工作流")
    @PostMapping(value = "/startWorkflow")
    public Result<String> startWorkflow(
            @ApiParam(value = "DM ID", required = true) @RequestParam String id,
            @ApiParam(value = "流程定义Key", required = true) @RequestParam String processKey,
            HttpServletRequest req) {
        String username = getUsername(req);
        String workflowInstanceId = ietmDataModuleService.startWorkflow(id, processKey, username);
        return Result.OK("工作流启动成功，实例ID：" + workflowInstanceId);
    }

    /**
     * 重启工作流（发布后）
     */
    @AutoLog(value = "数据模块管理-重启工作流")
    @ApiOperation(value = "数据模块管理-重启工作流", notes = "发布后重新启动工作流")
    @PostMapping(value = "/restartWorkflow")
    public Result<String> restartWorkflow(
            @ApiParam(value = "DM ID", required = true) @RequestParam String id,
            @ApiParam(value = "流程定义Key", required = true) @RequestParam String processKey,
            HttpServletRequest req) {
        String username = getUsername(req);
        String workflowInstanceId = ietmDataModuleService.startWorkflow(id, processKey, username);
        return Result.OK("工作流重启成功，实例ID：" + workflowInstanceId);
    }

    /**
     * 预览DM
     */
    @AutoLog(value = "数据模块管理-预览DM")
    @ApiOperation(value = "数据模块管理-预览DM", notes = "预览DM渲染结果")
    @GetMapping(value = "/previewDm")
    public void previewDm(
            @ApiParam(value = "DM ID", required = true) @RequestParam String id,
            HttpServletResponse response) {
        ietmDataModuleService.previewDm(id, response);
    }

    /**
     * 搜索DM
     */
    @AutoLog(value = "数据模块管理-搜索DM")
    @ApiOperation(value = "数据模块管理-搜索DM", notes = "全文搜索DM")
    @PostMapping(value = "/searchDm")
    public Result<List<IetmDataModule>> searchDm(
            @ApiParam(value = "关键词", required = true) @RequestParam String keyword,
            @ApiParam(value = "项目ID") @RequestParam(required = false) String projectId) {
        List<IetmDataModule> list = ietmDataModuleService.searchDm(keyword, projectId);
        return Result.OK(list);
    }

    /**
     * 查询DM资源列表
     */
    @AutoLog(value = "数据模块管理-查询资源")
    @ApiOperation(value = "数据模块管理-查询资源", notes = "查询DM关联的资源列表")
    @GetMapping(value = "/queryDmResources")
    public Result<List<Map<String, Object>>> queryDmResources(
            @ApiParam(value = "模块ID", required = true) @RequestParam String dmId) {
        List<Map<String, Object>> list = ietmDataModuleService.queryDmResources(dmId);
        return Result.OK(list);
    }

    /**
     * 添加DM资源
     * 注意：前端需要先调用通用文件上传接口获取fileId，然后调用此接口保存资源关联
     */
    @AutoLog(value = "数据模块管理-添加资源")
    @ApiOperation(value = "数据模块管理-添加资源", notes = "添加DM关联资源")
    @PostMapping(value = "/saveDmResource")
    public Result<String> saveDmResource(
            @ApiParam(value = "模块ID", required = true) @RequestParam String dmId,
            @ApiParam(value = "文件ID", required = true) @RequestParam String fileId,
            @ApiParam(value = "资源名称", required = true) @RequestParam String resourceName,
            @ApiParam(value = "文件大小（字节）") @RequestParam(required = false) Long fileSize,
            @ApiParam(value = "说明") @RequestParam(required = false) String comment) {

        boolean success = ietmDataModuleService.saveDmResource(dmId, fileId, resourceName, fileSize, comment);
        if (success) {
            return Result.OK("资源添加成功");
        } else {
            return Result.error("资源添加失败");
        }
    }

    /**
     * 更新DM资源
     */
    @AutoLog(value = "数据模块管理-更新资源")
    @ApiOperation(value = "数据模块管理-更新资源", notes = "更新DM资源说明")
    @PostMapping(value = "/updateDmResource")
    public Result<String> updateDmResource(
            @ApiParam(value = "资源ID", required = true) @RequestParam String id,
            @ApiParam(value = "说明") @RequestParam String comment) {
        boolean success = ietmDataModuleService.updateDmResource(id, comment);
        if (success) {
            return Result.OK("资源更新成功");
        } else {
            return Result.error("资源更新失败");
        }
    }

    /**
     * 删除DM资源
     */
    @AutoLog(value = "数据模块管理-删除资源")
    @ApiOperation(value = "数据模块管理-删除资源", notes = "删除DM关联资源（保留文件）")
    @DeleteMapping(value = "/deleteDmResource")
    public Result<String> deleteDmResource(
            @ApiParam(value = "资源ID", required = true) @RequestParam String id) {
        boolean success = ietmDataModuleService.deleteDmResource(id);
        if (success) {
            return Result.OK("资源删除成功");
        } else {
            return Result.error("资源删除失败");
        }
    }

    /**
     * 删除DM资源及文件
     */
    @AutoLog(value = "数据模块管理-删除资源及文件")
    @ApiOperation(value = "数据模块管理-删除资源及文件", notes = "删除DM关联资源及物理文件")
    @DeleteMapping(value = "/deleteDmResourceFile")
    public Result<String> deleteDmResourceFile(
            @ApiParam(value = "资源ID", required = true) @RequestParam String id) {
        boolean success = ietmDataModuleService.deleteDmResourceFile(id);
        if (success) {
            return Result.OK("资源和文件删除成功");
        } else {
            return Result.error("资源和文件删除失败");
        }
    }

    /**
     * 计算DMC编码
     * 重新计算指定节点下所有DM的DMC编码
     */
    @AutoLog(value = "数据模块管理-计算DMC编码")
    @ApiOperation(value = "数据模块管理-计算DMC编码", notes = "重新计算指定节点下所有DM的DMC编码")
    @GetMapping(value = "/calculateDmc")
    public Result<Integer> calculateDmc(
            @ApiParam(value = "项目ID", required = true) @RequestParam String projectId,
            @ApiParam(value = "构型节点ID", required = true) @RequestParam String cmNodeId) {


        try {
            // 查询该节点下所有DM
            LambdaQueryWrapper<IetmDataModule> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(IetmDataModule::getProjectId, projectId);
            queryWrapper.eq(IetmDataModule::getCmNodeId, cmNodeId);
            queryWrapper.eq(IetmDataModule::getStatus, "1");
            queryWrapper.eq(IetmDataModule::getIsLatest, "1");

            List<IetmDataModule> dmList = ietmDataModuleService.list(queryWrapper);

            int count = 0;
            for (IetmDataModule dm : dmList) {
                // 重新生成DMC编码（DMC是动态计算的，不需要存储到数据库）
                String newDmc = ietmDataModuleService.generateDmc(dm);
                // 注意：Entity没有dmCode字段，DMC由generateDmc计算得出
                // if (newDmc != null && !newDmc.equals(dm.getDmCode())) {
                //     dm.setDmCode(newDmc);
                //     ietmDataModuleService.updateById(dm);
                //     count++;
                // }
                if (newDmc != null) {
                    count++;
                }
            }

            return Result.OK("计算完成", count);

        } catch (Exception e) {
            return Result.error("计算DMC编码失败: " + e.getMessage());
        }
    }

    /**
     * 编辑DM属性（技术名称/信息名称）
     * 已签出（本人）→ 直接更新，inWork不变
     * 未签出       → 自动签出 + inWork+1 + 更新属性
     */
    @AutoLog(value = "数据模块管理-编辑DM属性")
    @ApiOperation(value = "数据模块管理-编辑DM属性", notes = "仅修改技术名称和信息名称，根据签出状态决定是否升级版本")
    @PutMapping(value = "/editProp/{id}")
    public Result<?> editProp(
            @ApiParam(value = "DM主键ID", required = true) @PathVariable String id,
            @Valid @RequestBody DmEditPropVO vo,
            HttpServletRequest req) {
        String currentUser = getUsername(req);
        return ietmDataModuleService.editProp(id, vo, currentUser);
    }

    /**
     * 获取当前用户名
     */
    private String getUsername(HttpServletRequest req) {
        String username = (String) req.getAttribute("username");
        if (username == null) {
            username = req.getHeader("X-Access-Token");
        }
        return username;
    }
}
