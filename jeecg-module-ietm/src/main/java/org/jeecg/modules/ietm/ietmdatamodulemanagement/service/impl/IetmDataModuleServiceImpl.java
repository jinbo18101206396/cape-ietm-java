package org.jeecg.modules.ietm.ietmdatamodulemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmComment;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmRef;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmType;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmCommentMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmRefMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmTypeMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.VersionCalculator;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmcUtils;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmCopyVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmEditPropVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmProjectInfoVO;
import org.jeecg.modules.ietm.ietmprojectcompany.entity.IetmProjectCompany;
import org.jeecg.modules.ietm.ietmprojectcompany.service.IIetmProjectCompanyService;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProject;
import org.jeecg.modules.ietm.projectmanagement.service.IIetmProjectService;
import org.jeecg.modules.ietm.common.service.ISnsCalculateService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class IetmDataModuleServiceImpl extends ServiceImpl<IetmDataModuleMapper, IetmDataModule> implements IIetmDataModuleService {

    // ==================== 常量定义 ====================

    private static final String INITIAL_ISSUE_NO       = "001";
    private static final String INITIAL_IN_WORK        = "00";
    private static final String WF_STATUS_ENDED        = "ended";
    private static final String STATUS_DELETED         = "0";
    private static final String STATUS_PUBLISHED       = "2";

    /** 工作流节点：DM编写 */
    private static final String WORKFLOW_STEP_DM_WRITE = "DM编写";

    /** DM状态：正常 */
    private static final String DM_STATUS_NORMAL = "1";

    /** DMC 输入白名单正则（防止 SQL 注入和路径遍历） */
    private static final Pattern DMC_SAFE_ALPHANUMERIC = Pattern.compile("^[A-Z0-9-]*$");
    private static final Pattern DMC_SAFE_SINGLE_CHAR  = Pattern.compile("^[A-Z0-9]?$");

    /** 版本号同步：正则表达式常量（性能优化） */
    private static final Pattern ISSUE_INFO_TAG_PATTERN = Pattern.compile(
        "<issueInfo[^>]*/>",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ISSUE_NUMBER_ATTR_PATTERN = Pattern.compile(
        "issueNumber\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern IN_WORK_ATTR_PATTERN = Pattern.compile(
        "inWork\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );


    @Autowired
    private IetmDataModuleMapper ietmDataModuleMapper;

    @Autowired
    private IetmDmCommentMapper ietmDmCommentMapper;

    @Autowired
    private IetmDmRefMapper ietmDmRefMapper;

    @Autowired
    private IIetmProjectConfigurationManagementService configurationService;

    @Autowired
    private IIetmProjectService projectService;

    @Autowired
    private ISnsCalculateService snsCalculateService;

    @Autowired
    private IIetmProjectCompanyService projectCompanyService;

    @Autowired
    private org.jeecg.modules.ietm.workflow.mapper.WfInstanceMapper wfInstanceMapper;

    @Autowired
    private IetmDmTypeMapper dmTypeMapper;

    /**
     * 获取项目信息（包含SNS编码）
     */
    @Override
    public DmProjectInfoVO getProjectInfo(String cmNodeId) {
        // 1. 获取构型节点
        IetmProjectConfigurationManagement config = configurationService.getById(cmNodeId);
        if (config == null) {
            throw new JeecgBootException("构型节点不存在");
        }

        // 2. 获取项目信息
        IetmProject project = projectService.getById(config.getProjectId());
        if (project == null) {
            throw new JeecgBootException("项目信息不存在");
        }

        // 3. 计算SNS编码（DM算法：coderule补全 + i=4/7位合并）
        String sns = snsCalculateService.calculateSnsForDm(cmNodeId);

        // 4. 组装返回对象
        DmProjectInfoVO vo = new DmProjectInfoVO();
        vo.setProjectId(project.getId());
        vo.setSecurity(String.valueOf(project.getSecurity()));
        vo.setSns(sns);
        vo.setCodeRule(project.getCodeRule());

        // 5. 语言和国家
        vo.setLanguageIsoCode(project.getLanguageCode());
        vo.setCountryIsoCode(project.getCountryCode());

        // 6. 构型节点技术名称
        vo.setTechName(config.getTitle());

        // 7. 项目的创作单位和责任单位（如果项目有配置的话）
        // 注：这里只设置编码，前端会从项目单位表加载名称列表
        // 如果需要默认值，前端会根据projectId过滤并自动选择

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDm(IetmDataModule dataModule) {
        log.info("保存数据模块，项目ID：{}", dataModule.getProjectId());

        // 校验 SNS 必填（SNS 空会导致 DMC 双横线脏数据）
        if (oConvertUtils.isEmpty(dataModule.getSns())) {
            throw new JeecgBootException("SNS 编码不能为空，请检查构型节点路径是否完整（至少2层）");
        }

        String dmc = generateDmc(dataModule);
        dataModule.setDmcCode(dmc);
        if (validateDmc(dataModule)) {
            throw new JeecgBootException("DMC编码已存在：" + dmc);
        }
        if (oConvertUtils.isEmpty(dataModule.getInWork())) {
            dataModule.setInWork("00");
        }
        if (oConvertUtils.isEmpty(dataModule.getIssueNo())) {
            dataModule.setIssueNo("001");
        }
        dataModule.setIsLatest("1");
        dataModule.setStatus("1");
        // 对齐旧系统：新建DM时设置版本日期
        if (dataModule.getIssueDate() == null) {
            dataModule.setIssueDate(new Date());
        }
        // 设置issueType默认值（S1000D标准）
        if (oConvertUtils.isEmpty(dataModule.getIssueType())) {
            dataModule.setIssueType("new");
        }
        boolean success = this.save(dataModule);

        // ✅ 保存后同步版本号到XML（导入等场景可能存在XML与数据库不一致）
        if (success && StringUtils.isNotEmpty(dataModule.getId()) && StringUtils.isNotEmpty(dataModule.getDmContent())) {
            syncVersionToXml(dataModule.getId());
        }

        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDm(IetmDataModule dataModule) {
        log.info("更新数据模块，ID：{}", dataModule.getId());
        IetmDataModule existDm = this.getById(dataModule.getId());
        if (existDm == null) {
            throw new JeecgBootException("数据模块不存在");
        }
        if (oConvertUtils.isNotEmpty(existDm.getCheckoutUser())) {
            throw new JeecgBootException("数据模块已被签出，无法修改");
        }

        // 检查版本号是否变更
        boolean versionChanged = !java.util.Objects.equals(existDm.getIssueNo(), dataModule.getIssueNo())
                              || !java.util.Objects.equals(existDm.getInWork(), dataModule.getInWork());

        // 若 DMC 组成字段有变更，重算 dmc_code 并检查唯一性
        // UI 编辑态已将这些字段全部禁用，正常路径下不会触发；此处为 API 层兜底防止旁路调用导致脏数据
        boolean dmcFieldChanged = isDmcFieldChanged(existDm, dataModule);
        if (dmcFieldChanged) {
            String newDmc = generateDmc(dataModule);
            dataModule.setDmcCode(newDmc);
            IetmDataModule conflict = ietmDataModuleMapper.selectByDmcForValidation(
                dataModule.getSns(), dataModule.getInfoCode(), dataModule.getInfoCodeVariant(),
                dataModule.getIetmLocationCode(), dataModule.getLanguageIsoCode(), dataModule.getCountryIsoCode(),
                dataModule.getId()
            );
            if (conflict != null) {
                throw new JeecgBootException("编辑失败：DMC冲突，" + newDmc + " 已存在（ID=" + conflict.getId() + "）");
            }
            log.info("updateDm: DMC字段变更，已重算 dmc_code={}", newDmc);

            // ✅ 同步 DMC 字段变更到 XML 内部的 dmIdent（dmCode/language/issueInfo）
            if (StringUtils.isNotBlank(dataModule.getDmContent())) {
                String syncedXml = DmXmlHelper.syncDmIdentToXml(dataModule.getDmContent(), dataModule);
                dataModule.setDmContent(syncedXml);
            }
        }

        boolean success = this.updateById(dataModule);

        // ✅ 如果版本号变更且有XML内容，同步版本号到XML
        if (success && versionChanged && StringUtils.isNotEmpty(dataModule.getDmContent())) {
            syncVersionToXml(dataModule.getId());
        }

        return success;
    }

    /** 判断 DMC 组成字段是否发生变更（任一字段不同即视为变更） */
    private boolean isDmcFieldChanged(IetmDataModule existing, IetmDataModule updated) {
        return !java.util.Objects.equals(existing.getSns(),              updated.getSns())
            || !java.util.Objects.equals(existing.getInfoCode(),         updated.getInfoCode())
            || !java.util.Objects.equals(existing.getInfoCodeVariant(),  updated.getInfoCodeVariant())
            || !java.util.Objects.equals(existing.getIetmLocationCode(), updated.getIetmLocationCode())
            || !java.util.Objects.equals(existing.getIssueNo(),          updated.getIssueNo())
            || !java.util.Objects.equals(existing.getInWork(),           updated.getInWork())
            || !java.util.Objects.equals(existing.getLanguageIsoCode(),  updated.getLanguageIsoCode())
            || !java.util.Objects.equals(existing.getCountryIsoCode(),   updated.getCountryIsoCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDm(String id) {
        log.info("删除数据模块，ID：{}", id);
        IetmDataModule existDm = this.getById(id);
        if (existDm == null) {
            throw new JeecgBootException("数据模块不存在");
        }
        return doDeleteDm(id, existDm);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDmWithEntity(String id, IetmDataModule existDm) {
        log.info("删除数据模块（预查询实体），ID：{}，DMC：{}", id,
                 existDm != null ? existDm.getDmcCode() : "null");
        if (existDm == null) {
            throw new JeecgBootException("数据模块不存在");
        }
        return doDeleteDm(id, existDm);
    }

    /**
     * 删除DM核心逻辑（内部方法，事务由调用方控制）
     * 注意：此方法必须在事务上下文中调用
     */
    private boolean doDeleteDm(String id, IetmDataModule existDm) {
        // 1. 签出状态检查
        if (oConvertUtils.isNotEmpty(existDm.getCheckoutUser())) {
            throw new JeecgBootException("数据模块已被【" + existDm.getCheckoutUser() + "】签出，请先签入或取消签出后再删除");
        }

        // 2. 创建者权限检查：只有创建者才能删除
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String currentUsername = loginUser.getUsername();
        if (!currentUsername.equals(existDm.getCreateBy())) {
            throw new JeecgBootException("该DM只能由创建者【" + existDm.getCreateBy() + "】删除");
        }

        // 3. 工作流状态检查：流程进行中（非草稿/编制阶段）不允许删除
        if (oConvertUtils.isNotEmpty(existDm.getWorkflowInstanceId())
                && oConvertUtils.isNotEmpty(existDm.getWorkflowStatus())
                && !WF_STATUS_ENDED.equals(existDm.getWorkflowStatus())) {
            throw new JeecgBootException("DM正在流程中（当前状态：" + existDm.getWorkflowStatus() + "），不允许删除");
        }

        // 4. 引用检查：查询是否被其他DM引用
        QueryWrapper<IetmDmRef> refWrapper = new QueryWrapper<>();
        refWrapper.eq("target_dm_id", id);
        int referencedCount = ietmDmRefMapper.selectCount(refWrapper).intValue();
        if (referencedCount > 0) {
            throw new JeecgBootException("此DM已被" + referencedCount + "个其他DM引用，请先解除引用关系后再删除");
        }

        // 5. 判断是物理删除还是逻辑删除
        String issueNo = existDm.getIssueNo() != null ? existDm.getIssueNo() : INITIAL_ISSUE_NO;
        String inwork = existDm.getInWork() != null ? existDm.getInWork() : INITIAL_IN_WORK;

        if (INITIAL_ISSUE_NO.equals(issueNo) && INITIAL_IN_WORK.equals(inwork)) {
            log.info("初始版本，执行物理删除，ID：{}，DMC：{}", id, existDm.getDmcCode());
            ietmDmCommentMapper.deleteByDmId(id);
            QueryWrapper<IetmDmRef> sourceRefWrapper = new QueryWrapper<>();
            sourceRefWrapper.eq("source_dm_id", id);
            ietmDmRefMapper.delete(sourceRefWrapper);
            log.info("物理删除完成，ID：{}", id);
            return this.removeById(id);
        } else {
            log.info("非初始版本，执行逻辑删除，ID：{}，版本：{}-{}", id, issueNo, inwork);

            // 级联删除资源记录（逻辑删除也应清理关联数据）
            ietmDmCommentMapper.deleteByDmId(id);

            // 删除出引用
            QueryWrapper<IetmDmRef> sourceRefWrapper = new QueryWrapper<>();
            sourceRefWrapper.eq("source_dm_id", id);
            ietmDmRefMapper.delete(sourceRefWrapper);

            // 软删除 DM
            existDm.setStatus(STATUS_DELETED);
            boolean success = this.updateById(existDm);
            if (!success) {
                throw new JeecgBootException("删除失败：数据已被其他用户修改，请刷新后重试");
            }

            log.info("逻辑删除完成，ID：{}", id);
            return true;
        }
    }

    @Override
    public IetmDataModule queryById(String id) {
        return this.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkOut(String id, String username) {
        log.info("签出数据模块，ID：{}，用户：{}", id, username);

        // ==================== 第1步：查询并校验原记录 ====================
        // ✅ 修复：使用baseMapper.selectById确保查询包含dm_content大字段
        // MyBatis-Plus的this.getById()可能使用轻量级查询，跳过CLOB字段
        IetmDataModule originalDm = baseMapper.selectById(id);
        if (originalDm == null) {
            throw new JeecgBootException("数据模块不存在");
        }

        // ✅ 调试日志：验证dm_content是否被正确查询
        int dmContentLength = (originalDm.getDmContent() != null) ? originalDm.getDmContent().length() : 0;
        log.debug("[签出-校验] 原版本ID：{}，dmContent长度：{} 字节", id, dmContentLength);
        if (dmContentLength == 0) {
            log.warn("[签出-警告] 原版本dmContent为空！签出后的历史版本将无法浏览。请检查数据完整性。");
        }

        // 校验1：是否已被签出
        if (oConvertUtils.isNotEmpty(originalDm.getCheckoutUser())) {
            throw new JeecgBootException("数据模块已被用户 " + originalDm.getCheckoutUser() + " 签出");
        }

        // 校验5：工作流是否已启动（方案A：workflow_instance_id 列不再回写，
        // 以 workflow_status 判定：null/空/'0'=未启动或已结束，'1'=流转中，'2'=已撤销）
        String wfStatus = originalDm.getWorkflowStatus();
        if (oConvertUtils.isEmpty(wfStatus) || "0".equals(wfStatus)) {
            throw new JeecgBootException("数据模块未启动工作流，不能签出");
        }
        if ("2".equals(wfStatus)) {
            throw new JeecgBootException("工作流已撤销，不能签出");
        }

        // 校验6：当前节点是否为DM编写（方案A：workflow_step 不回写基表，从 v_wf_instance 视图动态查询）
        IetmDataModule dmWithFlow = ietmDataModuleMapper.selectByIdWithFlow(id);
        String currentStep = dmWithFlow != null ? dmWithFlow.getWorkflowStep() : null;
        if (!"DM编写".equals(currentStep)) {
            throw new JeecgBootException("当前流程节点不是'DM编写'，不能签出（当前节点："
                + (currentStep != null ? currentStep : "无") + "）");
        }

        // 校验7：流程权限检查（对标旧系统attribute05权限验证）
        // 检查当前用户是否在流程节点的执行人列表中
        String workflowHandler = dmWithFlow != null ? dmWithFlow.getWorkflowHandler() : null;
        if (oConvertUtils.isNotEmpty(workflowHandler)) {
            // workflowHandler格式：userid1,userid2,userid3 或包含角色前缀如 rol_xxx,dpt_xxx
            // 也可能是用户真实姓名，如："管理员,张三,李四"
            boolean hasPermission = false;
            String[] handlers = workflowHandler.split(",");

            // 获取当前用户信息（用于匹配用户名称）
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            String realname = loginUser != null ? loginUser.getRealname() : null;

            log.debug("[签出-权限检查] 当前用户：username={}, realname={}, 执行人列表：{}",
                     username, realname, workflowHandler);

            for (String handler : handlers) {
                handler = handler.trim();

                // 基础验证1：直接匹配用户ID（username）
                if (username.equals(handler)) {
                    hasPermission = true;
                    log.debug("[签出-权限检查] 匹配成功：用户ID匹配");
                    break;
                }

                // 基础验证2：匹配用户真实姓名（realname）
                if (oConvertUtils.isNotEmpty(realname) && realname.equals(handler)) {
                    hasPermission = true;
                    log.debug("[签出-权限检查] 匹配成功：用户名称匹配");
                    break;
                }

                // TODO: 扩展验证角色/部门/岗位/工作组（如果handler包含前缀rol_/dpt_/pst_/grp_）
                // 当前为保守实现，只验证直接用户ID和真实姓名匹配，避免影响现有功能
                // 如需支持角色等复杂权限，需要：
                // 1. 获取当前用户的角色/部门/岗位/工作组信息
                // 2. 解析handler前缀并匹配
            }

            if (!hasPermission) {
                log.warn("[签出-权限检查] 权限验证失败：username={}, realname={}, handlers={}",
                         username, realname, workflowHandler);
                throw new JeecgBootException("您没有权限签出此DM，只能由流程指定的DM编写者才能签出（执行人："
                    + workflowHandler + "）");
            }
        }

        // 校验2：是否已发布
        if ("1".equals(originalDm.getVersionType())) {
            throw new JeecgBootException("已发布的数据模块不能签出");
        }

        // 校验3：是否最新版本
        if (!"1".equals(originalDm.getIsLatest())) {
            throw new JeecgBootException("只能签出最新版本，历史版本不可签出");
        }

        // 校验4：inwork版本号边界检查
        String currentInwork = originalDm.getInWork() != null ? originalDm.getInWork() : "00";
        String currentIssueno = originalDm.getIssueNo() != null ? originalDm.getIssueNo() : "001";
        int inwork = Integer.parseInt(currentInwork);
        if (inwork >= 99) {
            throw new JeecgBootException("在编版本已达上限99，请先发布后再签出");
        }

        // ==================== 第2步：克隆生成新版本 ====================
        IetmDataModule newDm = new IetmDataModule();

        // 2.1 复制所有字段（使用Spring的BeanUtils）
        org.springframework.beans.BeanUtils.copyProperties(originalDm, newDm);

        // ✅ 调试日志：验证dm_content是否被正确复制
        int newDmContentLength = (newDm.getDmContent() != null) ? newDm.getDmContent().length() : 0;
        log.debug("[签出-复制] 新版本dmContent长度：{} 字节（复制后）", newDmContentLength);
        if (newDmContentLength == 0 && dmContentLength > 0) {
            log.error("[签出-错误] BeanUtils.copyProperties未能复制dmContent！原：{} → 新：{}",
                dmContentLength, newDmContentLength);
            throw new JeecgBootException("签出失败：XML内容复制失败，请联系系统管理员");
        }

        // 2.2 清空主键和版本字段（让MyBatis-Plus自动生成新ID）
        newDm.setId(null);
        newDm.setVersion(null);

        // 2.3 升级版本号（inwork +1）
        Map<String, String> newVersion = calculateVersion(currentInwork, currentIssueno, "inwork");
        newDm.setInWork(newVersion.get("newInwork"));

        // 2.4 设置签出信息
        newDm.setCheckoutUser(username);
        newDm.setCheckoutTime(new Date());
        newDm.setCheckoutDmId(originalDm.getId()); // 关键：记录原版本ID

        // 2.5 标记为最新版本
        newDm.setIsLatest("1");

        // 2.6 重新生成DMC（因为inwork变化了）
        String newDmc = generateDmc(newDm);
        newDm.setDmcCode(newDmc);

        // ✅ 同步版本号变更到 XML 内部的 issueInfo
        // 签出时 inWork+1，需要同步到 XML
        if (StringUtils.isNotBlank(newDm.getDmContent())) {
            String syncedXml = DmXmlHelper.syncDmIdentToXml(newDm.getDmContent(), newDm);
            newDm.setDmContent(syncedXml);
            log.info("签出时已同步版本号到XML: 新版本inWork={}", newDm.getInWork());
        }

        // 2.7 更新时间戳
        newDm.setCreateTime(new Date());
        newDm.setUpdateTime(new Date());
        newDm.setCreateBy(username);
        newDm.setUpdateBy(username);

        // 2.8 清空签入/发布时间（新版本未签入/发布）
        newDm.setCheckinTime(null);
        newDm.setPublishDate(null);
        // 对齐旧系统：签出时继承原版本的issueDate，不清空
        // 只有在编辑属性时才更新为当前日期

        // ==================== 第3步：校验DMC唯一性 ====================
        IetmDataModule conflict = ietmDataModuleMapper.selectByDmcForValidation(
            newDm.getSns(), newDm.getInfoCode(), newDm.getInfoCodeVariant(),
            newDm.getIetmLocationCode(), newDm.getLanguageIsoCode(), newDm.getCountryIsoCode(),
            originalDm.getId() // 排除原版本（签出时原版本尚未更新为 is_latest=0）
        );
        if (conflict != null) {
            throw new JeecgBootException("签出失败：版本升级后 DMC 冲突，" + newDmc + " 已存在（ID=" + conflict.getId() + "）");
        }

        // ==================== 第4步：将当前版本标为历史版本（UPDATE）====================
        // 并发防护(CAS)：加 is_latest='1' 条件，把"读到未签出→降级"变成原子比较更新。
        // 两个用户并发签出同一DM时，只有一个能把 is_latest 从'1'改为'0'（影响1行），
        // 另一个匹配0行→updateSuccess=false→抛异常回滚，避免产生两个 is_latest='1' 版本。
        // 正常流程签出前 is_latest 必为'1'（第286行校验保证），故此条件对正常路径无影响。
        boolean updateSuccess = this.update(new LambdaUpdateWrapper<IetmDataModule>()
                .eq(IetmDataModule::getId, originalDm.getId())
                .eq(IetmDataModule::getIsLatest, "1")
                .set(IetmDataModule::getIsLatest, "0")
                .set(IetmDataModule::getUpdateTime, new Date())
                .set(IetmDataModule::getUpdateBy, username)
        );
        if (!updateSuccess) {
            throw new JeecgBootException("签出失败：该数据模块可能已被他人签出，请刷新后重试");
        }

        // ==================== 第5步：再保存新版本（INSERT）====================
        boolean insertSuccess = this.save(newDm);
        if (!insertSuccess) {
            throw new JeecgBootException("签出失败：创建新版本失败");
        }

        // ==================== 第6步：迁移工作流实例的formid关联 ====================
        // 将活动的工作流实例从旧版本关联到新版本，使列表页能继续显示"流程当前步骤"
        // 这是旧系统的行为：签出后新版本仍显示"DM编写"等流程信息
        // 注意：不依赖 workflow_instance_id 字段（该字段在当前系统中未使用），
        // 而是直接通过 wf_instance 表的 formid_ 字段来判断和迁移
        int updated = wfInstanceMapper.migrateFormid(
            originalDm.getId(),      // 旧版本id
            newDm.getId(),           // 新版本id
            username
        );
        if (updated > 0) {
            log.info("迁移工作流实例 formid: {} -> {}, 影响行数: {}",
                     originalDm.getId(), newDm.getId(), updated);
        } else {
            log.debug("无需迁移工作流实例：DM {} 没有关联的活动流程", originalDm.getId());
        }

        // ✅ 第6.5步：复制资源关联到新工作版本
        // 签出克隆出新ID，资源(ietm_dm_resource)仍挂在原版本上，需复制到新版本，
        // 否则列表刷新后选中新版本时，资源列表为空。
        int copiedRes = copyDmResources(originalDm.getId(), newDm.getId(), username);
        if (copiedRes > 0) {
            log.info("签出复制资源：{} -> {}, 复制 {} 条", originalDm.getId(), newDm.getId(), copiedRes);
        }

        // ✅ 第7步：同步版本号到XML（签出后新版本的XML中issueInfo标签需要更新）
        syncVersionToXml(newDm.getId());

        log.info("签出成功！原版本ID：{}，新版本ID：{}，版本号：{}-{} -> {}-{}",
                 originalDm.getId(), newDm.getId(),
                 currentIssueno, currentInwork,
                 newDm.getIssueNo(), newDm.getInWork());

        return true;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelCheckOut(String id, String username) {
        log.info("取消签出数据模块，ID：{}，用户：{}", id, username);

        // ==================== 第1步：查询并校验工作版本 ====================
        // ✅ 修复：使用baseMapper.selectById确保查询完整字段（虽然取消签出不需要dm_content，但保持一致性）
        IetmDataModule workDm = baseMapper.selectById(id);
        if (workDm == null) {
            throw new JeecgBootException("数据模块不存在");
        }

        // 校验1：是否已签出
        if (oConvertUtils.isEmpty(workDm.getCheckoutUser())) {
            throw new JeecgBootException("数据模块未被签出");
        }

        // 校验2：是否本人签出
        if (!username.equals(workDm.getCheckoutUser())) {
            throw new JeecgBootException("只能取消自己签出的数据模块");
        }

        // 校验3：是否有原版本ID（关键！）
        if (oConvertUtils.isEmpty(workDm.getCheckoutDmId())) {
            throw new JeecgBootException("取消签出失败：未找到原版本ID（数据异常，该记录可能不是通过签出生成的）");
        }

        // ==================== 第2步：查询原版本 ====================
        String originalDmId = workDm.getCheckoutDmId();
        // ✅ 修复：使用baseMapper.selectById确保查询完整字段
        IetmDataModule originalDm = baseMapper.selectById(originalDmId);
        if (originalDm == null) {
            throw new JeecgBootException("取消签出失败：原版本记录不存在（ID=" + originalDmId + "）");
        }

        // 校验原版本状态
        if (!"0".equals(originalDm.getIsLatest())) {
            log.warn("原版本的 isLatest 应该为 '0'，当前值为 '{}'，数据可能异常", originalDm.getIsLatest());
        }

        // ==================== 第3步：反向迁移工作流实例的formid关联 ====================
        // 取消签出时，将工作流实例从当前工作版本迁移回原版本
        // 必须在删除工作版本之前执行，否则找不到关联记录
        // 注意：不依赖 workflow_instance_id 字段（该字段在当前系统中未使用），
        // 而是直接通过 wf_instance 表的 formid_ 字段来判断和迁移
        int updated = wfInstanceMapper.migrateFormid(
            workDm.getId(),          // 当前工作版本id（即将删除）
            originalDm.getId(),      // 原版本id（将恢复为最新）
            username
        );
        if (updated > 0) {
            log.info("取消签出：反向迁移工作流实例 formid: {} -> {}, 影响行数: {}",
                     workDm.getId(), originalDm.getId(), updated);
        } else {
            log.debug("无需反向迁移工作流实例：工作版本 {} 没有关联的活动流程", workDm.getId());
        }

        // ==================== 第4步：删除工作版本（当前签出的记录）====================
        // 先清理工作版本在签出时复制的资源副本，避免删除DM后残留孤儿资源记录
        ietmDmCommentMapper.deleteByDmId(id);

        boolean deleteSuccess = this.removeById(id);
        if (!deleteSuccess) {
            throw new JeecgBootException("取消签出失败：删除工作版本失败");
        }

        // ==================== 第5步：恢复原版本为最新版本 ====================
        // 只更新 is_latest 字段，避免触发其他唯一约束
        boolean updateSuccess = this.update(new LambdaUpdateWrapper<IetmDataModule>()
                .eq(IetmDataModule::getId, originalDm.getId())
                .set(IetmDataModule::getIsLatest, "1")
                .set(IetmDataModule::getUpdateTime, new Date())
                .set(IetmDataModule::getUpdateBy, username)
        );
        if (!updateSuccess) {
            throw new JeecgBootException("取消签出失败：恢复原版本失败（乐观锁冲突）");
        }

        log.info("取消签出成功！已删除工作版本ID：{}，已恢复原版本ID：{}，版本号：{}-{}",
                 id, originalDm.getId(),
                 originalDm.getIssueNo(), originalDm.getInWork());

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkIn(String id, String username, String comment) {
        log.info("签入数据模块，ID：{}，用户：{}，备注：{}", id, username, comment);
        IetmDataModule dm = this.getById(id);
        if (dm == null) {
            throw new JeecgBootException("数据模块不存在");
        }
        if (oConvertUtils.isEmpty(dm.getCheckoutUser())) {
            throw new JeecgBootException("数据模块未被签出");
        }
        if (!username.equals(dm.getCheckoutUser())) {
            throw new JeecgBootException("只能签入自己签出的数据模块");
        }

        // 保存签入备注
        if (oConvertUtils.isNotEmpty(comment)) {
            dm.setReason(comment);
        }

        // 清除签出状态
        dm.setCheckoutUser(null);
        dm.setCheckoutTime(null);
        dm.setCheckinTime(new Date());

        // ✅ 签入时保留原版本作为历史版本 (is_latest='0', status='1')
        // 说明：签入后，原版本保留为 status='1' 以便在"历史版本"列表中可见
        if (oConvertUtils.isNotEmpty(dm.getCheckoutDmId())) {
            String originalDmId = dm.getCheckoutDmId();
            log.info("签入成功，原版本ID：{} 保留为历史版本 (is_latest='0', status='1')", originalDmId);
        }

        // 使用 LambdaUpdateWrapper 只更新需要的字段，避免触发唯一约束
        LambdaUpdateWrapper<IetmDataModule> updateWrapper = new LambdaUpdateWrapper<IetmDataModule>()
                .eq(IetmDataModule::getId, dm.getId())
                .set(IetmDataModule::getCheckoutUser, null)
                .set(IetmDataModule::getCheckoutTime, null)
                .set(IetmDataModule::getCheckinTime, dm.getCheckinTime())
                // 保留 checkout_dm_id 字段，用于追溯版本历史（对标旧系统行为）
                .set(IetmDataModule::getUpdateBy, username)
                .set(IetmDataModule::getUpdateTime, new Date());

        // 如果有备注，也更新 reason 字段
        if (oConvertUtils.isNotEmpty(comment)) {
            updateWrapper.set(IetmDataModule::getReason, dm.getReason());
        }

        boolean success = this.update(updateWrapper);
        if (!success) {
            throw new JeecgBootException("签入失败：数据已被其他用户修改，请刷新后重试");
        }

        log.info("签入成功！当前版本ID：{}，版本号：{}-{}",
                 dm.getId(), dm.getIssueNo(), dm.getInWork());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishDm(String id, String username) {
        log.info("发布数据模块，ID：{}，用户：{}", id, username);
        IetmDataModule dm = this.getById(id);
        if (dm == null) {
            throw new JeecgBootException("数据模块不存在");
        }

        // 1. 签出状态校验
        if (oConvertUtils.isNotEmpty(dm.getCheckoutUser())) {
            throw new JeecgBootException("请先签入后再发布");
        }

        // 2. 工作流状态校验（如果有工作流）
        if (oConvertUtils.isNotEmpty(dm.getWorkflowInstanceId())) {
            if (!WF_STATUS_ENDED.equals(dm.getWorkflowStatus())) {
                throw new JeecgBootException("工作流未结束，不可发布");
            }
        }

        // 3. 版本号上限校验
        String currentIssueno = dm.getIssueNo() != null ? dm.getIssueNo() : "001";
        int issueNo = Integer.parseInt(currentIssueno);
        if (issueNo >= 999) {
            throw new JeecgBootException("发行编号已达上限999，无法继续发布");
        }

        // 4. 升级版本号
        Map<String, String> newVersion = calculateVersion(dm.getInWork(), currentIssueno, "issue");
        dm.setInWork("00");
        dm.setIssueNo(newVersion.get("newIssueno"));

        // 5. 设置发布信息
        dm.setPublishDate(new Date());
        dm.setVersionType("1"); // 已发布
        dm.setStatus("2"); // 已发布状态

        // 6. 更新签发日期
        dm.setIssueDate(new Date());

        // 7. 重新生成 DMC（因为 issueNo 变化了）
        String newDmc = generateDmc(dm);
        dm.setDmcCode(newDmc);

        // 8. 校验 DMC 唯一性
        IetmDataModule conflict = ietmDataModuleMapper.selectByDmcForValidation(
            dm.getSns(), dm.getInfoCode(), dm.getInfoCodeVariant(),
            dm.getIetmLocationCode(), dm.getLanguageIsoCode(), dm.getCountryIsoCode(),
            dm.getId()
        );
        if (conflict != null) {
            throw new JeecgBootException("发布失败：版本升级后 DMC 冲突，" + newDmc + " 已存在（ID=" + conflict.getId() + "）");
        }

        // 9. 计算 issueType（S1000D 标准）
        String issueType = "001".equals(dm.getIssueNo()) ? "new" : "revised";

        // 10. 使用 LambdaUpdateWrapper 只更新需要的字段，避免触发唯一约束
        boolean success = this.update(new LambdaUpdateWrapper<IetmDataModule>()
                .eq(IetmDataModule::getId, dm.getId())
                .set(IetmDataModule::getInWork, "00")
                .set(IetmDataModule::getIssueNo, dm.getIssueNo())
                .set(IetmDataModule::getDmcCode, dm.getDmcCode())
                .set(IetmDataModule::getPublishDate, dm.getPublishDate())
                .set(IetmDataModule::getVersionType, "1")
                .set(IetmDataModule::getStatus, "2")
                .set(IetmDataModule::getIssueDate, dm.getIssueDate())
                .set(IetmDataModule::getIssueType, issueType)
                .set(IetmDataModule::getWorkflowStatus, "0")  // 流程结束（方案A：只回写 workflowStatus）
                // 注意：不设置 workflowStep 和 workflowHandler，视图会自动返回空
                .set(IetmDataModule::getUpdateBy, username)
                .set(IetmDataModule::getUpdateTime, new Date())
        );
        if (!success) {
            throw new JeecgBootException("发布失败：数据已被其他用户修改，请刷新后重试");
        }

        // ✅ 同步版本号变更到 XML 内部的 issueInfo
        // 注意：发布时版本号已变化（issueNo+1, inWork=00），需要同步到 XML
        if (StringUtils.isNotBlank(dm.getDmContent())) {
            // 重新加载完整实体（包含 dm_content 大字段）
            IetmDataModule fullDm = this.getById(dm.getId());
            if (fullDm != null && StringUtils.isNotBlank(fullDm.getDmContent())) {
                String syncedXml = DmXmlHelper.syncDmIdentToXml(fullDm.getDmContent(), dm);
                // 单独更新 dm_content 字段
                this.update(new LambdaUpdateWrapper<IetmDataModule>()
                    .eq(IetmDataModule::getId, dm.getId())
                    .set(IetmDataModule::getDmContent, syncedXml)
                );
                log.info("发布成功，已同步版本号到XML: ID={}, DMC={}", dm.getId(), dm.getDmcCode());
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchCheckOut(List<String> ids, String username) {
        log.info("批量签出数据模块，数量：{}，用户：{}", ids.size(), username);
        int success = 0;
        int fail = 0;
        List<String> failMessages = new ArrayList<>();

        for (String id : ids) {
            try {
                checkOut(id, username);
                success++;
            } catch (Exception e) {
                // 立即抛出，回滚整个事务（all-or-nothing）
                throw new JeecgBootException("批量签出失败（已回滚）：第 " + (success + 1) + " 条记录失败 - " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("failMessages", failMessages);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchCheckIn(List<String> ids, String username, String comment) {
        log.info("批量签入数据模块，数量：{}，用户：{}", ids.size(), username);
        int success = 0;

        for (String id : ids) {
            try {
                checkIn(id, username, comment);
                success++;
            } catch (Exception e) {
                // 立即抛出，回滚整个事务（all-or-nothing策略）
                throw new JeecgBootException("批量签入失败（已回滚）：第 " + (success + 1) + " 条记录失败 - " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("total", ids.size());
        return result;
    }

    @Override


    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchDelete(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            log.warn("batchDelete 收到空 ids 列表，直接返回");
            Map<String, Object> empty = new HashMap<>();
            empty.put("successCount", 0);
            empty.put("failCount", 0);
            empty.put("failMessages", new java.util.ArrayList<>());
            return empty;
        }
        // 批量预查询，使用 LambdaQueryWrapper 确保查询所有字段（包括 version 乐观锁字段）
        LambdaQueryWrapper<IetmDataModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(IetmDataModule::getId, ids);
        List<IetmDataModule> dms = this.list(queryWrapper);
        Map<String, IetmDataModule> dmMap = new HashMap<>();
        for (IetmDataModule dm : dms) {
            dmMap.put(dm.getId(), dm);
        }

        int success = 0;
        int fail = 0;
        List<String> failMessages = new ArrayList<>();
        for (String id : ids) {
            IetmDataModule dm = dmMap.get(id);
            String label = (dm != null && oConvertUtils.isNotEmpty(dm.getDmcCode()))
                    ? dm.getDmcCode() : id;
            try {
                if (dm == null) {
                    // 不存在的记录记录为失败，继续处理后续记录
                    fail++;
                    failMessages.add(label + ": 数据模块不存在");
                    continue;
                }
                // 调用公共方法，确保事务生效
                this.deleteDmWithEntity(id, dm);
                success++;
            } catch (Exception e) {
                fail++;
                failMessages.add(label + ": " + e.getMessage());
                log.error("批量删除失败，ID={}，错误：{}", id, e.getMessage(), e);
                // 关键修复：任何一条失败，立即抛出异常回滚整个事务
                throw new JeecgBootException("批量删除失败（已回滚）：第 " + (success + fail) + " 条记录失败 - " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", success);
        result.put("failCount", fail);
        result.put("failMessages", failMessages);

        // 记录批量删除结果
        log.info("批量删除完成，总数：{}，成功：{}，失败：{}", ids.size(), success, fail);
        if (fail > 0) {
            log.warn("批量删除部分失败，失败数量：{}，失败详情：{}", fail, failMessages);
        }

        return result;
    }

    @Override
    public List<IetmDataModule> queryByProjectId(String projectId) {
        return ietmDataModuleMapper.selectByProjectId(projectId);
    }

    @Override
    public List<IetmDataModule> queryByCmNodeId(String cmNodeId, boolean includeChildren) {
        return ietmDataModuleMapper.selectByCmNodeId(cmNodeId, includeChildren);
    }

    @Override
    public void updateReferenceCount(String dmId) {
        Integer refCount    = ietmDmRefMapper.countOutReferences(dmId);
        Integer refedCount  = ietmDmRefMapper.countInReferences(dmId);

        this.update(new LambdaUpdateWrapper<IetmDataModule>()
                .eq(IetmDataModule::getId, dmId)
                .set(IetmDataModule::getRefCount,   refCount   != null ? refCount   : 0)
                .set(IetmDataModule::getRefedCount, refedCount != null ? refedCount : 0));

        log.info("updateReferenceCount dmId={} refCount={} refedCount={}", dmId, refCount, refedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> calculateDmReferences(String dmId) throws Exception {
        System.out.println("【强制输出】calculateDmReferences 被调用！dmId=" + dmId);
        log.warn("【WARN级别】calculateDmReferences START dmId={}", dmId);
        log.info("calculateDmReferences START dmId={}", dmId);

        // ① 查询DM（含 dm_content）
        IetmDataModule dm = ietmDataModuleMapper.selectContentById(dmId);
        if (dm == null) {
            throw new JeecgBootException("DM不存在，ID：" + dmId);
        }
        String dmContent = dm.getDmContent();
        if (oConvertUtils.isEmpty(dmContent)) {
            throw new JeecgBootException("DM内容为空，无法计算引用关系，DMC：" + dm.getDmcCode());
        }

        // ② 解析XML，提取引用条目
        List<org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefExtractItemVO> extracted;
        try {
            extracted = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper
                    .extractReferencesFromXml(dmContent);
            log.info("calculateDmReferences 提取引用 dmId={} 提取数量={}", dmId, extracted.size());
        } catch (Exception e) {
            log.error("calculateDmReferences XML解析失败 dmId={} error={}", dmId, e.getMessage(), e);
            throw new JeecgBootException("XML解析失败：" + e.getMessage());
        }

        // 如果没有提取到任何引用，直接返回
        if (extracted.isEmpty()) {
            log.info("calculateDmReferences dmId={} 无引用内容", dmId);
            Map<String, Object> result = new HashMap<>();
            result.put("dmId", dmId);
            result.put("dmcCode", dm.getDmcCode());
            result.put("techName", dm.getTechName());
            result.put("refCount", 0);
            result.put("details", new ArrayList<>());
            return result;
        }

        // ③ 查询已有出引用（用于幂等合并：只删失效的、只插新增的）
        List<IetmDmRef> existingRefs = ietmDmRefMapper.selectList(
                new LambdaQueryWrapper<IetmDmRef>().eq(IetmDmRef::getSourceDmId, dmId));
        // key = targetDmId + ":" + refType + ":" + refPosition
        Map<String, IetmDmRef> existingMap = new HashMap<>();
        for (IetmDmRef r : existingRefs) {
            existingMap.put(r.getTargetDmId() + ":" + r.getRefType() + ":" + r.getRefPosition(), r);
        }

        String currentUser = getCurrentUsername();
        Date   now         = new Date();
        String srcDmc      = dm.getDmcCode();

        List<IetmDmRef> toInsert = new ArrayList<>();
        List<Map<String, Object>> details = new ArrayList<>();

        // 只处理 dmRef，graphic/multimedia 暂不建立 DM 间关系（目标不是 DM 主表）
        for (org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmRefExtractItemVO item : extracted) {
            if (!"dmRef".equals(item.getRefType())) {
                log.debug("calculateDmReferences 跳过非DM引用 dmId={} refType={} target={}",
                         dmId, item.getRefType(), item.getTargetDmc());
                Map<String, Object> d = new HashMap<>();
                d.put("refType", item.getRefType()); d.put("targetDmc", item.getTargetDmc());
                d.put("targetExists", false); d.put("note", "非DM引用，不建立关联");
                details.add(d);
                continue;
            }
            // 根据DMC查目标DM（最新有效版本）
            // 先尝试精确匹配
            IetmDataModule target = ietmDataModuleMapper.selectOne(
                    new LambdaQueryWrapper<IetmDataModule>()
                            .eq(IetmDataModule::getDmcCode, item.getTargetDmc())
                            .eq(IetmDataModule::getStatus,  "1")
                            .eq(IetmDataModule::getIsLatest,"1")
                            .last("FETCH FIRST 1 ROWS ONLY"));

            // 如果精确匹配失败，尝试前缀匹配（兼容无后缀的DMC引用）
            if (target == null) {
                log.debug("calculateDmReferences 精确匹配失败，尝试前缀匹配 targetDmc={}", item.getTargetDmc());
                target = ietmDataModuleMapper.selectOne(
                        new LambdaQueryWrapper<IetmDataModule>()
                                .likeRight(IetmDataModule::getDmcCode, item.getTargetDmc())
                                .eq(IetmDataModule::getStatus,  "1")
                                .eq(IetmDataModule::getIsLatest,"1")
                                .last("FETCH FIRST 1 ROWS ONLY"));
            }

            if (target == null) {
                log.warn("calculateDmReferences 目标DM不存在（精确和前缀匹配均失败） dmId={} targetDmc={}", dmId, item.getTargetDmc());
                Map<String, Object> d = new HashMap<>();
                d.put("refType", item.getRefType()); d.put("targetDmc", item.getTargetDmc());
                d.put("targetExists", false); d.put("note", "目标DM不存在");
                details.add(d);
                continue;
            } else {
                log.info("calculateDmReferences 找到目标DM dmId={} targetDmc={} actualDmc={} targetId={}",
                         dmId, item.getTargetDmc(), target.getDmcCode(), target.getId());
            }

            String key = target.getId() + ":" + item.getRefType() + ":" + item.getRefPosition();
            if (!existingMap.containsKey(key)) {
                // 仅插入新增的引用
                IetmDmRef ref = new IetmDmRef();
                ref.setId(org.jeecg.common.util.UUIDGenerator.generate());  // 手动生成ID（INSERT ALL 需要）
                ref.setSourceDmId(dmId);
                ref.setTargetDmId(target.getId());
                ref.setRefType(item.getRefType());
                ref.setRefDmc(srcDmc);
                ref.setTargetDmc(item.getTargetDmc());
                ref.setRefPosition(item.getRefPosition());
                ref.setCreateBy(currentUser);
                ref.setCreateTime(now);
                toInsert.add(ref);
            }
            // 从 existingMap 中移除，剩余的是失效引用
            existingMap.remove(key);

            Map<String, Object> d = new HashMap<>();
            d.put("refType", item.getRefType()); d.put("targetDmc", item.getTargetDmc());
            d.put("targetDmId", target.getId()); d.put("targetExists", true);
            details.add(d);
        }

        // ④ 删除失效引用（本次XML中已不存在的 dmRef 条目）
        if (!existingMap.isEmpty()) {
            List<String> staleIds = new ArrayList<>();
            for (IetmDmRef stale : existingMap.values()) staleIds.add(stale.getId());
            ietmDmRefMapper.deleteBatchIds(staleIds);
            log.info("calculateDmReferences 删除失效引用 {} 条 dmId={}", staleIds.size(), dmId);
        }

        // ⑤ 批量插入新增引用（逐条插入，避免INSERT ALL被SQL解析器拒绝）
        if (!toInsert.isEmpty()) {
            log.info("calculateDmReferences 准备插入引用 dmId={} 数量={}", dmId, toInsert.size());
            // 打印第一条记录的详细信息用于调试
            if (!toInsert.isEmpty()) {
                IetmDmRef first = toInsert.get(0);
                log.info("calculateDmReferences 首条记录 id={} sourceDmId={} targetDmId={} refType={} createBy={}",
                        first.getId(), first.getSourceDmId(), first.getTargetDmId(),
                        first.getRefType(), first.getCreateBy());
            }
            // 逐条插入，避免MyBatis-Plus的TenantLineInnerInterceptor解析INSERT ALL失败
            int successCount = 0;
            for (IetmDmRef ref : toInsert) {
                try {
                    ietmDmRefMapper.insertOne(ref);
                    successCount++;
                } catch (Exception e) {
                    log.error("calculateDmReferences 单条插入失败 dmId={} refId={} error={}",
                            dmId, ref.getId(), e.getMessage());
                    // 继续插入其他记录
                }
            }
            log.info("calculateDmReferences 插入新引用完成 成功={}/{} dmId={}", successCount, toInsert.size(), dmId);
        } else {
            log.info("calculateDmReferences 无需插入新引用 dmId={}", dmId);
        }

        // ⑥ 刷新 ref_count / refed_count
        updateReferenceCount(dmId);
        // 被引用方的 refed_count 也需要刷新
        Set<String> affectedTargetIds = new HashSet<>();
        for (IetmDmRef r : toInsert) affectedTargetIds.add(r.getTargetDmId());
        for (IetmDmRef r : existingMap.values()) affectedTargetIds.add(r.getTargetDmId());
        for (String tid : affectedTargetIds) updateReferenceCount(tid);

        Integer finalRefCount = ietmDmRefMapper.countOutReferences(dmId);
        Map<String, Object> result = new HashMap<>();
        result.put("dmId", dmId);
        result.put("dmcCode", srcDmc);
        result.put("techName", dm.getTechName());
        result.put("refCount", finalRefCount);
        result.put("details", details);

        log.info("calculateDmReferences END dmId={} refCount={}", dmId, finalRefCount);
        return result;
    }

    @Override
    public Map<String, Object> calculateAllDmReferences(int batchSize) {
        System.out.println("======================================");
        System.out.println("【强制输出】calculateAllDmReferences 被调用！");
        System.out.println("【强制输出】batchSize = " + batchSize);
        System.out.println("【强制输出】时间 = " + new java.util.Date());
        System.out.println("======================================");
        log.warn("【WARN级别】calculateAllDmReferences START batchSize={}", batchSize);
        log.info("calculateAllDmReferences START batchSize={}", batchSize);
        int total = 0, success = 0, fail = 0, skip = 0;
        long start = System.currentTimeMillis();
        int page = 1;
        while (true) {
            // BUG修复：selectPage 不会加载 CLOB 字段，需要只查 ID，再单独加载内容
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<IetmDataModule> p =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, batchSize);
            com.baomidou.mybatisplus.core.metadata.IPage<IetmDataModule> pageResult =
                    ietmDataModuleMapper.selectPage(p, new LambdaQueryWrapper<IetmDataModule>()
                            .select(IetmDataModule::getId, IetmDataModule::getDmcCode)  // 只查 ID 和 DMC
                            .eq(IetmDataModule::getStatus,   "1")
                            .eq(IetmDataModule::getIsLatest, "1")
                            .isNotNull(IetmDataModule::getDmContent)
                            .orderByAsc(IetmDataModule::getCreateTime));
            if (pageResult.getRecords().isEmpty()) break;
            for (IetmDataModule dm : pageResult.getRecords()) {
                total++;
                try {
                    // 通过 selectContentById 加载完整内容（含 dm_content）
                    calculateDmReferences(dm.getId());
                    success++;
                } catch (Exception e) {
                    fail++;
                    log.error("calculateAllDmReferences FAIL dmId={} dmc={} error={}",
                            dm.getId(), dm.getDmcCode(), e.getMessage());
                }
                if (total % 100 == 0) {
                    try { Thread.sleep(50); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); break;
                    }
                }
            }
            page++;
        }
        long duration = System.currentTimeMillis() - start;
        log.info("calculateAllDmReferences END total={} success={} fail={} skip={} duration={}ms",
                total, success, fail, skip, duration);
        Map<String, Object> result = new HashMap<>();
        result.put("totalCount",   total);
        result.put("successCount", success);
        result.put("failCount",    fail);
        result.put("skipCount",    skip);
        result.put("duration",     duration);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importXml(MultipartFile file, String projectId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 验证文件
            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("message", "上传文件为空");
                return result;
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".xml")) {
                result.put("success", false);
                result.put("message", "只支持XML格式文件");
                return result;
            }

            // 2. 解析XML文件
            SAXReader reader = createSafeSaxReader();
            reader.setEncoding("UTF-8");
            Document document = reader.read(file.getInputStream());
            Element root = document.getRootElement();

            // 3. 提取DMC信息（根据S1000D标准）
            IetmDataModule dataModule = new IetmDataModule();
            dataModule.setProjectId(projectId);

            // 查找<dmIdent>标签（S1000D标准DM标识）
            Element dmIdent = root.element("identAndStatusSection");
            if (dmIdent != null) {
                Element dmAddress = dmIdent.element("dmAddress");
                if (dmAddress != null) {
                    Element dmIdent2 = dmAddress.element("dmIdent");
                    if (dmIdent2 != null) {
                        // 提取DMC组成部分
                        Element dmCode = dmIdent2.element("dmCode");
                        if (dmCode != null) {
                            dataModule.setSchema(getAttributeValue(dmCode, "modelIdentCode", "J"));
                            // 【方案A】SNS 含 equipname 作首段，从 dmCode 全 8 属性重建（对标老系统）
                            dataModule.setSns(DmcUtils.composeSns(
                                    getAttributeValue(dmCode, "modelIdentCode"),
                                    getAttributeValue(dmCode, "systemDiffCode"),
                                    getAttributeValue(dmCode, "systemCode"),
                                    getAttributeValue(dmCode, "subSystemCode"),
                                    getAttributeValue(dmCode, "subSubSystemCode"),
                                    getAttributeValue(dmCode, "assyCode"),
                                    getAttributeValue(dmCode, "disassyCode"),
                                    getAttributeValue(dmCode, "disassyCodeVariant")));
                            dataModule.setInfoCode(getAttributeValue(dmCode, "infoCode"));
                            dataModule.setInfoCodeVariant(getAttributeValue(dmCode, "infoCodeVariant"));
                            dataModule.setIetmLocationCode(getAttributeValue(dmCode, "itemLocationCode"));
                            dataModule.setLearnCode(getAttributeValue(dmCode, "learnCode"));
                            dataModule.setLearnEventCode(getAttributeValue(dmCode, "learnEventCode"));
                        }

                        // 提取发行信息
                        Element issueInfo = dmIdent2.element("issueInfo");
                        if (issueInfo != null) {
                            dataModule.setIssueNo(getAttributeValue(issueInfo, "issueNumber", "001"));
                            dataModule.setInWork(getAttributeValue(issueInfo, "inWork", "00"));
                        }

                        // 提取语言信息
                        Element language = dmIdent2.element("language");
                        if (language != null) {
                            dataModule.setLanguageIsoCode(getAttributeValue(language, "languageIsoCode"));
                            dataModule.setCountryIsoCode(getAttributeValue(language, "countryIsoCode"));
                        }
                    }
                }

                // 提取技术名称和信息名称
                Element dmAddressItems = dmIdent.element("dmAddressItems");
                if (dmAddressItems != null) {
                    Element dmTitle = dmAddressItems.element("dmTitle");
                    if (dmTitle != null) {
                        Element techName = dmTitle.element("techName");
                        Element infoName = dmTitle.element("infoName");
                        if (techName != null) {
                            dataModule.setTechName(techName.getTextTrim());
                        }
                        if (infoName != null) {
                            dataModule.setInfoName(infoName.getTextTrim());
                        }
                    }
                }

                // 提取发行方信息
                Element dmStatus = dmIdent.element("dmStatus");
                if (dmStatus != null) {
                    Element responsiblePartnerCompany = dmStatus.element("responsiblePartnerCompany");
                    if (responsiblePartnerCompany != null) {
                        Element enterpriseName = responsiblePartnerCompany.element("enterpriseName");
                        if (enterpriseName != null) {
                            dataModule.setOriginatorName(enterpriseName.getTextTrim());
                        }
                    }

                    Element originator = dmStatus.element("originator");
                    if (originator != null) {
                        Element enterpriseName = originator.element("enterpriseName");
                        if (enterpriseName != null) {
                            dataModule.setRpcName(enterpriseName.getTextTrim());
                        }
                    }

                    // 提取密级
                    Element security = dmStatus.element("security");
                    if (security != null) {
                        dataModule.setSecurity(getAttributeValue(security, "securityClassification"));
                    }
                }
            }

            // 4. 保存完整XML内容
            String xmlContent = document.asXML();
            dataModule.setDmContent(xmlContent);

            // 5. 设置默认值
            dataModule.setVersionType("0");  // 导入默认为草稿
            dataModule.setStatus("1");       // 正常状态
            dataModule.setIsLatest("1");     // 最新版本

            // 6. 验证必填字段
            if (oConvertUtils.isEmpty(dataModule.getSns())) {
                result.put("success", false);
                result.put("message", "XML文件缺少SNS信息");
                return result;
            }
            if (oConvertUtils.isEmpty(dataModule.getInfoCode())) {
                result.put("success", false);
                result.put("message", "XML文件缺少信息码");
                return result;
            }

            // 7. 生成并校验 DMC 编码（importXml 之前绕过了 saveDm，需在此补全）
            String dmc = generateDmc(dataModule);
            dataModule.setDmcCode(dmc);
            if (validateDmc(dataModule)) {
                result.put("success", false);
                result.put("message", "DMC编码已存在：" + dmc);
                return result;
            }
            if (oConvertUtils.isEmpty(dataModule.getInWork())) {
                dataModule.setInWork(INITIAL_IN_WORK);
            }
            if (oConvertUtils.isEmpty(dataModule.getIssueNo())) {
                dataModule.setIssueNo(INITIAL_ISSUE_NO);
            }

            // 8. 保存到数据库
            boolean saveSuccess = this.save(dataModule);

            if (saveSuccess) {
                result.put("success", true);
                result.put("message", "XML导入成功");
                result.put("dmId", dataModule.getId());
                result.put("dmcCode", dataModule.getDmcCode());
                log.info("XML导入成功，DM ID：{}", dataModule.getId());
            } else {
                result.put("success", false);
                result.put("message", "数据库保存失败");
            }

        } catch (DocumentException e) {
            log.error("XML文件解析失败", e);
            result.put("success", false);
            result.put("message", "XML格式错误：" + e.getMessage());
        } catch (Exception e) {
            log.error("XML导入异常", e);
            result.put("success", false);
            result.put("message", "导入失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * 获取XML元素属性值
     */
    private String getAttributeValue(Element element, String attrName) {
        return getAttributeValue(element, attrName, null);
    }

    /**
     * 获取XML元素属性值（带默认值）
     */
    private String getAttributeValue(Element element, String attrName, String defaultValue) {
        if (element == null) {
            return defaultValue;
        }
        String value = element.attributeValue(attrName);
        return oConvertUtils.isEmpty(value) ? defaultValue : value;
    }

    /**
     * 生成DMC码
     */
    /**
     * 生成DMC编码的简化版本（仅前几段，用于特定场景）
     * 如果需要完整DMC，请使用generateDmc()方法
     */
    public String generateDmcCode(IetmDataModule dm) {
        // 注意：这里返回完整DMC，而不是简化版本
        // 如果确实需要简化版本，请明确使用场景后再调整
        return generateDmc(dm);
    }

    @Override
    public void exportXml(String id, HttpServletResponse response) {
        try {
            // 1. 查询DM数据
            IetmDataModule dataModule = this.getById(id);
            if (dataModule == null) {
                throw new JeecgBootException("未找到ID为" + id + "的数据模块");
            }

            // 2. 检查是否有XML内容
            String xmlContent = dataModule.getDmContent();
            if (oConvertUtils.isEmpty(xmlContent)) {
                // 如果没有存储XML内容，则生成标准S1000D XML
                xmlContent = generateS1000DXml(dataModule);
            }

            // 3. 构建文件名（DMC码）
            String fileName = buildDmcCode(dataModule) + ".xml";

            // 4. 设置响应头
            response.setContentType("application/xml;charset=UTF-8");
            response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, "UTF-8"));

            // 5. 写入响应流
            response.getOutputStream().write(xmlContent.getBytes(StandardCharsets.UTF_8));
            response.getOutputStream().flush();

            log.info("成功导出XML文件，DM ID：{}，文件名：{}", id, fileName);

        } catch (Exception e) {
            log.error("导出XML文件失败，DM ID：{}", id, e);
            throw new JeecgBootException("导出XML文件失败：" + e.getMessage());
        }
    }

    /**
     * 生成符合S1000D标准的XML内容
     * @param dataModule 数据模块对象
     * @return XML字符串
     */
    private String generateS1000DXml(IetmDataModule dataModule) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<dmodule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("         xsi:noNamespaceSchemaLocation=\"http://www.s1000d.org/S1000D_5-0/xml_schema_flat/descript.xsd\">\n");

        // 标识和状态部分
        xml.append("  <identAndStatusSection>\n");
        xml.append("    <dmAddress>\n");
        xml.append("      <dmIdent>\n");

        // DMC码【方案A】SNS 含 equipname 作首段(=modelIdentCode)，按 - 拆分对标老系统
        java.util.Map<String, String> dmcSeg = DmcUtils.decomposeSns(dataModule.getSns());
        String dmcModelIdentCode = dmcSeg.get("modelIdentCode");
        if (dmcModelIdentCode == null || dmcModelIdentCode.trim().isEmpty()) {
            dmcModelIdentCode = DmcUtils.resolveModelIdentCode(dataModule.getSchema(), null);
        }
        // subSubSystemCode/disassyCodeVariant XSD 要求非空，缺省兜底老逻辑默认值
        String dmcSubSub = dmcSeg.get("subSubSystemCode");
        if (dmcSubSub == null || dmcSubSub.isEmpty()) dmcSubSub = "0";
        String dmcVariant = dmcSeg.get("disassyCodeVariant");
        if (dmcVariant == null || dmcVariant.isEmpty()) dmcVariant = "A";
        xml.append("        <dmCode");
        xml.append(" modelIdentCode=\"").append(dmcModelIdentCode).append("\"");
        xml.append(" systemDiffCode=\"").append(dmcSeg.get("systemDiffCode")).append("\"");
        xml.append(" systemCode=\"").append(dmcSeg.get("systemCode")).append("\"");
        xml.append(" subSystemCode=\"").append(dmcSeg.get("subSystemCode")).append("\"");
        xml.append(" subSubSystemCode=\"").append(dmcSubSub).append("\"");
        xml.append(" assyCode=\"").append(dmcSeg.get("assyCode")).append("\"");
        xml.append(" disassyCode=\"").append(dmcSeg.get("disassyCode")).append("\"");
        xml.append(" disassyCodeVariant=\"").append(dmcVariant).append("\"");
        xml.append(" infoCode=\"").append(nvl(dataModule.getInfoCode(), "")).append("\"");
        xml.append(" infoCodeVariant=\"").append(nvl(dataModule.getInfoCodeVariant(), "")).append("\"");
        xml.append(" itemLocationCode=\"").append(nvl(dataModule.getIetmLocationCode(), "A")).append("\"");
        if (!oConvertUtils.isEmpty(dataModule.getLearnCode())) {
            xml.append(" learnCode=\"").append(dataModule.getLearnCode()).append("\"");
        }
        if (!oConvertUtils.isEmpty(dataModule.getLearnEventCode())) {
            xml.append(" learnEventCode=\"").append(dataModule.getLearnEventCode()).append("\"");
        }
        xml.append("/>\n");

        // 语言信息
        xml.append("        <language languageIsoCode=\"").append(nvl(dataModule.getLanguageIsoCode(), "zh")).append("\"");
        xml.append(" countryIsoCode=\"").append(nvl(dataModule.getCountryIsoCode(), "CN")).append("\"/>\n");

        // 发行信息
        xml.append("        <issueInfo issueNumber=\"").append(nvl(dataModule.getIssueNo(), "001")).append("\"");
        xml.append(" inWork=\"").append(nvl(dataModule.getInWork(), "00")).append("\"/>\n");

        xml.append("      </dmIdent>\n");

      xml.append("      <dmAddressItems>\n");
        xml.append("        <issueDate year=\"").append(getCurrentYear()).append("\"");
        xml.append(" month=\"").append(getCurrentMonth()).append("\"");
        xml.append(" day=\"").append(getCurrentDay()).append("\"/>\n");

        // 标题
        xml.append("        <dmTitle>\n");
        xml.append("          <techName>").append(escapeXml(nvl(dataModule.getTechName(), ""))).append("</techName>\n");
        xml.append("          <infoName>").append(escapeXml(nvl(dataModule.getInfoName(), ""))).append("</infoName>\n");
        xml.append("        </dmTitle>\n");
        xml.append("      </dmAddressItems>\n");
        xml.append("    </dmAddress>\n");

        // 状态部分
        xml.append("    <dmStatus>\n");
        xml.append("      <security securityClassification=\"").append(nvl(dataModule.getSecurity(), "01")).append("\"/>\n");
        xml.append("      <responsiblePartnerCompany>\n");
        xml.append("        <enterpriseName>").append(escapeXml(nvl(dataModule.getRpcName(), ""))).append("</enterpriseName>\n");
        xml.append("      </responsiblePartnerCompany>\n");
        xml.append("      <originator>\n");
        xml.append("        <enterpriseName>").append(escapeXml(nvl(dataModule.getOriginatorName(), ""))).append("</enterpriseName>\n");
        xml.append("      </originator>\n");
        xml.append("    </dmStatus>\n");
        xml.append("  </identAndStatusSection>\n");

        // 内容部分（简化版，实际应该根据DM类型生成不同的内容）
        xml.append("  <content>\n");
        xml.append("    <description>\n");
        xml.append("      <para>").append(escapeXml(nvl(dataModule.getInfoName(), "数据模块内容"))).append("</para>\n");
        xml.append("    </description>\n");
        xml.append("  </content>\n");

        xml.append("</dmodule>\n");

        return xml.toString();
    }

    /**
     * 构建DMC码
     */
    /**
     * 构建DMC编码（用于文件名等场景）
     * 委托 generateDmc() 保证与存储/查重/展示完全一致（单一数据源）
     */
    private String buildDmcCode(IetmDataModule dm) {
        return generateDmc(dm);
    }

    // 辅助方法
    private String nvl(String value, String defaultValue) {
        return oConvertUtils.isEmpty(value) ? defaultValue : value;
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * 同步版本号：确保XML中的issueInfo与数据库字段一致
     * 原则：以数据库为准，自动修正XML
     * 用途：签出时自动同步新版本的XML
     *
     * @param dmId 数据模块ID
     */
    private void syncVersionToXml(String dmId) {
        IetmDataModule dm = ietmDataModuleMapper.selectById(dmId);
        if (dm == null || StringUtils.isEmpty(dm.getDmContent())) {
            return;
        }

        String content = dm.getDmContent();
        String dbIssueNo = dm.getIssueNo();
        String dbInWork = StringUtils.isNotEmpty(dm.getInWork()) ? dm.getInWork() : "00";

        if (StringUtils.isEmpty(dbIssueNo)) {
            return;
        }

        try {
            // 使用静态常量Pattern（性能优化）
            java.util.regex.Matcher matcher = ISSUE_INFO_TAG_PATTERN.matcher(content);

            if (matcher.find()) {
                // 提取当前XML中的版本号
                String issueInfoTag = matcher.group();

                java.util.regex.Matcher issueNumberMatcher = ISSUE_NUMBER_ATTR_PATTERN.matcher(issueInfoTag);
                java.util.regex.Matcher inWorkMatcher = IN_WORK_ATTR_PATTERN.matcher(issueInfoTag);

                String xmlIssueNumber = issueNumberMatcher.find() ? issueNumberMatcher.group(1) : "";
                String xmlInWork = inWorkMatcher.find() ? inWorkMatcher.group(1) : "";

                // 如果不一致，替换整个标签
                if (!xmlIssueNumber.equals(dbIssueNo) || !xmlInWork.equals(dbInWork)) {
                    String newTag = String.format(
                        "<issueInfo issueNumber=\"%s\" inWork=\"%s\"/>",
                        escapeXml(dbIssueNo),
                        escapeXml(dbInWork)
                    );

                    content = ISSUE_INFO_TAG_PATTERN.matcher(content).replaceAll(
                        java.util.regex.Matcher.quoteReplacement(newTag)
                    );

                    // 重新保存修正后的XML
                    IetmDataModule update = new IetmDataModule();
                    update.setId(dmId);
                    update.setDmContent(content);
                    ietmDataModuleMapper.updateById(update);

                    log.debug("[版本号同步] DM: {}, 已将XML版本号从 {}-{} 修正为 {}-{}",
                        dmId, xmlIssueNumber, xmlInWork, dbIssueNo, dbInWork);
                }
            }
        } catch (Exception e) {
            log.error("[版本号同步失败] DM: {}, 错误: {}", dmId, e.getMessage(), e);
            // 同步失败不影响签出，只记录日志
        }
    }

    private String getCurrentYear() {
        return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    }

    private String getCurrentMonth() {
        return String.format("%02d", Calendar.getInstance().get(Calendar.MONTH) + 1);
    }

    private String getCurrentDay() {
        return String.format("%02d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
    }

    @Override
    // 注意：importZip 允许部分成功（逐条导入，记录成功/失败数），不使用全局事务
    // 每条 saveDm() 调用内部有独立事务，失败时只回滚该条，不影响其他
    public Map<String, Object> importZip(MultipartFile file, String projectId) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<String> errorMessages = new ArrayList<>();

        try {
            // 1. 验证文件
            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("message", "上传文件为空");
                return result;
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".zip")) {
                result.put("success", false);
                result.put("message", "文件格式错误，仅支持ZIP格式");
                return result;
            }

            // 2. 解压ZIP文件，查找所有XML文件
            try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream())) {
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String entryName = zipEntry.getName();

                // 跳过目录和非XML文件
                if (zipEntry.isDirectory() || !entryName.toLowerCase().endsWith(".xml")) {
                    zipInputStream.closeEntry();
                    continue;
                }

                try {
                    // 3. 读取XML内容
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zipInputStream.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    String xmlContent = baos.toString("UTF-8");

                    // 4. 解析并导入单个XML
                    SAXReader reader = createSafeSaxReader();
                    Document document = reader.read(new java.io.ByteArrayInputStream(xmlContent.getBytes("UTF-8")));
                    Element root = document.getRootElement();

                    // 5. 提取DM信息
                    IetmDataModule dataModule = extractDataModuleFromXml(root, projectId);
                    dataModule.setDmContent(xmlContent);

                    // 6. 保存到数据库
                    boolean saved = this.saveDm(dataModule);
                    if (saved) {
                        successCount++;
                        log.info("导入XML成功：{}", entryName);
                    } else {
                        failCount++;
                        errorMessages.add(entryName + "：保存失败");
                    }

                } catch (Exception e) {
                    failCount++;
                    errorMessages.add(entryName + "：" + e.getMessage());
                    log.error("导入XML失败：{}，错误：{}", entryName, e.getMessage());
                }

                zipInputStream.closeEntry();
            }
            } // end try-with-resources ZipInputStream

            // 7. 返回结果
            result.put("success", failCount == 0);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("message", String.format("导入完成：成功%d个，失败%d个", successCount, failCount));
            if (!errorMessages.isEmpty()) {
                result.put("errors", errorMessages);
            }

        } catch (Exception e) {
            log.error("ZIP导入失败", e);
            result.put("success", false);
            result.put("message", "ZIP解析失败：" + e.getMessage());
        }

        return result;
    }

    @Override
    public boolean validateDmc(IetmDataModule dataModule) {
        IetmDataModule existDm = ietmDataModuleMapper.selectByDmcForValidation(
            dataModule.getSns(),
            dataModule.getInfoCode(),
            dataModule.getInfoCodeVariant(),
            dataModule.getIetmLocationCode(),
            dataModule.getLanguageIsoCode(),
            dataModule.getCountryIsoCode(),
            dataModule.getId()
        );
        return existDm != null;
    }

    @Override
    public Map<String, Object> validateContent(String content) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        boolean valid = true;

        try {
            // 1. 基本非空校验
            if (oConvertUtils.isEmpty(content)) {
                errors.add("XML内容为空");
                result.put("valid", false);
                result.put("errors", errors);
                return result;
            }

            // 2. 尝试解析XML
            SAXReader reader = createSafeSaxReader();
            Document document;
            try {
                document = reader.read(new StringReader(content));
            } catch (DocumentException e) {
                errors.add("XML格式错误：" + e.getMessage());
                result.put("valid", false);
                result.put("errors", errors);
                return result;
            }

            Element root = document.getRootElement();

            // 3. 校验根节点
            if (root == null) {
                errors.add("XML根节点不存在");
                valid = false;
            } else {
                String rootName = root.getName();
                // S1000D标准DM根节点应为dmodule
                if (!"dmodule".equalsIgnoreCase(rootName)) {
                    errors.add("根节点名称错误，期望：dmodule，实际：" + rootName);
                    valid = false;
                }

                // 4. 校验必要子节点
                Element identAndStatusSection = root.element("identAndStatusSection");
                if (identAndStatusSection == null) {
                    errors.add("缺少identAndStatusSection节点");
                    valid = false;
                } else {
                    // 校验DMC标识信息
                    Element dmAddress = identAndStatusSection.element("dmAddress");
                    if (dmAddress == null) {
                        errors.add("缺少dmAddress节点");
                        valid = false;
                    } else {
                        Element dmIdent = dmAddress.element("dmIdent");
                        Element dmCode = dmIdent != null ? dmIdent.element("dmCode") : null;
                        if (dmCode == null) {
                            errors.add("缺少dmCode节点");
                            valid = false;
                        }
                    }

                    // 校验状态信息
                    Element dmStatus = identAndStatusSection.element("dmStatus");
                    if (dmStatus == null) {
                        errors.add("缺少dmStatus节点");
                        valid = false;
                    }
                }

                // 5. 校验内容节点（content）
                Element contentElement = root.element("content");
                if (contentElement == null) {
                    errors.add("缺少content节点");
                    valid = false;
                }
            }

            // 6. 返回校验结果
            result.put("valid", valid);
            result.put("errors", errors);

            if (valid) {
                log.info("DM内容校验通过");
            } else {
                log.warn("DM内容校验失败，错误：{}", errors);
            }

        } catch (Exception e) {
            log.error("DM内容校验异常", e);
            errors.add("校验异常：" + e.getMessage());
            result.put("valid", false);
            result.put("errors", errors);
        }

        return result;
    }

    @Override
    public String generateDmc(IetmDataModule dataModule) {
        // 输入白名单校验（防止 SQL 注入和路径遍历）
        validateDmcInput(dataModule);

        // 防御性检查：SNS 空会导致 DMC 双横线（`DMC--...`）
        if (oConvertUtils.isEmpty(dataModule.getSns())) {
            log.warn("生成 DMC 时 SNS 为空，将产生双横线格式（DMC--...），请检查构型路径，dmId={}", dataModule.getId());
        }

        // 生成DMC编码：逐字符对标老系统 IetmEditorUtils.js getDmc()（纯S1000D缩略标识+文件名后缀）：
        // DMC-{sns}-{infoCode}{infoCodeVariant}-{itemLocationCode}_{issueNo}-{inWork}_{lang}-{country}
        // 注：yearOfChange/seqNo/originator/learnCode 不进 DMC 字符串（老系统 getDmc 亦不含），仅存实体列与 <dmCode> XML 属性。
        StringBuilder dmc = new StringBuilder("DMC-");

        // SNS（系统编号，含 equipname 首段=modelIdentCode）
        dmc.append(oConvertUtils.getString(dataModule.getSns(), "")).append("-");

        // InfoCode + InfoCodeVariant（信息码+变体，无分隔符）
        dmc.append(oConvertUtils.getString(dataModule.getInfoCode(), ""));
        if (oConvertUtils.isNotEmpty(dataModule.getInfoCodeVariant())) {
            dmc.append(dataModule.getInfoCodeVariant());
        }
        dmc.append("-");

        // ItemLocationCode（位置码，默认A；老系统 getDmc 此段仅 itemLocationCode）
        dmc.append(oConvertUtils.getString(dataModule.getIetmLocationCode(), "A"));

        // 文件名后缀：_{issueNo}-{inWork}（发行块，下划线起始，连字符分隔）
        dmc.append("_").append(oConvertUtils.getString(dataModule.getIssueNo(), "001"));
        dmc.append("-").append(oConvertUtils.getString(dataModule.getInWork(), "00"));

        // 文件名后缀：_{lang}-{country}（语言块，下划线起始，连字符分隔）
        dmc.append("_").append(oConvertUtils.getString(dataModule.getLanguageIsoCode(), "zh"));
        dmc.append("-").append(oConvertUtils.getString(dataModule.getCountryIsoCode(), "CN"));

        return dmc.toString();
    }

    @Override
    public Map<String, String> calculateVersion(String currentInwork, String currentIssueno, String versionType) {
        Map<String, String> result = new HashMap<>();
        int inwork = Integer.parseInt(oConvertUtils.getString(currentInwork, "0"));
        int issueno = Integer.parseInt(oConvertUtils.getString(currentIssueno, "1"));
        
        if ("inwork".equals(versionType)) {
            if (inwork >= 99) {
                issueno++;
                inwork = 0;
            } else {
                inwork++;
            }
        } else if ("issue".equals(versionType)) {
            issueno++;
            inwork = 0;
        }
        
        result.put("newInwork", String.format("%02d", inwork));
        result.put("newIssueno", String.format("%03d", issueno));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String copyDm(String id, String targetProjectId, Integer copyType, String username) {
        log.info("复制数据模块，源ID：{}，目标项目：{}，类型：{}", id, targetProjectId, copyType);
        IetmDataModule sourceDm = this.getById(id);
        if (sourceDm == null) {
            throw new JeecgBootException("源数据模块不存在");
        }

        IetmDataModule newDm = new IetmDataModule();

        // 如果未指定目标项目，则使用源DM的项目
        String targetProjId = (targetProjectId != null && !targetProjectId.isEmpty())
            ? targetProjectId : sourceDm.getProjectId();

        // 复制基本字段
        newDm.setProjectId(targetProjId);
        newDm.setProjectName(sourceDm.getProjectName());
        // newDm.setProjectCode(sourceDm.getProjectCode());  // Entity没有该字段
        newDm.setSns(sourceDm.getSns());
        newDm.setInfoCode(sourceDm.getInfoCode());
        newDm.setInfoCodeVariant(sourceDm.getInfoCodeVariant());
        newDm.setIetmLocationCode(sourceDm.getIetmLocationCode());
        newDm.setLearnCode(sourceDm.getLearnCode());
        newDm.setLearnEventCode(sourceDm.getLearnEventCode());
        newDm.setOriginator(sourceDm.getOriginator());
        newDm.setOriginatorName(sourceDm.getOriginatorName());
        newDm.setTechName(sourceDm.getTechName());
        newDm.setInfoName(sourceDm.getInfoName());
        newDm.setTechNameEn(sourceDm.getTechNameEn());
        newDm.setInfoNameEn(sourceDm.getInfoNameEn());
        newDm.setDmContent(sourceDm.getDmContent());
        newDm.setDmType(sourceDm.getDmType());
        newDm.setSecurity(sourceDm.getSecurity());
        newDm.setRpc(sourceDm.getRpc());
        newDm.setRpcName(sourceDm.getRpcName());
        newDm.setCmNodeId(sourceDm.getCmNodeId());      // 字段名修正
        newDm.setCmNodeName(sourceDm.getCmNodeName());  // 字段名修正
        newDm.setCmNodePath(sourceDm.getCmNodePath());  // 字段名修正
        newDm.setLanguageIsoCode(sourceDm.getLanguageIsoCode());
        newDm.setCountryIsoCode(sourceDm.getCountryIsoCode());
        newDm.setSchema(sourceDm.getSchema());
        newDm.setYearOfChange(sourceDm.getYearOfChange());
        newDm.setSeqNo(sourceDm.getSeqNo());

        // 根据copyType决定版本控制字段（mainId/versionPath/isOriginal字段不存在，版本控制逻辑已简化）
        if (copyType != null && copyType == 1) {
            // type=1：创建新版本链
            // newDm.setMainId(sourceDm.getMainId() != null ? sourceDm.getMainId() : sourceDm.getId()); // 字段不存在
            // newDm.setIsOriginal("0");  // 字段不存在
            // newDm.setVersionPath(sourceDm.getVersionPath() + "," + sourceDm.getId());  // 字段不存在

            // 继承版本号并升级inwork
            String currentInwork = sourceDm.getInWork() != null ? sourceDm.getInWork() : "00";
            Map<String, String> newVersion = calculateVersion(currentInwork, sourceDm.getIssueNo(), "inwork");
            newDm.setInWork(newVersion.get("newInwork"));
            newDm.setIssueNo(sourceDm.getIssueNo());

            // 将源DM的isLatest设为0
            sourceDm.setIsLatest("0");
            this.updateById(sourceDm);
        } else {
            // type=0：仅复制属性（创建全新DM）
            // newDm.setMainId(null); // 字段不存在，已注释
            // newDm.setIsOriginal("1");  // 字段不存在
            // newDm.setVersionPath(null); // 字段不存在，已注释
            newDm.setInWork("00");
            newDm.setIssueNo("001");
        }

        newDm.setIsLatest("1");
        newDm.setStatus("1");

        this.saveDm(newDm);

        // ✅ type=1（新版本链）：源版本已降为历史(is_latest='0')，新版本需继承资源快照，
        // 与 checkOut 语义一致——否则资源列表在切换到新版本后为空。
        // type=0（属性复制）：创建全新DM，不携带原DM资源，不复制。
        if (copyType != null && copyType == 1) {
            int copiedRes = copyDmResources(id, newDm.getId(), username);
            if (copiedRes > 0) {
                log.info("copyDm(type=1) 复制资源：{} -> {}, 复制 {} 条", id, newDm.getId(), copiedRes);
            }
        }

        // ✅ 复制后同步版本号到XML（复制的dmContent中issueInfo标签是旧版本号）
        syncVersionToXml(newDm.getId());

        return newDm.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startWorkflow(String id, String processKey, String username) {
        IetmDataModule dm = this.getById(id);
        if (dm == null) {
            throw new RuntimeException("DM不存在");
        }

        try {
            // TODO: 集成工作流引擎（Flowable/Activiti）
            // 1. 创建流程实例
            // ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            //     processKey,
            //     id,
            //     createProcessVariables(dm, username)
            // );

            // 暂时返回模拟的实例ID，等待集成工作流引擎
            String mockInstanceId = "workflow-" + id + "-" + System.currentTimeMillis();

            // TODO: 集成工作流引擎后，用专属常量替换此状态值
            // 注意：STATUS_PUBLISHED="2" 含义为"已发布"，此处"审批中"需区分
            dm.setStatus("2"); // 占位：待工作流集成后改为审批中专属状态
            dm.setWorkflowStatus("1"); // 1=流转中（方案A：只回写 workflowStatus，不回写 workflowInstanceId）
            this.updateById(dm);

            log.info("启动工作流成功，DM ID：{}，流程Key：{}，用户：{}，Mock实例ID：{}", id, processKey, username, mockInstanceId);

            return mockInstanceId;

        } catch (Exception e) {
            log.error("启动工作流失败", e);
            throw new RuntimeException("启动工作流失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeWorkflowTask(String id, String taskId, boolean approved, String comment, String username) {
        IetmDataModule dm = this.getById(id);
        if (dm == null) {
            throw new RuntimeException("DM不存在");
        }

        try {
            // TODO: 集成工作流引擎（Flowable/Activiti）
            // 1. 完成任务
            // Map<String, Object> variables = new HashMap<>();
            // variables.put("approved", approved);
            // variables.put("comment", comment);
            // taskService.complete(taskId, variables);

            // 2. 更新DM状态和工作流状态
            if (approved) {
                dm.setStatus("3"); // 假设3表示已批准
                dm.setWorkflowStatus("0"); // 0=已结束（方案A：只回写 workflowStatus）
            } else {
                dm.setStatus("4"); // 假设4表示已拒绝
                dm.setWorkflowStatus("0"); // 0=已结束
            }
            // 注意：不设置 workflowStep 和 workflowHandler，这些字段从 v_wf_instance 视图动态获取
            this.updateById(dm);

            log.info("完成工作流任务成功，DM ID：{}，任务ID：{}，是否通过：{}，workflowStatus已更新", id, taskId, approved);
            return true;

        } catch (Exception e) {
            log.error("完成工作流任务失败", e);
            throw new RuntimeException("完成工作流任务失败：" + e.getMessage());
        }
    }

    @Override
    public void previewDm(String id, HttpServletResponse response) {
        IetmDataModule dm = this.getById(id);
        if (dm == null) {
            throw new RuntimeException("DM不存在");
        }

        try {
            response.setContentType("text/html;charset=UTF-8");
            response.setHeader("Content-Disposition", "inline");

            // 渲染DM内容为HTML
            String htmlContent = renderDmToHtml(dm);
            response.getWriter().write(htmlContent);
            response.getWriter().flush();
        } catch (Exception e) {
            log.error("预览DM失败", e);
            throw new RuntimeException("预览失败：" + e.getMessage());
        }
    }

    /**
     * 将DM XML内容渲染为HTML
     * @param dm 数据模块
     * @return HTML内容
     */
    private String renderDmToHtml(IetmDataModule dm) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='zh-CN'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>").append(escapeHtml(dm.getTechName())).append("</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }");
        html.append("h1 { color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }");
        html.append("h2 { color: #555; margin-top: 20px; }");
        html.append(".info-table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append(".info-table th, .info-table td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append(".info-table th { background-color: #4CAF50; color: white; }");
        html.append(".content-section { margin: 20px 0; padding: 15px; background-color: #f9f9f9; border-radius: 5px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // 标题
        html.append("<h1>").append(escapeHtml(dm.getTechName())).append("</h1>");
        html.append("<h2>").append(escapeHtml(dm.getInfoName())).append("</h2>");

        // 基本信息表
        html.append("<table class='info-table'>");
        html.append("<tr><th colspan='2'>数据模块信息</th></tr>");
        // html.append("<tr><td><strong>DMC编码</strong></td><td>").append(escapeHtml(dm.getDmc())).append("</td></tr>");  // Entity没有该字段
        html.append("<tr><td><strong>ID</strong></td><td>").append(escapeHtml(dm.getId())).append("</td></tr>");
        html.append("<tr><td><strong>SNS</strong></td><td>").append(escapeHtml(dm.getSns())).append("</td></tr>");
        html.append("<tr><td><strong>信息代码</strong></td><td>").append(escapeHtml(dm.getInfoCode())).append("</td></tr>");
        html.append("<tr><td><strong>发行编号</strong></td><td>").append(escapeHtml(dm.getIssueNo())).append("</td></tr>");
        html.append("<tr><td><strong>在编版本</strong></td><td>").append(escapeHtml(dm.getInWork())).append("</td></tr>");
        html.append("<tr><td><strong>语言</strong></td><td>").append(escapeHtml(dm.getLanguageIsoCode())).append("</td></tr>");
        html.append("<tr><td><strong>国家</strong></td><td>").append(escapeHtml(dm.getCountryIsoCode())).append("</td></tr>");
        html.append("<tr><td><strong>类型</strong></td><td>").append(escapeHtml(dm.getDmType())).append("</td></tr>");
        html.append("<tr><td><strong>安全等级</strong></td><td>").append(escapeHtml(dm.getSecurity())).append("</td></tr>");
        html.append("</table>");

        // XML内容解析（简化版，展示主要内容节点）
        String dmContent = dm.getDmContent();
        if (oConvertUtils.isNotEmpty(dmContent)) {
            try {
                SAXReader reader = createSafeSaxReader();
                Document document = reader.read(new StringReader(dmContent));
                Element root = document.getRootElement();

                // 提取并显示content节点内容
                Element content = root.element("content");
                if (content != null) {
                    html.append("<div class='content-section'>");
                    html.append("<h2>内容</h2>");
                    html.append(extractContentHtml(content));
                    html.append("</div>");
                }
            } catch (Exception e) {
                log.warn("解析DM XML内容失败：{}", e.getMessage());
                html.append("<div class='content-section'>");
                html.append("<h2>XML内容</h2>");
                html.append("<pre>").append(escapeHtml(dmContent)).append("</pre>");
                html.append("</div>");
            }
        } else {
            html.append("<p><em>暂无内容</em></p>");
        }

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 从XML content节点提取HTML
     * @param contentElement content节点
     * @return HTML内容
     */
    private String extractContentHtml(Element contentElement) {
        StringBuilder html = new StringBuilder();

        // 递归提取所有文本内容和标题
        extractElementContent(contentElement, html, 1);

        return html.toString();
    }

    /**
     * 递归提取XML元素内容
     * @param element 当前元素
     * @param html HTML构建器
     * @param level 标题级别
     */
    private void extractElementContent(Element element, StringBuilder html, int level) {
        String elementName = element.getName();

        // 处理标题节点
        if (elementName.contains("title") || elementName.contains("Title")) {
            int headerLevel = Math.min(level + 1, 6);
            html.append("<h").append(headerLevel).append(">")
                .append(escapeHtml(element.getTextTrim()))
                .append("</h").append(headerLevel).append(">");
        }
        // 处理段落节点
        else if (elementName.contains("para") || elementName.contains("Para")) {
            html.append("<p>").append(escapeHtml(element.getTextTrim())).append("</p>");
        }
        // 处理列表
        else if (elementName.contains("list") || elementName.contains("List")) {
            html.append("<ul>");
            List<Element> items = element.elements();
            for (Element item : items) {
                html.append("<li>").append(escapeHtml(item.getTextTrim())).append("</li>");
            }
            html.append("</ul>");
        }
        // 其他节点递归处理子元素
        else {
            List<Element> children = element.elements();
            if (children.isEmpty()) {
                // 叶子节点，输出文本
                String text = element.getTextTrim();
                if (oConvertUtils.isNotEmpty(text)) {
                    html.append("<p>").append(escapeHtml(text)).append("</p>");
                }
            } else {
                // 有子元素，递归处理
                for (Element child : children) {
                    extractElementContent(child, html, level + 1);
                }
            }
        }
    }


    @Override
    public List<IetmDataModule> searchDm(String keyword, String projectId) {
        LambdaQueryWrapper<IetmDataModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IetmDataModule::getStatus, "1");

        if (projectId != null && !projectId.isEmpty()) {
            queryWrapper.eq(IetmDataModule::getProjectId, projectId);
        }

        // 全文搜索：DMC编码、技术名称、信息名称
        queryWrapper.and(wrapper -> wrapper
                .like(IetmDataModule::getDmcCode, keyword)
                .or().like(IetmDataModule::getTechName, keyword)
                .or().like(IetmDataModule::getInfoName, keyword)
        );

        return this.list(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> queryDmResources(String dmId) {
        return ietmDmCommentMapper.selectByDmId(dmId);
    }

    /**
     * 复制资源关联：签出时把原版本(fromDmId)的资源记录复制到新工作版本(toDmId)。
     * 只改挂 dm_id，其余字段（file_path 等物理文件指针）保持不变，实现版本各自独立的资源快照。
     * @return 复制的记录数
     */
    private int copyDmResources(String fromDmId, String toDmId, String username) {
        List<IetmDmComment> resources = ietmDmCommentMapper.selectList(
                new LambdaQueryWrapper<IetmDmComment>().eq(IetmDmComment::getDmId, fromDmId));
        if (resources == null || resources.isEmpty()) {
            return 0;
        }
        Date now = new Date();
        int count = 0;
        for (IetmDmComment src : resources) {
            IetmDmComment copy = new IetmDmComment();
            org.springframework.beans.BeanUtils.copyProperties(src, copy);
            copy.setId(null);            // 让 MyBatis-Plus 生成新主键
            copy.setDmId(toDmId);        // 改挂到新版本
            copy.setCreateBy(username);
            copy.setCreateTime(now);
            copy.setUpdateBy(username);
            copy.setUpdateTime(now);
            count += ietmDmCommentMapper.insert(copy);
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDmResource(String dmId, String fileId, String resourceName, Long fileSize, String comment) {
        // 从fileId（文件路径）中提取文件名
        String fileName = fileId;
        if (fileId != null && fileId.contains("/")) {
            fileName = fileId.substring(fileId.lastIndexOf("/") + 1);
        }

        IetmDmComment dmComment = new IetmDmComment();
        dmComment.setDmId(dmId);
        dmComment.setFilePath(fileId);  // fileId存储在filePath字段中
        dmComment.setFileName(fileName); // 设置文件名
        dmComment.setResourceName(resourceName);
        dmComment.setFileSize(fileSize); // 设置文件大小
        dmComment.setRemark(comment);   // comment存储在remark字段中
        dmComment.setOperateTime(new Date());
        dmComment.setOperator(getCurrentUsername());  // 从当前登录用户获取

        int result = ietmDmCommentMapper.insert(dmComment);

        // 注意：IetmDataModule没有resourceCount字段，此处不更新
        // 如需统计资源数量，可在查询时动态计算

        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDmResource(String id, String comment) {
        IetmDmComment dmComment = ietmDmCommentMapper.selectById(id);
        if (dmComment == null) {
            throw new RuntimeException("资源不存在");
        }
        dmComment.setRemark(comment);   // 使用remark字段
        dmComment.setOperateTime(new Date());
        return ietmDmCommentMapper.updateById(dmComment) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDmResource(String id) {
        IetmDmComment dmComment = ietmDmCommentMapper.selectById(id);
        if (dmComment == null) {
            throw new RuntimeException("资源不存在");
        }

        int result = ietmDmCommentMapper.deleteById(id);

        return result > 0;
    }

    @Override
    public boolean deleteDmResourceFile(String id) {
        IetmDmComment dmComment = ietmDmCommentMapper.selectById(id);
        if (dmComment == null) {
            throw new RuntimeException("资源不存在");
        }

        // 删除物理文件
        String fileId = dmComment.getFilePath();  // fileId存储在filePath字段中
        if (oConvertUtils.isNotEmpty(fileId)) {
            try {
                // TODO: 集成文件服务（JeecgBoot文件管理模块）
                // 方式1：通过MinIO/OSS删除
                // minioClient.removeObject(bucketName, fileId);

                // 方式2：通过JeecgBoot CommonAPI删除
                // commonAPI.deleteFileById(fileId);

                // 方式3：本地文件删除
                // File file = new File(uploadPath + "/" + fileId);
                // if (file.exists()) {
                //     file.delete();
                // }

                log.info("已标记删除物理文件，fileId：{}（待集成文件服务）", fileId);
            } catch (Exception e) {
                log.error("删除物理文件失败，fileId：{}，错误：{}", fileId, e.getMessage());
                // 不抛出异常，允许继续删除数据库记录
            }
        }

        // 删除数据库记录
        return ietmDmCommentMapper.deleteById(id) > 0;
    }

    @Override
    public List<IetmDataModule> queryHistoryVersions(String projectId, String sns, String infoCode,
                                                     String infoCodeVariant, String ietmLocationCode, Boolean onlyPublished) {
        log.info("查询历史版本 projectId={}, sns={}, infoCode={}, variant={}, locationCode={}, onlyPublished={}",
                projectId, sns, infoCode, infoCodeVariant, ietmLocationCode, onlyPublished);
        // 走 Mapper：含 status IN ('1','2') 过滤、issue_no/in_work 倒序、包含 dm_content
        return baseMapper.selectHistoryVersions(projectId, sns, infoCode, infoCodeVariant, ietmLocationCode, onlyPublished);
    }

    @Override
    public Map<String, Object> compareVersions(String sourceId, String targetId) {
        IetmDataModule source = baseMapper.selectContentById(sourceId);
        IetmDataModule target = baseMapper.selectContentById(targetId);
        Map<String, Object> data = new HashMap<>(4);
        if (source == null || target == null) {
            log.warn("版本记录不存在，sourceId={}, targetId={}", sourceId, targetId);
            return data;
        }

        String sourceContent = getContentWithTemplateFallback(source);
        String targetContent = getContentWithTemplateFallback(target);

        data.put("sourceContent", sourceContent);
        data.put("targetContent", targetContent);
        return data;
    }

    /**
     * 获取DM内容，如果dm_content为空则从模板加载
     * @param dm DM实体
     * @return XML内容
     */
    private String getContentWithTemplateFallback(IetmDataModule dm) {
        String content = dm.getDmContent();
        if (StringUtils.isNotBlank(content)) {
            return content;
        }

        // dm_content为空，从模板加载
        String standard = "S1000D4.0";
        if (StringUtils.isNotBlank(dm.getProjectId())) {
            IetmProject project = projectService.getById(dm.getProjectId());
            if (project != null && StringUtils.isNotBlank(project.getIetmStandard())) {
                standard = project.getIetmStandard();
            }
        }

        String templateFile = null;
        if (StringUtils.isNotBlank(dm.getDmType())) {
            templateFile = getDmTypeTemplateFile(dm.getDmType(), standard);
        }
        if (StringUtils.isBlank(templateFile)) {
            templateFile = "descript.xml";
        }

        try {
            return DmXmlHelper.loadTemplate(standard, templateFile, dm);
        } catch (Exception e) {
            log.error("DM[{}] 加载模板失败", dm.getId(), e);
            return "";
        }
    }

    /**
     * 从dm_type表获取模板文件名
     * @param dmTypeCode DM类型编码
     * @param standard 标准
     * @return 模板文件名
     */
    private String getDmTypeTemplateFile(String dmTypeCode, String standard) {
        try {
            if (StringUtils.isBlank(dmTypeCode) || StringUtils.isBlank(standard)) {
                return null;
            }

            LambdaQueryWrapper<IetmDmType> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(IetmDmType::getTypeCode, dmTypeCode)
                   .eq(IetmDmType::getIetmStandard, standard)
                   .eq(IetmDmType::getStatus, "1")
                   .last("FETCH FIRST 1 ROWS ONLY");

            IetmDmType dmType = dmTypeMapper.selectOne(wrapper);
            if (dmType != null && StringUtils.isNotBlank(dmType.getTemplateFile())) {
                return dmType.getTemplateFile();
            }

            return null;
        } catch (Exception e) {
            log.warn("获取dm_type模板文件失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> queryReferenceTree(String dmId, String refType) {
        log.info("查询引用关系树，DM ID：{}，引用类型：{}", dmId, refType);

        // 使用Set记录已访问的DM，防止循环引用导致无限递归
        Set<String> visited = new HashSet<>();
        visited.add(dmId); // 根节点加入已访问集合

        if ("out".equals(refType)) {
            // 查询出引用（当前DM引用了哪些DM）
            return buildReferenceTree(dmId, refType, visited, 1, 10);
        } else if ("in".equals(refType)) {
            // 查询入引用（哪些DM引用了当前DM）
            return buildReferenceTree(dmId, refType, visited, 1, 10);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * 递归构建引用关系树
     *
     * @param dmId 当前DM ID
     * @param refType 引用类型（out/in）
     * @param visited 已访问的DM ID集合（防止循环引用）
     * @param currentDepth 当前深度
     * @param maxDepth 最大深度限制（默认10层）
     * @return 树形结构的引用关系列表
     */
    private List<Map<String, Object>> buildReferenceTree(String dmId, String refType,
                                                          Set<String> visited, int currentDepth, int maxDepth) {
        // 深度限制，防止无限递归
        if (currentDepth > maxDepth) {
            log.warn("引用关系树深度超过限制 {}，停止递归", maxDepth);
            return new ArrayList<>();
        }

        // 查询当前层级的直接引用
        List<Map<String, Object>> directRefs;
        if ("out".equals(refType)) {
            directRefs = ietmDmRefMapper.selectOutReferences(dmId);
        } else {
            directRefs = ietmDmRefMapper.selectInReferences(dmId);
        }

        if (directRefs == null || directRefs.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建树形结构
        List<Map<String, Object>> treeNodes = new ArrayList<>();
        for (Map<String, Object> ref : directRefs) {
            // 获取子节点的DM ID
            String childDmId = "out".equals(refType)
                ? (String) ref.get("targetDmId")
                : (String) ref.get("sourceDmId");

            // 检查是否循环引用
            boolean isCircular = visited.contains(childDmId);
            ref.put("isCircular", isCircular);
            ref.put("refDepth", currentDepth);

            // 如果不是循环引用，递归查询子节点
            if (!isCircular) {
                visited.add(childDmId);
                List<Map<String, Object>> children = buildReferenceTree(
                    childDmId, refType, visited, currentDepth + 1, maxDepth
                );
                if (!children.isEmpty()) {
                    ref.put("children", children);
                }
                // 回溯：移除当前节点（允许在不同分支中重复出现）
                visited.remove(childDmId);
            }

            treeNodes.add(ref);
        }

        return treeNodes;
    }

    @Override
    public List<Map<String, Object>> queryReferenceChain(String rootDmId, String targetDmId, String refType) {
        log.info("查询引用链路径，根DM ID：{}，目标DM ID：{}，引用类型：{}", rootDmId, targetDmId, refType);

        // 调用Mapper查询引用链（包含path字段）
        List<Map<String, Object>> allNodes = ietmDmRefMapper.selectReferenceChain(rootDmId, targetDmId, refType);

        log.info("SQL返回的节点数量：{}", allNodes == null ? 0 : allNodes.size());
        if (allNodes != null && !allNodes.isEmpty()) {
            for (Map<String, Object> node : allNodes) {
                log.info("节点：dmId={}, dmcCode={}, depth={}, path={}",
                    node.get("dmId"), node.get("dmcCode"), node.get("depth"), node.get("path"));
            }
        }

        if (allNodes == null || allNodes.isEmpty()) {
            log.warn("未找到从 {} 到 {} 的引用链路径", rootDmId, targetDmId);
            return new ArrayList<>();
        }

        // 找到包含targetDmId的最短路径（depth最小）
        Map<String, Object> targetNode = null;
        int minDepth = Integer.MAX_VALUE;

        for (Map<String, Object> node : allNodes) {
            String dmId = String.valueOf(node.get("dmId"));
            if (targetDmId.equals(dmId)) {
                Object depthObj = node.get("depth");
                int depth = Integer.MAX_VALUE;

                // 处理多种数字类型
                if (depthObj instanceof Integer) {
                    depth = (Integer) depthObj;
                } else if (depthObj instanceof Long) {
                    depth = ((Long) depthObj).intValue();
                } else if (depthObj instanceof java.math.BigDecimal) {
                    depth = ((java.math.BigDecimal) depthObj).intValue();
                } else if (depthObj != null) {
                    try {
                        depth = Integer.parseInt(String.valueOf(depthObj));
                    } catch (NumberFormatException e) {
                        log.warn("depth字段类型转换失败：{}, 类型：{}", depthObj, depthObj.getClass().getName());
                    }
                }

                log.info("找到候选路径：dmId={}, depth={} (原始类型: {}), path={}",
                    dmId, depth, depthObj != null ? depthObj.getClass().getSimpleName() : "null", node.get("path"));

                if (depth < minDepth) {
                    minDepth = depth;
                    targetNode = node;
                }
            }
        }

        if (targetNode != null) {
            log.info("选择最短路径：depth={}, path={}", minDepth, targetNode.get("path"));
        }

        if (targetNode == null) {
            log.warn("未找到目标节点 {} 在引用链中", targetDmId);
            return new ArrayList<>();
        }

        // 从path字段提取完整路径的所有节点ID
        String pathStr = String.valueOf(targetNode.get("path"));
        if (pathStr == null || "null".equals(pathStr) || pathStr.isEmpty()) {
            log.warn("目标节点的path字段为空");
            return new ArrayList<>();
        }

        log.info("解析path字段：{}", pathStr);
        String[] pathIds = pathStr.split(",");
        log.info("path拆分后的ID数组：{}", java.util.Arrays.toString(pathIds));

        // 按照path顺序构建完整链路
        // 需要为路径上的每个节点查询完整信息
        List<Map<String, Object>> chain = new ArrayList<>();

        for (int i = 0; i < pathIds.length; i++) {
            String pathId = pathIds[i].trim();

            // 第一个节点是根节点，需要单独查询
            if (i == 0) {
                Map<String, Object> rootNode = new HashMap<>();
                // 查询根节点的完整信息
                IetmDataModule rootDm = this.getById(pathId);
                if (rootDm != null) {
                    rootNode.put("dmId", rootDm.getId());
                    rootNode.put("dmcCode", rootDm.getDmcCode());
                    rootNode.put("techName", rootDm.getTechName());
                    rootNode.put("infoName", rootDm.getInfoName());
                    rootNode.put("refType", null);
                    rootNode.put("refPosition", null);
                    chain.add(rootNode);
                    log.info("添加根节点到链路：dmId={}, dmcCode={}", rootDm.getId(), rootDm.getDmcCode());
                }
            } else {
                // 中间节点和目标节点，从allNodes中查找或查询数据库
                boolean found = false;
                for (Map<String, Object> node : allNodes) {
                    if (pathId.equals(String.valueOf(node.get("dmId")))) {
                        chain.add(node);
                        log.info("添加到链路：dmId={}, dmcCode={}", node.get("dmId"), node.get("dmcCode"));
                        found = true;
                        break;
                    }
                }

                // 如果在allNodes中没找到，从数据库查询
                if (!found) {
                    IetmDataModule dm = this.getById(pathId);
                    if (dm != null) {
                        Map<String, Object> node = new HashMap<>();
                        node.put("dmId", dm.getId());
                        node.put("dmcCode", dm.getDmcCode());
                        node.put("techName", dm.getTechName());
                        node.put("infoName", dm.getInfoName());
                        // 查询引用关系信息，根据refType决定查询方向
                        if (i > 0) {
                            String prevId = pathIds[i - 1].trim();
                            QueryWrapper<IetmDmRef> qw = new QueryWrapper<>();
                            if ("out".equals(refType)) {
                                // 出引用：prevId引用了pathId
                                qw.eq("source_dm_id", prevId);
                                qw.eq("target_dm_id", pathId);
                            } else {
                                // 入引用：pathId引用了prevId
                                qw.eq("source_dm_id", pathId);
                                qw.eq("target_dm_id", prevId);
                            }
                            IetmDmRef ref = ietmDmRefMapper.selectOne(qw);
                            if (ref != null) {
                                node.put("refType", ref.getRefType());
                                node.put("refPosition", ref.getRefPosition());
                            }
                        }
                        chain.add(node);
                        log.info("从数据库查询并添加到链路：dmId={}, dmcCode={}", dm.getId(), dm.getDmcCode());
                    }
                }
            }
        }

        log.info("最终查询到引用链路径，共 {} 个节点", chain.size());
        return chain;
    }

    // ==================== 私有工具方法 ====================

    /**
     * 获取当前登录用户名
     * @return 用户名，如果未登录返回"system"
     */
    private String getCurrentUsername() {
        try {
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            if (loginUser != null) {
                return loginUser.getUsername();
            }
        } catch (Exception e) {
            log.warn("获取当前登录用户失败：{}", e.getMessage());
        }
        return "system";
    }

    /**
     * HTML转义工具方法
     * @param text 原始文本
     * @return 转义后的文本
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    /**
     * 从XML根元素提取DataModule信息
     * @param root XML根元素
     * @param projectId 项目ID
     * @return DataModule实体
     */
    private IetmDataModule extractDataModuleFromXml(Element root, String projectId) throws Exception {
        IetmDataModule dataModule = new IetmDataModule();
        dataModule.setProjectId(projectId);

        // 提取identAndStatusSection节点
        Element identAndStatusSection = root.element("identAndStatusSection");
        if (identAndStatusSection != null) {
            Element dmAddress = identAndStatusSection.element("dmAddress");
            if (dmAddress != null) {
                Element dmIdent = dmAddress.element("dmIdent");
                if (dmIdent != null) {
                    Element dmCode = dmIdent.element("dmCode");
                    if (dmCode != null) {
                        dataModule.setSchema(dmCode.attributeValue("modelIdentCode"));
                        // 【方案A】SNS 含 equipname 作首段，从 dmCode 全 8 属性重建（对标老系统）
                        dataModule.setSns(DmcUtils.composeSns(
                                dmCode.attributeValue("modelIdentCode"),
                                dmCode.attributeValue("systemDiffCode"),
                                dmCode.attributeValue("systemCode"),
                                dmCode.attributeValue("subSystemCode"),
                                dmCode.attributeValue("subSubSystemCode"),
                                dmCode.attributeValue("assyCode"),
                                dmCode.attributeValue("disassyCode"),
                                dmCode.attributeValue("disassyCodeVariant")));
                        dataModule.setInfoCode(dmCode.attributeValue("infoCode"));
                        dataModule.setInfoCodeVariant(dmCode.attributeValue("infoCodeVariant"));
                        dataModule.setIetmLocationCode(dmCode.attributeValue("itemLocationCode"));
                        dataModule.setLearnCode(dmCode.attributeValue("learnCode"));
                        dataModule.setLearnEventCode(dmCode.attributeValue("learnEventCode"));
                    }

                    Element language = dmIdent.element("language");
                    if (language != null) {
                        dataModule.setLanguageIsoCode(language.attributeValue("languageIsoCode"));
                        dataModule.setCountryIsoCode(language.attributeValue("countryIsoCode"));
                    }

                    Element issueInfo = dmIdent.element("issueInfo");
                    if (issueInfo != null) {
                        dataModule.setIssueNo(issueInfo.attributeValue("issueNumber"));
                        dataModule.setInWork(issueInfo.attributeValue("inWork"));
                    }
                }

                Element dmAddressItems = dmAddress.element("dmAddressItems");
                if (dmAddressItems != null) {
                    Element dmTitle = dmAddressItems.element("dmTitle");
                    if (dmTitle != null) {
                        Element techName = dmTitle.element("techName");
                        if (techName != null) {
                            dataModule.setTechName(techName.getTextTrim());
                        }
                        Element infoName = dmTitle.element("infoName");
                        if (infoName != null) {
                            dataModule.setInfoName(infoName.getTextTrim());
                        }
                    }
                }
            }

            Element dmStatus = identAndStatusSection.element("dmStatus");
            if (dmStatus != null) {
                Element responsiblePartnerCompany = dmStatus.element("responsiblePartnerCompany");
                if (responsiblePartnerCompany != null) {
                    Element enterpriseName = responsiblePartnerCompany.element("enterpriseName");
                    if (enterpriseName != null) {
                        dataModule.setOriginatorName(enterpriseName.getTextTrim());
                    }
                }

                Element originator = dmStatus.element("originator");
                if (originator != null) {
                    Element enterpriseName = originator.element("enterpriseName");
                    if (enterpriseName != null) {
                        dataModule.setRpcName(enterpriseName.getTextTrim());
                    }
                }

                Element security = dmStatus.element("security");
                if (security != null) {
                    dataModule.setSecurity(security.attributeValue("securityClassification"));
                }
            }
        }

        // 设置默认值
        dataModule.setSchema("J");
        dataModule.setIsLatest("1");
        dataModule.setStatus("1");

        return dataModule;
    }

    /**
     * 编辑DM属性（技术名称/信息名称）
     * 已签出（本人）：直接更新，inWork不变
     * 未签出：自动签出 + inWork+1 + 更新属性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> editProp(String id, DmEditPropVO vo, String currentUser) {
        // 1. 查询DM（JOIN流程视图，获取实时的workflowStep）
        IetmDataModule dm = baseMapper.selectByIdWithFlow(id);
        if (dm == null) {
            return Result.error("DM不存在");
        }

        // DEBUG: 打印当前 issueNo 和 inWork 值
        log.info("编辑DM属性 - 当前数据: id={}, issueNo={}, inWork={}, checkoutUser={}",
            id, dm.getIssueNo(), dm.getInWork(), dm.getCheckoutUser());

        // 2. 校验工作流状态（使用 workflowStatus 字段，来自 v_wf_instance 视图）
        // workflowStatus 含义：null/空=未启动，0=已结束，1=流转中，2=已撤销
        if (dm.getWorkflowStatus() == null || "0".equals(dm.getWorkflowStatus())) {
            return Result.error("还没有启动流程，不能编辑DM属性。");
        }
        if ("2".equals(dm.getWorkflowStatus())) {
            return Result.error("流程已撤销，不能编辑DM属性。");
        }

        // 3. 校验当前节点为"DM编写"（workflowStep 来自 v_wf_instance 视图动态计算）
        if (dm.getWorkflowStep() != null && !"DM编写".equals(dm.getWorkflowStep())) {
            return Result.error("流程状态不是DM编写状态，不能编辑DM属性。当前状态：" + dm.getWorkflowStep());
        }

        String checkoutUser = dm.getCheckoutUser();
        boolean isCheckedOut = StringUtils.isNotBlank(checkoutUser);

        // currentUser 防御性空值检查（正常 JeecgBoot 鉴权下不会为 null）
        if (StringUtils.isBlank(currentUser)) {
            return Result.error("无法获取当前用户信息，请重新登录");
        }

        if (isCheckedOut) {
            // 4a. 已签出，必须是本人
            if (!currentUser.equals(checkoutUser)) {
                return Result.error("该DM已由【" + checkoutUser + "】签出，不能编辑DM属性");
            }
            // 直接更新属性，inWork 不升级
            // 防御性处理：确保 issueNo 不为 0 或空
            if (StringUtils.isBlank(dm.getIssueNo()) || "0".equals(dm.getIssueNo())) {
                dm.setIssueNo("001");
                log.warn("DM issueNo 值异常({}), 已重置为 001, id={}", dm.getIssueNo(), id);
            }
            // 防御性处理：确保 inWork 不为空
            if (StringUtils.isBlank(dm.getInWork())) {
                dm.setInWork("00");
                log.warn("DM inWork 值为空, 已重置为 00, id={}", id);
            }

            dm.setTechName(vo.getTechName());
            dm.setInfoName(vo.getInfoName());
            // 对齐旧系统：编辑DM属性时更新版本日期
            dm.setIssueDate(new Date());
            dm.setUpdateBy(currentUser);

            // ✅ 修复：即使版本号未变，也需重新生成 DMC 以确保一致性
            // （防止 issueNo/inWork 被手动修复但 DMC 未同步的遗留数据）
            String newDmc = generateDmc(dm);
            dm.setDmcCode(newDmc);

            boolean updateSuccess = this.updateById(dm);
            if (!updateSuccess) {
                log.warn("编辑DM属性失败(乐观锁冲突)，id={}, user={}", id, currentUser);
                return Result.error("DM数据已被他人修改，请刷新后重试");
            }
            // ✅ 同步 techName/infoName 到 dm_content XML（关系列已更新，再同步 XML 内部节点）
            syncTitleToXml(id, dm);
            log.info("编辑DM属性(已签出)，id={}, user={}, dmcCode={}", id, currentUser, newDmc);
            return Result.OK("修改DM属性成功");
        } else {
            // 4b. 未签出：自动签出 + inWork+1 + 更新属性
            // 防御：inWork/issueNo 为空或异常值时给默认值，避免 parseInt 异常
            String safeInWork = StringUtils.isBlank(dm.getInWork()) ? "00" : dm.getInWork();
            String safeIssueNo = dm.getIssueNo();

            // 额外防御：如果 issueNo 为 null, 空, "0" 或其他无效值，设为 "001"
            if (StringUtils.isBlank(safeIssueNo) || "0".equals(safeIssueNo)) {
                safeIssueNo = "001";
                log.warn("DM issueNo 值异常({}), 已重置为 001, id={}", dm.getIssueNo(), id);
            }

            Map<String, String> versionMap = VersionCalculator.upgradeInwork(safeInWork, safeIssueNo);
            String newInWork = versionMap.get("newInwork");
            String newIssueNo = versionMap.get("newIssueno");

            log.info("准备更新DM - issueNo: {} -> {}, inWork: {} -> {}", safeIssueNo, newIssueNo, safeInWork, newInWork);

            dm.setInWork(newInWork);
            dm.setIssueNo(newIssueNo);
            dm.setCheckoutUser(currentUser);
            dm.setCheckoutTime(new Date());
            dm.setTechName(vo.getTechName());
            dm.setInfoName(vo.getInfoName());
            // 对齐旧系统：编辑DM属性（自动签出）时更新版本日期
            dm.setIssueDate(new Date());
            dm.setUpdateBy(currentUser);

            // ✅ 修复：版本号升级后必须重新生成 DMC，确保 DMC 与 issueNo/inWork 一致
            String newDmc = generateDmc(dm);
            dm.setDmcCode(newDmc);

            boolean updateSuccess = this.updateById(dm);
            if (!updateSuccess) {
                log.warn("编辑DM属性失败(乐观锁冲突)，id={}, user={}", id, currentUser);
                return Result.error("DM数据已被他人修改，请刷新后重试");
            }
            // ✅ 同步 techName/infoName 到 dm_content XML（关系列已更新，再同步 XML 内部节点）
            syncTitleToXml(id, dm);
            log.info("编辑DM属性(自动签出)，id={}, user={}, newInWork={}, dmcCode={}", id, currentUser, newInWork, newDmc);
            return Result.OK("已自动签出并修改DM属性成功");
        }
    }

    /**
     * 将 techName/infoName 同步写入 dm_content XML 内部节点。
     * <p>editProp 只更新关系列；dm_content 的 &lt;techName&gt;/&lt;infoName&gt; 需单独同步，否则
     * 编辑器/预览读 XML 时仍显示旧标题。</p>
     * <p>使用 selectContentById（BaseResultMap，含 dm_content CLOB）加载 XML，
     * 再通过 DmXmlHelper.syncDmIdentToXml 写回，最后单独 UPDATE dm_content。</p>
     */
    private void syncTitleToXml(String id, IetmDataModule dm) {
        IetmDataModule contentDm = baseMapper.selectContentById(id);
        if (contentDm == null || StringUtils.isBlank(contentDm.getDmContent())) {
            log.warn("syncTitleToXml: dm_content 为空，跳过 XML 同步，id={}", id);
            return;
        }
        try {
            String syncedXml = DmXmlHelper.syncDmIdentToXml(contentDm.getDmContent(), dm);
            this.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<IetmDataModule>()
                .eq(IetmDataModule::getId, id)
                .set(IetmDataModule::getDmContent, syncedXml));
            log.info("syncTitleToXml 成功：id={}, techName={}, infoName={}", id, dm.getTechName(), dm.getInfoName());
        } catch (Exception e) {
            log.error("syncTitleToXml 失败，id={}，XML 同步跳过: {}", id, e.getMessage(), e);
        }
    }

    // ==================== 复制新建DM相关方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> copyAndCreateDm(DmCopyVO vo) {
        log.info("开始复制新建DM，源DM ID={}, 目标节点ID={}", vo.getSourceDmId(), vo.getTargetCmNodeId());

        IetmDataModule sourceDm = validateAndGetSourceDm(vo.getSourceDmId());
        IetmDataModule newDm = createNewDmFromSource(sourceDm, vo);
        setTargetNodeInfo(newDm, vo);
        setVersionAndStatus(newDm, vo);
        setOriginatorAndRpc(newDm, vo);

        Result<?> dmcResult = generateAndValidateDmc(newDm);
        if (!dmcResult.isSuccess()) {
            return dmcResult;
        }

        // 校验 DMC 唯一性（防止重复）
        IetmDataModule conflict = ietmDataModuleMapper.selectByDmcForValidation(
            newDm.getSns(), newDm.getInfoCode(), newDm.getInfoCodeVariant(),
            newDm.getIetmLocationCode(), newDm.getLanguageIsoCode(), newDm.getCountryIsoCode(),
            null  // 新建时无需排除自身
        );
        if (conflict != null) {
            log.warn("复制新建失败：DMC冲突，dmcCode={}，冲突ID={}", newDm.getDmcCode(), conflict.getId());
            return Result.error("DMC 冲突：" + newDm.getDmcCode() + " 已存在（ID=" + conflict.getId() + "）");
        }

        // ✅ 同步数据库字段到 XML 内部的 dmIdent（dmCode/language/issueInfo）
        // 确保复制后的 XML 内容与新的数据库字段一致
        if (StringUtils.isNotBlank(newDm.getDmContent())) {
            String syncedXml = DmXmlHelper.syncDmIdentToXml(newDm.getDmContent(), newDm);
            newDm.setDmContent(syncedXml);
        }

        setAuditFieldsAndSave(newDm);

        log.info("复制新建DM成功，新DM ID={}, DMC={}", newDm.getId(), newDm.getDmcCode());
        return Result.OK("复制新建成功", newDm);
    }

    private IetmDataModule validateAndGetSourceDm(String sourceDmId) {
        IetmDataModule sourceDm = this.getById(sourceDmId);
        if (sourceDm == null) {
            throw new JeecgBootException("源DM不存在");
        }
        return sourceDm;
    }

    private IetmDataModule createNewDmFromSource(IetmDataModule sourceDm, DmCopyVO vo) {
        IetmDataModule newDm = new IetmDataModule();
        newDm.setProjectId(sourceDm.getProjectId());
        newDm.setProjectName(sourceDm.getProjectName());
        newDm.setInfoCode(getValueOrDefault(vo.getInfoCode(), sourceDm.getInfoCode()));
        newDm.setInfoCodeVariant(getValueOrDefault(vo.getInfoCodeVariant(), sourceDm.getInfoCodeVariant()));
        newDm.setIetmLocationCode(getValueOrDefault(vo.getIetmLocationCode(), sourceDm.getIetmLocationCode()));
        newDm.setLearnCode(getValueOrDefault(vo.getLearnCode(), sourceDm.getLearnCode()));
        newDm.setLearnEventCode(getValueOrDefault(vo.getLearnEventCode(), sourceDm.getLearnEventCode()));
        newDm.setInfoName(getValueOrDefault(vo.getInfoName(), sourceDm.getInfoName()));
        newDm.setDmType(getValueOrDefault(vo.getDmType(), sourceDm.getDmType()));
        newDm.setLanguageIsoCode(getValueOrDefault(vo.getLanguageIsoCode(), sourceDm.getLanguageIsoCode()));
        newDm.setCountryIsoCode(getValueOrDefault(vo.getCountryIsoCode(), sourceDm.getCountryIsoCode()));
        newDm.setSecurity(getValueOrDefault(vo.getSecurity(), sourceDm.getSecurity()));
        newDm.setSecurityClassification(sourceDm.getSecurityClassification());
        newDm.setDmContent(sourceDm.getDmContent());
        return newDm;
    }

    private void setTargetNodeInfo(IetmDataModule newDm, DmCopyVO vo) {
        newDm.setCmNodeId(vo.getTargetCmNodeId());
        newDm.setCmNodeName(vo.getTargetCmNodeName());
        newDm.setSns(calculateOrUseSns(vo));
        newDm.setTechName(extractOrUseTechName(vo));
    }

    private String calculateOrUseSns(DmCopyVO vo) {
        if (StringUtils.isNotBlank(vo.getSns())) {
            return vo.getSns();
        }
        String calculated = snsCalculateService.calculateSnsForDm(vo.getTargetCmNodeId());
        if (StringUtils.isBlank(calculated)) {
            throw new JeecgBootException("计算SNS失败，请检查构型节点配置");
        }
        return calculated;
    }

    private String extractOrUseTechName(DmCopyVO vo) {
        return StringUtils.isNotBlank(vo.getTechName())
            ? vo.getTechName()
            : extractTechName(vo.getTargetCmNodeName());
    }

    private void setVersionAndStatus(IetmDataModule newDm, DmCopyVO vo) {
        newDm.setIssueNo(getValueOrDefault(vo.getIssueNo(), INITIAL_ISSUE_NO));
        newDm.setInWork(getValueOrDefault(vo.getInWork(), "00"));
        // 对齐旧系统：复制新建DM时设置版本日期
        newDm.setIssueDate(new Date());
        newDm.setIsLatest("1");
        newDm.setStatus("1");
        newDm.setVersionType("0");
        // 设置issueType默认值（对齐addDm逻辑，S1000D标准）
        newDm.setIssueType("new");
        newDm.setCheckoutUser(null);
        newDm.setCheckoutTime(null);
    }

    private void setOriginatorAndRpc(IetmDataModule newDm, DmCopyVO vo) {
        if (StringUtils.isNotBlank(vo.getOriginator())) {
            newDm.setOriginator(vo.getOriginator());
            newDm.setOriginatorName(vo.getOriginatorName());
        } else {
            setOriginatorFromProject(newDm);
        }
        if (StringUtils.isNotBlank(vo.getRpc())) {
            newDm.setRpc(vo.getRpc());
            newDm.setRpcName(vo.getRpcName());
        } else {
            setRpcFromProject(newDm);
        }
    }

    // 仅负责生成 DMC 编码；唯一性校验由调用方（copyAndCreateDm）通过
    // selectByDmcForValidation 做完整校验，此处不再做残缺的浅层重复校验。
    private Result<?> generateAndValidateDmc(IetmDataModule newDm) {
        // 校验 SNS 必填（防止空 SNS 导致 DMC 双横线）
        if (oConvertUtils.isEmpty(newDm.getSns())) {
            return Result.error("SNS 编码不能为空，请检查构型节点路径是否完整（至少2层）");
        }

        String dmcCode = generateDmcCode(newDm);
        newDm.setDmcCode(dmcCode);
        return Result.OK();
    }

    private void setAuditFieldsAndSave(IetmDataModule newDm) {
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser != null) {
            newDm.setCreateBy(loginUser.getUsername());
            newDm.setUpdateBy(loginUser.getUsername());
        }
        newDm.setCreateTime(new Date());
        newDm.setUpdateTime(new Date());
        if (!this.save(newDm)) {
            throw new JeecgBootException("保存失败");
        }
    }

    /**
     * DMC查重（优化版）
     * 根据 VO 参数生成完整 DMC 编码并查询数据库
     * @param vo DMC查重请求对象
     * @return null=唯一，否则返回重复的DMC编码字符串
     */
    @Override
    public String checkDmcUnique(org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmcUniqueCheckVO vo) {
        // 1. 根据 VO 参数生成完整 DMC 编码（应用默认值）
        String dmcToCheck = buildDmcString(
            vo.getOriginator(),
            vo.getSns(),
            vo.getInfoCode(),
            vo.getInfoCodeVariant(),
            vo.getIetmLocationCode(),
            vo.getLearnCode(),
            vo.getLearnEventCode(),
            vo.getYearOfChange(),
            vo.getSeqNo(),
            vo.getIssueNo(),
            vo.getInWork(),
            vo.getLanguageIsoCode(),
            vo.getCountryIsoCode()
        );

        // 2. 查询数据库中是否存在该 DMC
        LambdaQueryWrapper<IetmDataModule> qw = new LambdaQueryWrapper<>();
        qw.eq(IetmDataModule::getDmcCode, dmcToCheck)
          .eq(IetmDataModule::getStatus, DM_STATUS_NORMAL);  // 仅查有效记录

        // 3. 编辑模式排除当前记录
        if (StringUtils.isNotBlank(vo.getExcludeId())) {
            qw.ne(IetmDataModule::getId, vo.getExcludeId());
        }

        // 4. 查询是否存在重复记录
        IetmDataModule existing = this.getOne(qw);
        return existing != null ? dmcToCheck : null;
    }

    /**
     * DMC查重（旧版本，保留向后兼容）
     * @deprecated 建议使用 checkDmcUnique(DmcUniqueCheckVO)
     */
    @Override
    @Deprecated
    public boolean checkDmcUnique(String sns, String infoCode, String infoCodeVariant,
                                  String ietmLocationCode, String excludeId) {
        QueryWrapper<IetmDataModule> qw = new QueryWrapper<>();
        qw.eq("sns", sns).eq("info_code", infoCode).eq("status", "1").eq("is_latest", "1");
        if (StringUtils.isNotBlank(infoCodeVariant)) {
            qw.eq("info_code_variant", infoCodeVariant);
        } else {
            qw.isNull("info_code_variant");
        }
        if (StringUtils.isNotBlank(ietmLocationCode)) {
            qw.eq("ietm_location_code", ietmLocationCode);
        } else {
            qw.isNull("ietm_location_code");
        }
        if (StringUtils.isNotBlank(excludeId)) {
            qw.ne("id", excludeId);
        }
        return this.count(qw) == 0;
    }

    /**
     * 构建 DMC 编码字符串（私有辅助方法）
     * 委托给 generateDmc() 保证格式完全一致，避免两套逻辑产生不同结果
     */
    private String buildDmcString(String originator, String sns, String infoCode,
                                  String infoCodeVariant, String ietmLocationCode,
                                  String learnCode, String learnEventCode,
                                  String yearOfChange, String seqNo,
                                  String issueNo, String inWork,
                                  String languageIsoCode, String countryIsoCode) {
        IetmDataModule dm = new IetmDataModule();
        dm.setOriginator(originator);
        dm.setSns(sns);
        dm.setInfoCode(infoCode);
        dm.setInfoCodeVariant(infoCodeVariant);
        dm.setIetmLocationCode(ietmLocationCode);
        dm.setLearnCode(learnCode);
        dm.setLearnEventCode(learnEventCode);
        dm.setYearOfChange(yearOfChange);
        dm.setSeqNo(seqNo);
        dm.setIssueNo(issueNo);
        dm.setInWork(inWork);
        dm.setLanguageIsoCode(languageIsoCode);
        dm.setCountryIsoCode(countryIsoCode);
        return generateDmc(dm);
    }

    /**
     * 创建禁用外部实体的安全 SAXReader，防止 XXE 攻击
     */
    private SAXReader createSafeSaxReader() {
        SAXReader reader = new SAXReader();
        try {
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception e) {
            log.warn("设置SAXReader安全特性失败，存在XXE风险: {}", e.getMessage());
        }
        return reader;
    }

    @Override
    public String extractTechName(String nodeNameWithCode) {
        if (StringUtils.isBlank(nodeNameWithCode)) {
            return "";
        }
        int spaceIdx = nodeNameWithCode.indexOf(' ');
        if (spaceIdx > 0 && spaceIdx < nodeNameWithCode.length() - 1) {
            return nodeNameWithCode.substring(spaceIdx + 1).trim();
        }
        return nodeNameWithCode;
    }

    @Override
    public void setOriginatorFromProject(IetmDataModule dm) {
        try {
            IetmProject project = projectService.getById(dm.getProjectId());
            if (project == null) {
                log.warn("项目不存在，使用默认单位，projectId={}", dm.getProjectId());
                setDefaultOriginator(dm);
                setDefaultRpc(dm);
                return;
            }
            QueryWrapper<IetmProjectCompany> qw = new QueryWrapper<>();
            qw.eq("pid", dm.getProjectId()).in("type", Arrays.asList(1, 2)).orderByAsc("type");
            List<IetmProjectCompany> companies = projectCompanyService.list(qw);

            String originator = null, originatorName = null, rpc = null, rpcName = null;
            if (companies != null) {
                for (IetmProjectCompany c : companies) {
                    if (c.getType() == 1 && originator == null) {
                        originator = c.getCompanyCode();
                        originatorName = c.getCompanyName();
                    } else if (c.getType() == 2 && rpc == null) {
                        rpc = c.getCompanyCode();
                        rpcName = c.getCompanyName();
                    }
                }
            }

            if (StringUtils.isNotBlank(originator)) {
                dm.setOriginator(originator);
                dm.setOriginatorName(originatorName);
            } else {
                setDefaultOriginator(dm);
            }
            if (StringUtils.isNotBlank(rpc)) {
                dm.setRpc(rpc);
                dm.setRpcName(rpcName);
            } else {
                setDefaultRpc(dm);
            }
        } catch (Exception e) {
            log.error("设置创作单位失败，使用默认值", e);
            setDefaultOriginator(dm);
            setDefaultRpc(dm);
        }
    }

    private void setRpcFromProject(IetmDataModule dm) {
        try {
            QueryWrapper<IetmProjectCompany> qw = new QueryWrapper<>();
            qw.eq("pid", dm.getProjectId()).eq("type", 2).last("FETCH FIRST 1 ROWS ONLY");
            IetmProjectCompany company = projectCompanyService.getOne(qw);
            if (company != null && StringUtils.isNotBlank(company.getCompanyCode())) {
                dm.setRpc(company.getCompanyCode());
                dm.setRpcName(company.getCompanyName());
            } else {
                setDefaultRpc(dm);
            }
        } catch (Exception e) {
            log.error("设置责任单位失败，使用默认值", e);
            setDefaultRpc(dm);
        }
    }

    private void setDefaultOriginator(IetmDataModule dm) {
        dm.setOriginator("DEFAULT");
        dm.setOriginatorName("默认创作单位");
    }

    private void setDefaultRpc(IetmDataModule dm) {
        dm.setRpc("DEFAULT");
        dm.setRpcName("默认责任单位");
    }

    private String getValueOrDefault(String value, String defaultValue) {
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }

    @Override
    public Result<?> copyDm(String dmId) {
        log.info("校验DM是否可复制，dmId={}", dmId);
        IetmDataModule dm = this.getById(dmId);
        if (dm == null) {
            return Result.error("DM不存在");
        }
        if (StringUtils.isNotBlank(dm.getCheckoutUser())) {
            return Result.error("DM已被签出，无法复制");
        }
        if (!"1".equals(dm.getStatus())) {
            return Result.error("DM状态无效，无法复制");
        }
        log.info("DM可以复制，dmId={}, dmc={}", dmId, dm.getDmcCode());
        return Result.OK("DM可以复制", dm);
    }

    @Override
    public Map<String, Object> validateXmlContent(String xmlContent) {
        Map<String, Object> result = new HashMap<>();

        if (oConvertUtils.isEmpty(xmlContent)) {
            result.put("valid", false);
            result.put("message", "XML内容为空");
            return result;
        }

        // 简单的XML格式验证
        boolean valid = validateXmlFormat(xmlContent);
        result.put("valid", valid);
        result.put("message", valid ? "XML格式正确" : "XML格式错误，请检查标签是否闭合");

        return result;
    }

    /**
     * 简单的XML格式验证
     */
    private boolean validateXmlFormat(String xmlContent) {
        try {
            // 检查基本的XML标签匹配
            if (!xmlContent.trim().startsWith("<") || !xmlContent.trim().endsWith(">")) {
                return false;
            }
            // 简单检查：< 和 > 的数量应该匹配
            long openTags = xmlContent.chars().filter(ch -> ch == '<').count();
            long closeTags = xmlContent.chars().filter(ch -> ch == '>').count();
            return openTags == closeTags && openTags > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验 DMC 输入字段（防止 SQL 注入和路径遍历）
     *
     * @param dm 数据模块对象
     * @throws IllegalArgumentException 输入包含非法字符时
     */
    private void validateDmcInput(IetmDataModule dm) {
        if (dm == null) {
            return;
        }

        // 校验多字符字段（只允许大写字母、数字、短横线）
        validateField("SNS", dm.getSns(), DMC_SAFE_ALPHANUMERIC);
        validateField("InfoCode", dm.getInfoCode(), DMC_SAFE_ALPHANUMERIC);
        validateField("Originator", dm.getOriginator(), DMC_SAFE_ALPHANUMERIC);

        // 校验单字符字段（只允许大写字母或数字）
        validateField("InfoCodeVariant", dm.getInfoCodeVariant(), DMC_SAFE_SINGLE_CHAR);
        validateField("LearnEventCode", dm.getLearnEventCode(), DMC_SAFE_SINGLE_CHAR);

        // 校验语言/国家代码（接受小写，与VO Pattern保持一致）
        if (dm.getLanguageIsoCode() != null && !dm.getLanguageIsoCode().matches("^[a-z]{2,3}$")) {
            throw new IllegalArgumentException("LanguageIsoCode 必须为2-3位小写字母");
        }
        if (dm.getCountryIsoCode() != null && !dm.getCountryIsoCode().matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException("CountryIsoCode 必须为2位大写字母");
        }
    }

    /**
     * 校验单个字段
     */
    private void validateField(String fieldName, String value, Pattern pattern) {
        if (value != null && !pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " 包含非法字符，只允许大写字母、数字和短横线");
        }
    }

    /**
     * 批量修复 DMC 与版本号不一致的数据
     * <p>
     * 问题根因：editProp 方法在升级版本号时未重新生成 DMC，导致：
     * - 数据库字段：issue_no='001', in_work='02'
     * - DMC 字段：dmc_code='DMC-..._001-01_zh-CN'（旧版本）
     * <p>
     * 修复方案：根据当前 issue_no/in_work 重新生成 DMC 并更新数据库
     *
     * @param limit 最多修复的记录数（防止一次处理过多数据）
     * @return 修复结果统计
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> fixInconsistentDmc(int limit) {
        log.info("开始批量修复 DMC 与版本号不一致的数据，limit={}", limit);

        Map<String, Object> result = new HashMap<>();
        int checkedCount = 0;
        int fixedCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            // 查询所有有效记录（使用 MyBatis-Plus 标准分页，兼容所有数据库）
            Page<IetmDataModule> pageQuery = new Page<>(1, limit);
            Page<IetmDataModule> pageResult = this.page(pageQuery, new LambdaQueryWrapper<IetmDataModule>()
                    .eq(IetmDataModule::getStatus, "1"));
            List<IetmDataModule> dmList = pageResult.getRecords();

            for (IetmDataModule dm : dmList) {
                checkedCount++;

                try {
                    // 根据当前字段重新生成 DMC
                    String expectedDmc = generateDmc(dm);
                    String currentDmc = dm.getDmcCode();

                    // 检查是否不一致
                    if (!expectedDmc.equals(currentDmc)) {
                        log.warn("发现不一致 DMC，id={}, current={}, expected={}",
                                dm.getId(), currentDmc, expectedDmc);

                        // 更新 DMC
                        boolean success = this.update(new LambdaUpdateWrapper<IetmDataModule>()
                                .eq(IetmDataModule::getId, dm.getId())
                                .set(IetmDataModule::getDmcCode, expectedDmc));

                        if (success) {
                            fixedCount++;
                            log.info("修复成功 id={}, {} -> {}", dm.getId(), currentDmc, expectedDmc);
                        } else {
                            errorCount++;
                            String error = String.format("id=%s 更新失败", dm.getId());
                            errors.add(error);
                            log.error(error);
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                    String error = String.format("id=%s 处理异常: %s", dm.getId(), e.getMessage());
                    errors.add(error);
                    log.error("修复 DMC 失败", e);
                }
            }

            result.put("checkedCount", checkedCount);
            result.put("fixedCount", fixedCount);
            result.put("errorCount", errorCount);
            result.put("errors", errors);

            log.info("批量修复完成，检查={}, 修复={}, 错误={}", checkedCount, fixedCount, errorCount);
            return result;

        } catch (Exception e) {
            log.error("批量修复 DMC 发生异常", e);
            result.put("error", e.getMessage());
            return result;
        }
    }
}
