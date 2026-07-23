package org.jeecg.modules.ietm.ietmdatamodulemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmCommentMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmRefMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.VersionCalculator;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmEditPropVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.DmProjectInfoVO;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProject;
import org.jeecg.modules.ietm.projectmanagement.service.IIetmProjectService;
import org.jeecg.modules.ietm.common.service.ISnsCalculateService;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class IetmDataModuleServiceImpl extends ServiceImpl<IetmDataModuleMapper, IetmDataModule> 
        implements IIetmDataModuleService {

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

        // 3. 计算SNS编码（使用公共服务）
        String sns = snsCalculateService.calculateSns(cmNodeId);

        // 4. 组装返回对象
        DmProjectInfoVO vo = new DmProjectInfoVO();
        vo.setProjectId(project.getId());
        vo.setSecurity(project.getSecurity());
        vo.setSns(sns);
        vo.setCodeRule(project.getCodeRule());

        // 5. 语言和国家
        vo.setLanguageCode(project.getLanuageCode());
        vo.setCountryCode(project.getCountryCode());

        // 6. 构型节点技术名称
        vo.setTechName(config.getTitle());

        // 7. 项目的创作单位和责任单位（如果项目有配置的话）
        // 注：这里只设置编码，前端会从项目单位表加载名称列表
        // 如果需要默认值，前端会根据projectId过滤并自动选择
        // vo.setOriginator(project.getOriginator());  // 项目表可能没有这个字段
        // vo.setRpc(project.getRpc());  // 项目表可能没有这个字段

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDm(IetmDataModule dataModule) {
        log.info("保存数据模块，项目ID：{}", dataModule.getProjectId());
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
        return this.save(dataModule);
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
        return this.updateById(dataModule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDm(String id) {
        log.info("删除数据模块，ID：{}", id);
        IetmDataModule existDm = this.getById(id);
        if (existDm == null) {
            throw new JeecgBootException("数据模块不存在");
        }

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
                && !"ended".equals(existDm.getWorkflowStatus())) {
            throw new JeecgBootException("DM正在流程中（当前状态：" + existDm.getWorkflowStatus() + "），不允许删除");
        }

        // 4. 引用检查：查询是否被其他DM引用（使用正确字段名 target_dm_id）
        QueryWrapper<IetmDmRef> refWrapper = new QueryWrapper<>();
        refWrapper.eq("target_dm_id", id);
        int referencedCount = ietmDmRefMapper.selectCount(refWrapper).intValue();
        if (referencedCount > 0) {
            throw new JeecgBootException("此DM已被" + referencedCount + "个其他DM引用，请先解除引用关系后再删除");
        }

        // 5. 判断是物理删除还是逻辑删除
        String issueNo = existDm.getIssueNo() != null ? existDm.getIssueNo() : "001";
        String inwork = existDm.getInWork() != null ? existDm.getInWork() : "00";

        // 初始版本（001-00）执行物理删除
        if ("001".equals(issueNo) && "00".equals(inwork)) {
            log.info("初始版本，执行物理删除，ID：{}，DMC：{}", id, existDm.getDmcCode());

            // 5.1 删除关联资源（ietm_dm_comment）
            ietmDmCommentMapper.deleteByDmId(id);

            // 5.2 删除该DM作为引用方的引用关系记录（ietm_dm_reference 中 source_dm_id = id）
            QueryWrapper<IetmDmRef> sourceRefWrapper = new QueryWrapper<>();
            sourceRefWrapper.eq("source_dm_id", id);
            ietmDmRefMapper.delete(sourceRefWrapper);

            // 5.3 物理删除主记录
            log.info("物理删除完成，ID：{}", id);
            return this.removeById(id);
        } else {
            // 非初始版本执行逻辑删除（标记 status=0）
            log.info("非初始版本，执行逻辑删除，ID：{}，版本：{}-{}", id, issueNo, inwork);
            existDm.setStatus("0");
            log.info("逻辑删除完成，ID：{}", id);
            return this.updateById(existDm);
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
        IetmDataModule dm = this.getById(id);
        if (dm == null) {
            throw new JeecgBootException("数据模块不存在");
        }
        if (oConvertUtils.isNotEmpty(dm.getCheckoutUser())) {
            throw new JeecgBootException("数据模块已被用户 " + dm.getCheckoutUser() + " 签出");
        }
        dm.setCheckoutUser(username);
        dm.setCheckoutTime(new Date());
        return this.updateById(dm);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelCheckOut(String id, String username) {
        log.info("取消签出数据模块，ID：{}，用户：{}", id, username);
        IetmDataModule dm = this.getById(id);
        if (dm == null) {
            throw new JeecgBootException("数据模块不存在");
        }
        if (oConvertUtils.isEmpty(dm.getCheckoutUser())) {
            throw new JeecgBootException("数据模块未被签出");
        }
        if (!username.equals(dm.getCheckoutUser())) {
            throw new JeecgBootException("只能取消自己签出的数据模块");
        }
        dm.setCheckoutUser(null);
        dm.setCheckoutTime(null);
        return this.updateById(dm);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkIn(String id, String username, String comment) {
        log.info("签入数据模块，ID：{}，用户：{}", id, username);
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

        // 版本号升级（inwork）
        String currentInwork = dm.getInWork() != null ? dm.getInWork() : "00";
        String currentIssueno = dm.getIssueNo() != null ? dm.getIssueNo() : "001";

        // 检查inwork边界
        int inwork = Integer.parseInt(currentInwork);
        if (inwork >= 99) {
            throw new JeecgBootException("在编版本已达上限99，请先发布后再签入");
        }

        Map<String, String> newVersion = calculateVersion(currentInwork, currentIssueno, "inwork");
        dm.setInWork(newVersion.get("newInwork"));

        // 清除签出状态
        dm.setCheckoutUser(null);
        dm.setCheckoutTime(null);
        dm.setCheckinTime(new Date());

        // 重新生成DMC（因为inwork变化了）
        String newDmc = generateDmc(dm);
        // dm.setDmCode(newDmc);  // Entity没有该字段，DMC由generateDmc计算得出，不存储

        return this.updateById(dm);
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
            if (!"ended".equals(dm.getWorkflowStatus())) {
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

        // 6. 保存已发布的DMC（Entity没有publishedDmc和dmCode字段，跳过）
        // dm.setPublishedDmc(dm.getDmCode());

        // 7. 更新签发日期
        dm.setIssueDate(new Date());

        return this.updateById(dm);
    }

    @Override
    public Map<String, Object> batchCheckOut(List<String> ids, String username) {
        int success = 0;
        int fail = 0;
        List<String> failMessages = new ArrayList<>();
        for (String id : ids) {
            try {
                checkOut(id, username);
                success++;
            } catch (Exception e) {
                fail++;
                failMessages.add(id + ": " + e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("failMessages", failMessages);
        return result;
    }

    @Override
    public Map<String, Object> batchCheckIn(List<String> ids, String username, String comment) {
        int success = 0;
        int fail = 0;
        List<String> failMessages = new ArrayList<>();
        for (String id : ids) {
            try {
                checkIn(id, username, comment);
                success++;
            } catch (Exception e) {
                fail++;
                failMessages.add(id + ": " + e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("failMessages", failMessages);
        return result;
    }

    @Override
    public Map<String, Object> batchDelete(List<String> ids) {
        int success = 0;
        int fail = 0;
        List<String> failMessages = new ArrayList<>();
        for (String id : ids) {
            // 预查询 DM，用 DMC 编码替代原始 ID 显示在错误信息中，方便定位问题
            IetmDataModule dm = this.getById(id);
            String label = (dm != null && oConvertUtils.isNotEmpty(dm.getDmcCode()))
                    ? dm.getDmcCode() : id;
            try {
                deleteDm(id);
                success++;
            } catch (Exception e) {
                fail++;
                failMessages.add(label + ": " + e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("failMessages", failMessages);
        return result;
    }

    @Override
    public List<IetmDataModule> queryByProjectId(String projectId) {
        return ietmDataModuleMapper.selectByProjectId(projectId);
    }

    @Override
    public List<IetmDataModule> queryByCmNodeId(String cmNodeId, boolean includeChildren) {
        return ietmDataModuleMapper.selectByCmNodeId(cmNodeId, includeChildren ? "1" : "0");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReferenceCount(String dmId) {
        // 1. 统计作为被引用方的引用数量（其他DM引用了此DM）
        // TODO: 需要在IetmDmRefMapper中添加countByReferencedDmId方法
        // Integer beReferencedCount = ietmDmRefMapper.countByReferencedDmId(dmId);

        // 2. 统计作为引用方的引用数量（此DM引用了其他DM）
        // TODO: 需要在IetmDmRefMapper中添加countByDmId方法
        // Integer referenceCount = ietmDmRefMapper.countByDmId(dmId);

        // 3. 手动统计引用数量（临时方案）
        QueryWrapper<org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmRef> refWrapper = new QueryWrapper<>();
        refWrapper.eq("target_dm_id", dmId);
        int beReferencedCount = ietmDmRefMapper.selectCount(refWrapper).intValue();

        refWrapper.clear();
        refWrapper.eq("source_dm_id", dmId);
        int referenceCount = ietmDmRefMapper.selectCount(refWrapper).intValue();

        log.info("更新引用计数成功，DM ID：{}，被引用数：{}，引用数：{}",
                dmId, beReferencedCount, referenceCount);

        // 注意：IetmDataModule没有beReferencedCount/referenceCount字段
        // 如需保存，需要在Entity中添加这些字段
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
            SAXReader reader = new SAXReader();
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
                            dataModule.setSns(getAttributeValue(dmCode, "systemDiffCode"));
                            dataModule.setInfoCode(getAttributeValue(dmCode, "infoCode"));
                            dataModule.setInfoCodeVariant(getAttributeValue(dmCode, "infoCodeVariant"));
                            dataModule.setIetmLocationCode(getAttributeValue(dmCode, "itemLocationCode"));
                            dataModule.setLearnCode(getAttributeValue(dmCode, "learnCode"));
                            dataModule.setLearnCodeEventCode(getAttributeValue(dmCode, "learnEventCode"));
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

            // 7. 保存到数据库
            boolean saveSuccess = this.save(dataModule);

            if (saveSuccess) {
                result.put("success", true);
                result.put("message", "XML导入成功");
                result.put("dmId", dataModule.getId());
                result.put("dmcCode", generateDmcCode(dataModule));
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
    private String generateDmcCode(IetmDataModule dm) {
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

        // DMC码
        xml.append("        <dmCode");
        xml.append(" modelIdentCode=\"").append(nvl(dataModule.getSchema(), "J")).append("\"");
        xml.append(" systemDiffCode=\"").append(nvl(dataModule.getSns(), "")).append("\"");
        xml.append(" systemCode=\"").append(extractSystemCode(dataModule.getSns())).append("\"");
        xml.append(" subSystemCode=\"").append(extractSubSystemCode(dataModule.getSns())).append("\"");
        xml.append(" subSubSystemCode=\"").append(extractSubSubSystemCode(dataModule.getSns())).append("\"");
        xml.append(" assyCode=\"").append(extractAssyCode(dataModule.getSns())).append("\"");
        xml.append(" disassyCode=\"").append(extractDisassyCode(dataModule.getSns())).append("\"");
        xml.append(" disassyCodeVariant=\"").append(extractDisassyCodeVariant(dataModule.getSns())).append("\"");
        xml.append(" infoCode=\"").append(nvl(dataModule.getInfoCode(), "")).append("\"");
        xml.append(" infoCodeVariant=\"").append(nvl(dataModule.getInfoCodeVariant(), "")).append("\"");
        xml.append(" itemLocationCode=\"").append(nvl(dataModule.getIetmLocationCode(), "A")).append("\"");
        if (!oConvertUtils.isEmpty(dataModule.getLearnCode())) {
            xml.append(" learnCode=\"").append(dataModule.getLearnCode()).append("\"");
        }
        if (!oConvertUtils.isEmpty(dataModule.getLearnCodeEventCode())) {
            xml.append(" learnEventCode=\"").append(dataModule.getLearnCodeEventCode()).append("\"");
        }
        xml.append("/>\n");

        // 语言信息
        xml.append("        <language languageIsoCode=\"").append(nvl(dataModule.getLanguageIsoCode(), "ZH")).append("\"");
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
     * 使用标准S1000D格式，与generateDmc()保持一致
     */
    private String buildDmcCode(IetmDataModule dm) {
        StringBuilder dmc = new StringBuilder();
        dmc.append("DMC-");

        // 第1段：Schema
        dmc.append(nvl(dm.getSchema(), "J")).append("-");

        // 第2段：SNS
        dmc.append(nvl(dm.getSns(), "")).append("-");

        // 第3段：InfoCode + InfoCodeVariant
        dmc.append(nvl(dm.getInfoCode(), "")).append(nvl(dm.getInfoCodeVariant(), "")).append("-");

        // 第4段：IetmLocationCode + LearnCode + LearnCodeEventCode
        dmc.append(nvl(dm.getIetmLocationCode(), ""));
        if (!oConvertUtils.isEmpty(dm.getLearnCode())) {
            dmc.append(dm.getLearnCode()).append(nvl(dm.getLearnCodeEventCode(), ""));
        }
        dmc.append("-");

        // 第5段：YearOfChange + SeqNo
        dmc.append(nvl(dm.getYearOfChange(), "")).append(nvl(dm.getSeqNo(), ""));
        dmc.append("-");

        // 第6段：Originator
        dmc.append(nvl(dm.getOriginator(), "")).append("-");

        // 第7段：IssueNo
        dmc.append(nvl(dm.getIssueNo(), "001")).append("-");

        // 第8段：InWork + LanguageIsoCode
        dmc.append(nvl(dm.getInWork(), "00"));
        dmc.append(nvl(dm.getLanguageIsoCode(), "ZH")).append("-");

        // 第9段：CountryIsoCode
        dmc.append(nvl(dm.getCountryIsoCode(), "CN"));

        return dmc.toString();
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

    private String extractSystemCode(String sns) {
        return sns != null && sns.length() >= 2 ? sns.substring(0, 2) : "00";
    }

    private String extractSubSystemCode(String sns) {
        return sns != null && sns.length() >= 3 ? sns.substring(2, 3) : "0";
    }

    private String extractSubSubSystemCode(String sns) {
        return sns != null && sns.length() >= 4 ? sns.substring(3, 4) : "0";
    }

    private String extractAssyCode(String sns) {
        return sns != null && sns.length() >= 6 ? sns.substring(4, 6) : "00";
    }

    private String extractDisassyCode(String sns) {
        return sns != null && sns.length() >= 8 ? sns.substring(6, 8) : "00";
    }

    private String extractDisassyCodeVariant(String sns) {
        return sns != null && sns.length() >= 9 ? sns.substring(8, 9) : "A";
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
    @Transactional(rollbackFor = Exception.class)
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
            InputStream inputStream = file.getInputStream();
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
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
                    SAXReader reader = new SAXReader();
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

            zipInputStream.close();

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
            SAXReader reader = new SAXReader();
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
        // 生成DMC编码（S1000D标准11段格式）：
        // DMC-{schema}-{sns}-{infocode}{variant}-{location}{learn}{event}-{yearOfChange}{seqNo}-{originator}-{issueno}-{inwork}{lang}-{country}
        StringBuilder dmc = new StringBuilder("DMC-");

        // 第1段：Schema（模式代码，默认J）
        dmc.append(oConvertUtils.getString(dataModule.getSchema(), "J")).append("-");

        // 第2段：SNS（系统编号）
        dmc.append(dataModule.getSns()).append("-");

        // 第3段：InfoCode + InfoCodeVariant（信息码+变体）
        dmc.append(dataModule.getInfoCode());
        if (oConvertUtils.isNotEmpty(dataModule.getInfoCodeVariant())) {
            dmc.append(dataModule.getInfoCodeVariant());
        }
        dmc.append("-");

        // 第4段：IetmLocationCode + LearnCode + LearnCodeEventCode（位置码+学习码+学习事件码）
        if (oConvertUtils.isNotEmpty(dataModule.getIetmLocationCode())) {
            dmc.append(dataModule.getIetmLocationCode());
        }
        if (oConvertUtils.isNotEmpty(dataModule.getLearnCode())) {
            dmc.append(dataModule.getLearnCode());
        }
        if (oConvertUtils.isNotEmpty(dataModule.getLearnCodeEventCode())) {
            dmc.append(dataModule.getLearnCodeEventCode());
        }
        dmc.append("-");

        // 第5段：YearOfChange + SeqNo（变更年代码+顺序码）
        if (oConvertUtils.isNotEmpty(dataModule.getYearOfChange())) {
            dmc.append(dataModule.getYearOfChange());
        }
        if (oConvertUtils.isNotEmpty(dataModule.getSeqNo())) {
            dmc.append(dataModule.getSeqNo());
        }
        dmc.append("-");

        // 第6段：Originator（发行方代码）
        dmc.append(dataModule.getOriginator()).append("-");

        // 第7段：IssueNo（发行编号，默认001）
        dmc.append(oConvertUtils.getString(dataModule.getIssueNo(), "001")).append("-");

        // 第8段：InWork + LanguageIsoCode（在编编号+语言代码）
        dmc.append(oConvertUtils.getString(dataModule.getInWork(), "00"));
        dmc.append(oConvertUtils.getString(dataModule.getLanguageIsoCode(), "ZH")).append("-");

        // 第9段：CountryIsoCode（国家代码，默认CN）
        dmc.append(oConvertUtils.getString(dataModule.getCountryIsoCode(), "CN"));

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
        newDm.setLearnCodeEventCode(sourceDm.getLearnCodeEventCode());
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

        // 如果是type=0且mainId为null，将ID设置为mainId
        // mainId和versionPath字段不存在，已注释
        // if (copyType == null || copyType == 0) {
        //     newDm.setMainId(newDm.getId());
        //     newDm.setVersionPath(newDm.getId());
        //     this.updateById(newDm);
        // }

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
            // String instanceId = instance.getId();

            // 2. 更新DM状态为"审批中"
            dm.setStatus("2"); // 假设2表示审批中
            this.updateById(dm);

            log.info("启动工作流成功，DM ID：{}，流程Key：{}，用户：{}", id, processKey, username);

            // 暂时返回模拟的实例ID，等待集成工作流引擎
            String mockInstanceId = "workflow-" + id + "-" + System.currentTimeMillis();
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

            // 2. 更新DM状态
            if (approved) {
                dm.setStatus("3"); // 假设3表示已批准
            } else {
                dm.setStatus("4"); // 假设4表示已拒绝
            }
            this.updateById(dm);

            log.info("完成工作流任务成功，DM ID：{}，任务ID：{}，是否通过：{}", id, taskId, approved);
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
                SAXReader reader = new SAXReader();
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

        // 全文搜索：DMC、技术名称、信息名称
        queryWrapper.and(wrapper -> wrapper
                .like(IetmDataModule::getTechName, keyword)
                .or().like(IetmDataModule::getInfoName, keyword)
        );

        return this.list(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> queryDmResources(String dmId) {
        return ietmDmCommentMapper.selectByDmId(dmId);
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

        // 更新DM的资源数量
        if (result > 0) {
            String dmId = dmComment.getDmId();
            Integer count = ietmDmCommentMapper.countByDmId(dmId);
            IetmDataModule dm = this.getById(dmId);
            if (dm != null) {
            // dm.setResourceCount(count); // 字段不存在，已注释
            }
        }

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
        int result = ietmDmCommentMapper.deleteById(id);

        // 更新DM的资源数量
        if (result > 0) {
            String dmId = dmComment.getDmId();
            Integer count = ietmDmCommentMapper.countByDmId(dmId);
            IetmDataModule dm = this.getById(dmId);
            if (dm != null) {
            // dm.setResourceCount(count); // 字段不存在，已注释
            }
        }

        return result > 0;
    }

    @Override
    public List<IetmDataModule> queryHistoryVersions(String sns, String infoCode, String infoCodeVariant) {
        log.info("查询历史版本，sns：{}，infoCode：{}，infoCodeVariant：{}", sns, infoCode, infoCodeVariant);
        LambdaQueryWrapper<IetmDataModule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(IetmDataModule::getSns, sns);
        queryWrapper.eq(IetmDataModule::getInfoCode, infoCode);
        if (oConvertUtils.isNotEmpty(infoCodeVariant)) {
            queryWrapper.eq(IetmDataModule::getInfoCodeVariant, infoCodeVariant);
        }
        queryWrapper.orderByDesc(IetmDataModule::getIssueNo, IetmDataModule::getInWork);
        return this.list(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> queryReferenceTree(String dmId, String refType) {
        log.info("查询引用关系树，DM ID：{}，引用类型：{}", dmId, refType);

        if ("out".equals(refType)) {
            // 查询出引用（当前DM引用了哪些DM）
            return ietmDmRefMapper.selectOutReferences(dmId);
        } else if ("in".equals(refType)) {
            // 查询入引用（哪些DM引用了当前DM）
            return ietmDmRefMapper.selectInReferences(dmId);
        } else {
            return new ArrayList<>();
        }
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
                        dataModule.setSns(dmCode.attributeValue("systemCode"));
                        dataModule.setInfoCode(dmCode.attributeValue("infoCode"));
                        dataModule.setInfoCodeVariant(dmCode.attributeValue("infoCodeVariant"));
                        dataModule.setIetmLocationCode(dmCode.attributeValue("disassyCode"));
                        dataModule.setLearnCode(dmCode.attributeValue("learnCode"));
                        dataModule.setLearnCodeEventCode(dmCode.attributeValue("learnEventCode"));
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
        // 1. 查询DM
        IetmDataModule dm = this.getById(id);
        if (dm == null) {
            return Result.error("DM不存在");
        }

        // 2. 校验工作流已启动
        if (StringUtils.isBlank(dm.getWorkflowInstanceId())) {
            return Result.error("还没有启动流程，不能编辑DM属性");
        }

        // 3. 校验当前节点为"DM编写"
        if (!"DM编写".equals(dm.getWorkflowStep())) {
            return Result.error("流程状态不是DM编写状态，不能编辑DM属性");
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
            dm.setTechName(vo.getTechName());
            dm.setInfoName(vo.getInfoName());
            dm.setIssueDate(new Date());
            dm.setUpdateBy(currentUser);
            this.updateById(dm);
            log.info("编辑DM属性(已签出)，id={}, user={}", id, currentUser);
            return Result.OK("修改DM属性成功");
        } else {
            // 4b. 未签出：自动签出 + inWork+1 + 更新属性
            // 防御：inWork/issueNo 为空时给默认值，避免 parseInt 异常
            String safeInWork = StringUtils.isBlank(dm.getInWork()) ? "00" : dm.getInWork();
            String safeIssueNo = StringUtils.isBlank(dm.getIssueNo()) ? "001" : dm.getIssueNo();
            Map<String, String> versionMap = VersionCalculator.upgradeInwork(safeInWork, safeIssueNo);
            dm.setInWork(versionMap.get("newInwork"));
            dm.setIssueNo(versionMap.get("newIssueno"));
            dm.setCheckoutUser(currentUser);
            dm.setCheckoutTime(new Date());
            dm.setTechName(vo.getTechName());
            dm.setInfoName(vo.getInfoName());
            dm.setIssueDate(new Date());
            dm.setUpdateBy(currentUser);
            this.updateById(dm);
            log.info("编辑DM属性(自动签出)，id={}, user={}, newInWork={}", id, currentUser, dm.getInWork());
            return Result.OK("已自动签出并修改DM属性成功");
        }
    }
}
