package org.jeecg.modules.ietm.ietmdatamodulemanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.constant.DmConstants;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.constant.DmConstants;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmFormVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmEditPropVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmProjectInfoVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmCopyVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmcUniqueCheckVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.exception.DmValidationException;
import org.jeecg.common.system.vo.LoginUser;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Api(tags = "数据模块管理")
@RestController
@RequestMapping("/ietm/datamodule")
public class IetmDataModuleController extends JeecgController<IetmDataModule, IIetmDataModuleService> {

    @Autowired
    private IIetmDataModuleService ietmDataModuleService;

    @Autowired
    private IetmDataModuleMapper ietmDataModuleMapper;

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

        // 使用自定义Mapper方法，JOIN v_wf_instance 视图获取动态流程步骤
        Page<IetmDataModule> page = new Page<>(pageNo, pageSize);
        IPage<IetmDataModule> pageList = ietmDataModuleMapper.selectPageWithFlow(
                page,
                projectId,
                cmNodeId,
                nodePath,           // 前端传nodePath，Mapper需要cmNodePath
                showChildren        // 前端传showChildren，Mapper需要includeChildren
        );

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
        dataModule.setIsLatest(DmConstants.IS_LATEST_YES);  // 新增默认为最新版本
        dataModule.setStatus(DmConstants.STATUS_VALID);    // 状态默认为正常
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
        entity.setLearnEventCode(formVO.getLearnEventCode());
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
        // ✅ 修复：Service返回新记录的ID
        String newId = ietmDataModuleService.checkOut(id, username);
        if (newId != null) {
            // ✅ 修复：返回新记录的ID给前端，前端需要用新ID重新选中
            return Result.OK("签出成功！", newId);
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
        String originalId = ietmDataModuleService.cancelCheckOut(id, username);
        if (originalId != null) {
            return Result.OK("取消签出成功！", originalId);
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
     * 批量查询签出状态
     */
    @AutoLog(value = "数据模块管理-批量查询签出状态")
    @ApiOperation(value = "数据模块管理-批量查询签出状态", notes = "批量查询DM的签出状态")
    @PostMapping(value = "/batchCheckoutStatus")
    public Result<Map<String, Map<String, String>>> batchCheckoutStatus(
            @ApiParam(value = "DM ID列表", required = true) @RequestBody List<String> dmIds) {
        if (dmIds == null || dmIds.isEmpty()) {
            return Result.error("DM ID列表不能为空");
        }

        Map<String, Map<String, String>> result = new HashMap<>();

        // 批量查询签出状态
        LambdaQueryWrapper<IetmDataModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(IetmDataModule::getId, dmIds);
        queryWrapper.select(IetmDataModule::getId, IetmDataModule::getCheckoutUser, IetmDataModule::getCheckoutTime);

        List<IetmDataModule> dmList = ietmDataModuleService.list(queryWrapper);

        for (IetmDataModule dm : dmList) {
            Map<String, String> statusMap = new HashMap<>();
            statusMap.put("checkoutUser", dm.getCheckoutUser());
            statusMap.put("checkoutTime", dm.getCheckoutTime() != null ? dm.getCheckoutTime().toString() : null);
            result.put(dm.getId(), statusMap);
        }

        return Result.OK(result);
    }

    /**
     * 发布
     */
    @AutoLog(value = "数据模块管理-发布")
    @ApiOperation(value = "数据模块管理-发布", notes = "数据模块管理-发布")
    @PostMapping(value = "/publish")
    public Result<Map<String, Object>> publish(
            @RequestBody Map<String, String> params,
            HttpServletRequest req) {
        String id = params.get("id");
        if (id == null || id.isEmpty()) {
            return Result.error("DM ID不能为空");
        }
        String username = getUsername(req);

        try {
            boolean success = ietmDataModuleService.publishDm(id, username);
            if (success) {
                return Result.OK("发布成功！");
            } else {
                return Result.error("发布失败！");
            }
        } catch (DmValidationException e) {
            // 捕获校验异常，返回结构化错误信息
            log.warn("发布失败：XSD校验不通过，错误数：{}", e.getErrors() != null ? e.getErrors().size() : 0);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errors", e.getErrors());
            errorData.put("message", e.getMessage());
            // 使用 error(String msg, T data) 并手动设置 code
            Result<Map<String, Object>> result = Result.error(e.getMessage(), errorData);
            result.setCode(40001);  // 设置自定义错误码，前端根据此code展示详细错误弹窗
            return result;
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
     * TODO-权限: @RequiresPermissions("ietm:datamodule:history") 需在菜单/权限表登记后启用，否则返回403
     */
    @AutoLog(value = "数据模块管理-查询历史版本")
    @ApiOperation(value = "数据模块管理-查询历史版本", notes = "查询同一DMC的所有历史版本（轻量列表）")
    @GetMapping(value = "/historyVersions")
    public Result<List<IetmDataModule>> queryHistoryVersions(
            @ApiParam(value = "项目ID") @RequestParam(required = false) String projectId,
            @ApiParam(value = "SNS编号", required = true) @RequestParam String sns,
            @ApiParam(value = "信息代码", required = true) @RequestParam String infoCode,
            @ApiParam(value = "信息代码变体") @RequestParam(required = false) String infoCodeVariant,
            @ApiParam(value = "位置代码") @RequestParam(required = false) String ietmLocationCode,
            @ApiParam(value = "仅显示发布版本(version_type=1)") @RequestParam(required = false, defaultValue = "false") Boolean onlyPublished) {
        List<IetmDataModule> list = ietmDataModuleService.queryHistoryVersions(
                projectId, sns, infoCode, infoCodeVariant, ietmLocationCode, onlyPublished);
        return Result.OK(list);
    }

    /**
     * 版本内容对比（按需取两版本XML，供前端MergeView渲染）
     */
    @AutoLog(value = "数据模块管理-版本内容对比")
    @ApiOperation(value = "数据模块管理-版本内容对比", notes = "返回两个版本的XML原文用于差异比对")
    @GetMapping(value = "/compareVersions")
    public Result<Map<String, Object>> compareVersions(
            @ApiParam(value = "源版本ID", required = true) @RequestParam String sourceId,
            @ApiParam(value = "目标版本ID", required = true) @RequestParam String targetId) {
        Map<String, Object> data = ietmDataModuleService.compareVersions(sourceId, targetId);
        if (data.isEmpty()) {
            return Result.error("对比版本不存在或已删除");
        }
        return Result.OK(data);
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
     * 查询引用链路径
     */
    @AutoLog(value = "数据模块管理-查询引用链路径")
    @ApiOperation(value = "数据模块管理-查询引用链路径", notes = "数据模块管理-查询引用链路径")
    @GetMapping(value = "/referenceChain")
    public Result<List<Map<String, Object>>> queryReferenceChain(
            @ApiParam(value = "根DM ID", required = true) @RequestParam String rootDmId,
            @ApiParam(value = "目标DM ID", required = true) @RequestParam String targetDmId,
            @ApiParam(value = "引用类型：out-出引用，in-入引用", required = false, defaultValue = "out") @RequestParam(defaultValue = "out") String refType) {
        List<Map<String, Object>> chain = ietmDataModuleService.queryReferenceChain(rootDmId, targetDmId, refType);
        return Result.OK(chain);
    }

    /**
     * 计算指定DM的引用关系
     * <p>
     * 解析 dm_content XML，提取 dmRef/graphic/multimedia 引用，更新 ietm_dm_reference 表，
     * 并刷新 ref_count / refed_count 统计字段。
     * <ul>
     *   <li>id 为普通 DM 主键：计算单个 DM</li>
     *   <li>id 为字面量 {@code "all"}：批量计算全部有效 DM（管理员功能，耗时较长）</li>
     * </ul>
     */
    @AutoLog(value = "数据模块管理-计算DM引用关系")
    @ApiOperation(value = "数据模块管理-计算DM引用关系", notes = "解析XML内容，提取引用关系并存储到ietm_dm_reference表")
    @PostMapping(value = "/calcref/{id}")
    public Result<Map<String, Object>> calculateDmReferences(
            @ApiParam(value = "DM主键ID，或 'all' 批量计算所有DM", required = true)
            @PathVariable("id") String id) {
        log.debug("calcref接口被访问，id={}, 时间={}", id, new java.util.Date());
        try {
            if ("all".equalsIgnoreCase(id)) {
                log.warn("calcref/all 批量计算触发");
                Map<String, Object> result = ietmDataModuleService.calculateAllDmReferences(100);
                log.info("批量计算完成，result={}", result);
                return Result.OK("批量计算完成", result);
            }
            log.warn("calcref/{} 单个计算触发", id);
            Map<String, Object> result = ietmDataModuleService.calculateDmReferences(id);
            return Result.OK("计算引用关系成功", result);
        } catch (Exception e) {
            log.error("calcref失败，id={}", id, e);
            return Result.error("计算引用关系失败：" + e.getMessage());
        }
    }

    /**
     * 批量修复 DMC 与版本号不一致的数据
     * <p>用于修复 editProp 历史遗留的版本号与 DMC 不一致问题</p>
     */
    @AutoLog(value = "数据模块管理-修复DMC不一致")
    @ApiOperation(value = "数据模块管理-修复DMC不一致", notes = "批量修复版本号字段与DMC编码不一致的历史数据")
    @PostMapping(value = "/fixDmc")
    @RequiresRoles("admin")  // 仅管理员可操作
    public Result<Map<String, Object>> fixInconsistentDmc(
            @ApiParam(value = "最多修复记录数", required = false, defaultValue = "1000")
            @RequestParam(required = false, defaultValue = "1000") int limit) {
        log.info("管理员触发批量修复 DMC，limit={}", limit);
        try {
            Map<String, Object> result = ietmDataModuleService.fixInconsistentDmc(limit);
            return Result.OK("修复完成", result);
        } catch (Exception e) {
            log.error("批量修复 DMC 失败", e);
            return Result.error("修复失败：" + e.getMessage());
        }
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
     * XML内容校验
     */
    @AutoLog(value = "数据模块管理-XML内容校验")
    @ApiOperation(value = "数据模块管理-XML内容校验", notes = "数据模块管理-XML内容校验")
    @PostMapping(value = "/validateContent")
    public Result<Map<String, Object>> validateContent(@RequestBody Map<String, String> params) {
        String content = params.get("content");
        Map<String, Object> result = ietmDataModuleService.validateXmlContent(content);
        return Result.OK(result);
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
     *
     * 🔧 修复：前端使用postAction发送JSON Body，后端需要用@RequestBody接收
     * 问题：原来用@RequestParam无法接收JSON参数，导致"Required request parameter 'id' is not present"错误
     */
    @AutoLog(value = "数据模块管理-启动工作流")
    @ApiOperation(value = "数据模块管理-启动工作流", notes = "数据模块管理-启动工作流")
    @PostMapping(value = "/startWorkflow")
    public Result<String> startWorkflow(@RequestBody Map<String, String> params, HttpServletRequest req) {
        String id = params.get("id");
        String processKey = params.get("processKey");

        if (id == null || id.isEmpty()) {
            return Result.error("DM ID不能为空");
        }
        if (processKey == null || processKey.isEmpty()) {
            return Result.error("流程定义Key不能为空");
        }

        String username = getUsername(req);
        String workflowInstanceId = ietmDataModuleService.startWorkflow(id, processKey, username);
        return Result.OK("工作流启动成功，实例ID：" + workflowInstanceId);
    }

    /**
     * 重启工作流（发布后）
     *
     * 🔧 修复记录：
     * <ul>
     *   <li>2026-08-22：修复参数绑定问题，前端使用postAction发送JSON Body，后端需要用@RequestBody接收</li>
     *   <li>2026-08-23：标记为废弃，前端改用批量启动流程对话框（让用户选择处理人，对标旧系统）</li>
     * </ul>
     *
     * @deprecated 前端已改为使用 {@link org.jeecg.modules.ietm.workflow.controller.WfInstanceController#batchStartFlow}
     *             统一通过批量启动流程接口，让用户选择处理人（对标旧系统业务逻辑）。
     *             本接口保留用于向后兼容，但不推荐直接调用。
     *
     * 问题：原来用@RequestParam无法接收JSON参数，导致"Required request parameter 'id' is not present"错误
     */
    @Deprecated
    @AutoLog(value = "数据模块管理-重启工作流")
    @ApiOperation(value = "数据模块管理-重启工作流", notes = "发布后重新启动工作流（已废弃，请使用批量启动流程接口）")
    @PostMapping(value = "/restartWorkflow")
    public Result<String> restartWorkflow(@RequestBody Map<String, String> params, HttpServletRequest req) {
        String id = params.get("id");
        String processKey = params.get("processKey");

        if (id == null || id.isEmpty()) {
            return Result.error("DM ID不能为空");
        }
        if (processKey == null || processKey.isEmpty()) {
            return Result.error("流程定义Key不能为空");
        }

        String username = getUsername(req);
        try {
            String workflowInstanceId = ietmDataModuleService.startWorkflow(id, processKey, username);
            return Result.OK("工作流重启成功，实例ID：" + workflowInstanceId);
        } catch (Exception e) {
            log.error("重启工作流失败，DM ID：{}", id, e);
            return Result.error("启动工作流失败：" + e.getMessage());
        }
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
                // 重新生成DMC编码，与存储值比对；不一致则更新落库（dmcCode 字段确实存储）
                String newDmc = ietmDataModuleService.generateDmc(dm);
                if (newDmc != null && !newDmc.equals(dm.getDmcCode())) {
                    dm.setDmcCode(newDmc);
                    ietmDataModuleService.updateById(dm);
                    count++;
                }
            }

            return Result.OK("计算完成，更新" + count + "条", count);

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
     * 获取当前登录用户名
     * 优先从 Shiro SecurityUtils 获取，确保返回真实的用户名而不是 Token
     * 限制长度不超过50个字符（数据库字段限制）
     */
    private String getUsername(HttpServletRequest req) {
        String username = null;

        try {
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            if (loginUser != null && loginUser.getUsername() != null) {
                username = loginUser.getUsername();
            }
        } catch (Exception e) {
            // 如果 Shiro 获取失败，尝试从请求属性获取
        }

        // 降级方案：从请求属性获取
        if (username == null) {
            username = (String) req.getAttribute("username");
        }

        // 最后兜底：返回默认用户名（避免返回长 token）
        if (username == null) {
            username = "system";
        }

        // 🔧 关键修复：截断用户名，避免超出数据库字段长度限制（VARCHAR2(50)）
        if (username.length() > 50) {
            username = username.substring(0, 50);
        }

        return username;
    }

    // ==================== 复制DM相关接口 ====================

    /**
     * 复制DM（校验是否可复制）
     */
    @AutoLog(value = "数据模块管理-复制DM")
    @ApiOperation(value = "复制DM", notes = "校验DM是否可复制")
    @GetMapping(value = "/copyDm")
    public Result<?> copyDm(@ApiParam(value = "被复制DM的ID", required = true)
                            @RequestParam(name = "dmId", required = true) String dmId) {
        return ietmDataModuleService.copyDm(dmId);
    }

    /**
     * 复制新建DM（核心方法）
     */
    @AutoLog(value = "数据模块管理-复制新建DM")
    @ApiOperation(value = "复制新建DM", notes = "基于源DM创建新DM")
    @PostMapping(value = "/copyAndCreateDm")
    public Result<?> copyAndCreateDm(@Valid @RequestBody DmCopyVO vo) {
        return ietmDataModuleService.copyAndCreateDm(vo);
    }

    /**
     * 计算SNS编码（供前端预览）
     */
    @ApiOperation(value = "计算SNS", notes = "根据构型节点计算SNS编码")
    @GetMapping(value = "/calculateSns")
    public Result<?> calculateSns(@ApiParam(value = "构型节点ID", required = true)
                                  @RequestParam(name = "cmNodeId", required = true) String cmNodeId) {
        try {
            // 使用已存在的getProjectInfo方法中的SNS计算逻辑
            DmProjectInfoVO projectInfo = ietmDataModuleService.getProjectInfo(cmNodeId);
            return Result.OK("计算成功", projectInfo.getSns());
        } catch (Exception e) {
            return Result.error("计算SNS失败：" + e.getMessage());
        }
    }

    /**
     * DMC查重（优化版）
     * 检查根据输入参数生成的 DMC 编码是否已存在
     */
    @ApiOperation(value = "DMC查重", notes = "检查DMC编码是否唯一（支持完整参数）")
    @PostMapping(value = "/checkDmcUnique")
    public Result<?> checkDmcUnique(@Valid @RequestBody DmcUniqueCheckVO vo) {
        String duplicateDmc = ietmDataModuleService.checkDmcUnique(vo);

        if (duplicateDmc == null) {
            return Result.OK("DMC编码唯一", true);
        } else {
            return Result.error("DMC编码重复：" + duplicateDmc);
        }
    }

    /**
     * 提取技术名称（从节点名称中提取空格后的部分）
     */
    @ApiOperation(value = "提取技术名称", notes = "从节点名称中提取空格后的部分")
    @GetMapping(value = "/extractTechName")
    public Result<?> extractTechName(@ApiParam(value = "节点名称（格式：编码 名称）", required = true)
                                     @RequestParam(name = "nodeName", required = true) String nodeName) {
        String techName = ietmDataModuleService.extractTechName(nodeName);
        return Result.OK("提取成功", techName);
    }

    /**
     * 引用DM弹窗-分页查询（§14.5）
     */
    @AutoLog(value = "引用DM弹窗-分页查询")
    @ApiOperation(value = "引用DM弹窗-分页查询", notes = "§14.5 引用DM弹窗使用，返回最新版DM列表")
    @GetMapping(value = "/listForDialog")
    public Result<IPage<IetmDataModule>> listForDialog(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "cmNodeId", required = false) String cmNodeId,
            @RequestParam(name = "cmNodePath", required = false) String cmNodePath,
            @RequestParam(name = "includeChildren", required = false, defaultValue = "false") Boolean includeChildren,
            @RequestParam(name = "onlyIssued", required = false, defaultValue = "false") Boolean onlyIssued,
            @RequestParam(name = "dmc", required = false) String dmc,
            @RequestParam(name = "techName", required = false) String techName,
            @RequestParam(name = "infoName", required = false) String infoName,
            @RequestParam(name = "dmTypeName", required = false) String dmTypeName) {

        Page<IetmDataModule> page = new Page<>(pageNo, pageSize);
        IPage<IetmDataModule> pageList = ietmDataModuleMapper.selectPageForDialog(
                page,
                cmNodeId,
                cmNodePath,
                includeChildren,
                onlyIssued,
                dmc,
                techName,
                infoName,
                dmTypeName
        );
        return Result.OK(pageList);
    }

    /**
     * 预览DM（列表页专用）
     * 复用编辑器预览的DmXsltTransformer转换引擎
     *
     * @param id DM主键ID
     * @param request HTTP请求（获取contextPath用于ICN路径构建）
     * @return {flag, html, message}
     */
    @AutoLog(value = "数据模块管理-预览DM")
    @ApiOperation(value = "预览DM", notes = "预览已保存的DM内容（复用编辑器预览引擎）")
    @GetMapping(value = "/preview/{id}")
    public Result<Map<String, Object>> preview(
            @PathVariable("id") String id,
            HttpServletRequest request) {

        try {
            // 1. 查询dmcontent（独立查询，避免大字段随列表加载）
            long queryStart = System.currentTimeMillis();
            String dmContent = ietmDataModuleMapper.getDmcontentById(id);
            long queryTime = System.currentTimeMillis() - queryStart;

            if (dmContent == null || dmContent.trim().isEmpty()) {
                Map<String, Object> ret = new HashMap<>();
                ret.put("flag", "null");
                ret.put("message", "DM内容为空");
                return Result.OK(ret);
            }

            // 2. 复用编辑器预览的转换逻辑（DmXsltTransformer）
            long transformStart = System.currentTimeMillis();
            String html = DmXmlHelper.renderHtml(dmContent, request.getContextPath());

            Map<String, Object> ret = new HashMap<>();
            if (html == null || html.isEmpty()) {
                ret.put("flag", "noxsl");
                ret.put("message", "无解析引擎");
            } else {
                ret.put("flag", "success");
                ret.put("html", html);
            }
            return Result.OK(ret);
        } catch (Exception e) {
            Map<String, Object> ret = new HashMap<>();
            ret.put("flag", "error");
            ret.put("message", "预览失败: " + e.getMessage());
            return Result.OK(ret);
        }
    }

    // ==================== 构型树节点级DM操作（西区三功能按钮）====================

    /**
     * 查询节点下的所有DM（供复制节点DM使用）
     */
    @AutoLog(value = "数据模块管理-查询节点DM")
    @ApiOperation(value = "查询节点下所有DM", notes = "根据构型节点ID查询该节点下的所有最新版DM")
    @GetMapping(value = "/queryByNodeId")
    public Result<?> queryByNodeId(@ApiParam(value = "构型节点ID", required = true)
                                   @RequestParam(name = "cmNodeId", required = true) String cmNodeId) {
        LambdaQueryWrapper<IetmDataModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IetmDataModule::getCmNodeId, cmNodeId)
                    .eq(IetmDataModule::getIsLatest, "1")
                    .eq(IetmDataModule::getStatus, "1")
                    .orderByAsc(IetmDataModule::getInfoCode);

        List<IetmDataModule> dmList = ietmDataModuleService.list(queryWrapper);
        return Result.OK("查询成功", dmList);
    }

    /**
     * 批量复制DM到目标节点（复用copyAndCreateDm逻辑）
     */
    @AutoLog(value = "数据模块管理-批量复制DM")
    @ApiOperation(value = "批量复制DM到目标节点", notes = "将源节点下的所有DM批量复制到目标节点")
    @GetMapping(value = "/batchCopyToNode")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchCopyToNode(@ApiParam(value = "源节点ID", required = true)
                                     @RequestParam(name = "sourceCmNodeId", required = true) String sourceCmNodeId,
                                     @ApiParam(value = "目标节点ID", required = true)
                                     @RequestParam(name = "targetCmNodeId", required = true) String targetCmNodeId,
                                     @ApiParam(value = "目标节点名称", required = true)
                                     @RequestParam(name = "targetCmNodeName", required = true) String targetCmNodeName) {
        try {
            // 1. 查询源节点下的所有DM
            LambdaQueryWrapper<IetmDataModule> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(IetmDataModule::getCmNodeId, sourceCmNodeId)
                       .eq(IetmDataModule::getIsLatest, "1")
                       .eq(IetmDataModule::getStatus, "1");

            List<IetmDataModule> sourceDms = ietmDataModuleService.list(queryWrapper);

            if (sourceDms == null || sourceDms.isEmpty()) {
                return Result.error("源节点下没有可复制的DM");
            }

            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMsg = new StringBuilder();

            // 2. 逐个复制DM（复用copyAndCreateDm的核心逻辑）
            for (IetmDataModule sourceDm : sourceDms) {
                try {
                    // 构建DmCopyVO（复用现有VO）
                    DmCopyVO vo = new DmCopyVO();
                    vo.setSourceDmId(sourceDm.getId());
                    vo.setTargetCmNodeId(targetCmNodeId);
                    vo.setTargetCmNodeName(targetCmNodeName);

                    // 调用现有的复制方法（100%复用）
                    Result<?> copyResult = ietmDataModuleService.copyAndCreateDm(vo);

                    if (copyResult.isSuccess()) {
                        successCount++;
                    } else {
                        failCount++;
                        errorMsg.append(sourceDm.getDmcCode()).append(": ").append(copyResult.getMessage()).append("; ");
                    }
                } catch (Exception e) {
                    failCount++;
                    errorMsg.append(sourceDm.getDmcCode()).append(": ").append(e.getMessage()).append("; ");
                }
            }

            // 3. 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("total", sourceDms.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("errors", errorMsg.toString());

            if (failCount == 0) {
                return Result.OK("批量复制成功，共复制 " + successCount + " 个DM", result);
            } else if (successCount == 0) {
                return Result.error("批量复制全部失败：" + errorMsg.toString());
            } else {
                return Result.OK("批量复制部分成功：成功 " + successCount + " 个，失败 " + failCount + " 个", result);
            }
        } catch (Exception e) {
            return Result.error("批量复制失败：" + e.getMessage());
        }
    }

    /**
     * 计算节点下所有DM的引用信息（统计）
     * BUG修复：list() 不加载 CLOB，需从 ietm_dm_reference 表统计
     */
    @AutoLog(value = "数据模块管理-计算引用信息")
    @ApiOperation(value = "计算节点引用信息", notes = "统计节点下所有DM的引用关系")
    @GetMapping(value = "/calcNodeRefInfo")
    public Result<?> calcNodeRefInfo(@ApiParam(value = "构型节点ID", required = true)
                                     @RequestParam(name = "cmNodeId", required = true) String cmNodeId,
                                     @ApiParam(value = "是否包含子节点", required = false)
                                     @RequestParam(name = "includeChildren", required = false, defaultValue = "false") Boolean includeChildren) {
        try {
            // 查询节点下的所有DM（只查 ID，不加载 dm_content）
            LambdaQueryWrapper<IetmDataModule> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(IetmDataModule::getId, IetmDataModule::getDmcCode,
                              IetmDataModule::getRefCount);  // 只查必要字段

            if (includeChildren != null && includeChildren) {
                // 包含子节点：使用层级查询获取当前节点及所有子孙节点的DM
                queryWrapper.apply("cm_node_id IN (SELECT id FROM ietm_project_configuration_management " +
                                 "START WITH id = {0} CONNECT BY PRIOR id = pid)", cmNodeId);
            } else {
                // 只查询当前节点
                queryWrapper.eq(IetmDataModule::getCmNodeId, cmNodeId);
            }

            queryWrapper.eq(IetmDataModule::getIsLatest, "1")
                       .eq(IetmDataModule::getStatus, "1");

            List<IetmDataModule> dmList = ietmDataModuleService.list(queryWrapper);

            if (dmList == null || dmList.isEmpty()) {
                return Result.OK("节点下没有DM", new HashMap<>());
            }

            // 统计引用信息（基于 ref_count 字段，无需加载 dm_content）
            int totalDms = dmList.size();
            int hasRefCount = 0;

            for (IetmDataModule dm : dmList) {
                Integer refCount = dm.getRefCount();
                if (refCount != null && refCount > 0) {
                    hasRefCount++;
                }
            }

            // 返回统计结果
            Map<String, Object> result = new HashMap<>();
            result.put("totalDms", totalDms);
            result.put("hasRefCount", hasRefCount);
            result.put("noRefCount", totalDms - hasRefCount);

            return Result.OK("计算完成", result);
        } catch (Exception e) {
            return Result.error("计算引用信息失败：" + e.getMessage());
        }
    }
}
