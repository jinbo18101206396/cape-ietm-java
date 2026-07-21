package org.jeecg.modules.ietm.icnmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.icnmanage.vo.IcnProjectInfoVO;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.service.IIetmAttachmentService;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProject;
import org.jeecg.modules.ietm.projectmanagement.service.IIetmProjectService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import utils.DESUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * @Description: 项目管理-项目实体管理ServiceImpl
 * @Author: jeecg-boot
 * @Date: 2026-07-19
 * @Version: V2.0
 */
@Slf4j
@Service
public class IetmIcnManageServiceImpl extends ServiceImpl<IetmIcnManageMapper, IetmIcnManage>
        implements IIetmIcnManageService {

    @Autowired
    private IIetmAttachmentService attachmentService;

    @Autowired
    private IIetmProjectConfigurationManagementService configurationService;

    @Autowired
    private IIetmProjectService projectService;

    @Value("${accessFile.location}")
    private String fileStorageLocation;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addWithFiles(IetmIcnManage icnManage, MultipartFile[] files) throws IOException {
        // 1. 生成ICN主键
        String icnId = String.valueOf(IdWorker.getId());
        icnManage.setId(icnId);

        // 2. 生成唯一识别码（如果为空）
        if (StringUtils.isBlank(icnManage.getUniqueId())) {
            String uniqueId = getNextUniqueId(icnManage.getCmnodeId());
            icnManage.setUniqueId(uniqueId);
            log.info("自动生成uniqueId: {}", uniqueId);
        }

        // 3. 生成ICN完整编码
        String icnCode = generateIcnCode(icnManage);
        icnManage.setIcn(icnCode);

        // 4. 保存ICN记录
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        icnManage.setCreateBy(loginUser.getUsername());
        icnManage.setCreateTime(new Date());
        this.save(icnManage);

        // 4. 保存文件附件
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                saveAttachment(icnId, file, "实体文件", icnManage.getSecurity(), loginUser.getUsername());
            }
        }

        log.info("新增ICN成功，ICN编码：{}", icnCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadRelatedFiles(String icnId, MultipartFile[] files) throws IOException {
        IetmIcnManage icnManage = this.getById(icnId);
        if (icnManage == null) {
            throw new JeecgBootException("ICN记录不存在");
        }

        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

        // 删除旧的相关文件
        LambdaQueryWrapper<IetmAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IetmAttachment::getPid, icnId)
               .eq(IetmAttachment::getFileType, "相关文件");
        List<IetmAttachment> oldAttachments = attachmentService.list(wrapper);
        if (!oldAttachments.isEmpty()) {
            for (IetmAttachment att : oldAttachments) {
                deletePhysicalFile(att.getFileKey());
            }
            attachmentService.remove(wrapper);
        }

        // 保存新的相关文件
        for (MultipartFile file : files) {
            saveAttachment(icnId, file, "相关文件", icnManage.getSecurity(), loginUser.getUsername());
        }

        // 注意：不要修改uniqueId，它应该保持原值
        // 只更新修改时间和修改人
        icnManage.setUpdateBy(loginUser.getUsername());
        icnManage.setUpdateTime(new Date());
        this.updateById(icnManage);

        log.info("相关文件上传成功，ICN ID：{}", icnId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadDiffFiles(String originalIcnId, MultipartFile[] files,
                                String newUniqueId, String newVariantCode) throws IOException {
        // 1. 复制原ICN信息
        IetmIcnManage originalIcn = this.getById(originalIcnId);
        if (originalIcn == null) {
            throw new JeecgBootException("原ICN记录不存在");
        }

        IetmIcnManage newIcn = new IetmIcnManage();
        BeanUtils.copyProperties(originalIcn, newIcn);

        // 2. 生成新ICN
        String newIcnId = String.valueOf(IdWorker.getId());
        newIcn.setId(newIcnId);

        // 如果newUniqueId为空，则使用原ICN的uniqueId；如果原ICN的uniqueId也为空，则自动生成
        if (StringUtils.isBlank(newUniqueId)) {
            if (StringUtils.isBlank(originalIcn.getUniqueId())) {
                newUniqueId = getNextUniqueId(originalIcn.getCmnodeId());
            } else {
                newUniqueId = originalIcn.getUniqueId();
            }
        }
        newIcn.setUniqueId(newUniqueId);
        newIcn.setVariantCode(newVariantCode);

        // 3. 生成新ICN编码
        String newIcnCode = generateIcnCode(newIcn);
        newIcn.setIcn(newIcnCode);

        // 4. 保存新ICN
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        newIcn.setCreateBy(loginUser.getUsername());
        newIcn.setCreateTime(new Date());
        this.save(newIcn);

        // 5. 保存文件附件
        for (MultipartFile file : files) {
            saveAttachment(newIcnId, file, "实体文件", newIcn.getSecurity(), loginUser.getUsername());
        }

        log.info("差异上传成功，原ICN：{}，新ICN：{}", originalIcn.getIcn(), newIcnCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadNewVersion(String icnId, MultipartFile[] files) throws IOException {
        IetmIcnManage icnManage = this.getById(icnId);
        if (icnManage == null) {
            throw new JeecgBootException("ICN记录不存在");
        }

        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

        // 1. 删除原实体文件
        LambdaQueryWrapper<IetmAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IetmAttachment::getPid, icnId)
               .eq(IetmAttachment::getFileType, "实体文件");
        List<IetmAttachment> oldAttachments = attachmentService.list(wrapper);
        if (!oldAttachments.isEmpty()) {
            for (IetmAttachment att : oldAttachments) {
                deletePhysicalFile(att.getFileKey());
            }
            attachmentService.remove(wrapper);
        }

        // 2. 保存新文件
        for (MultipartFile file : files) {
            saveAttachment(icnId, file, "实体文件", icnManage.getSecurity(), loginUser.getUsername());
        }

        // 3. 更新ICN记录
        icnManage.setUpdateBy(loginUser.getUsername());
        icnManage.setUpdateTime(new Date());
        this.updateById(icnManage);

        log.info("新版上传成功，ICN编码：{}", icnManage.getIcn());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeWithAttachments(String icnId) {
        // 1. 查询并删除附件
        LambdaQueryWrapper<IetmAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IetmAttachment::getPid, icnId);
        List<IetmAttachment> attachments = attachmentService.list(wrapper);

        // 2. 删除物理文件
        for (IetmAttachment att : attachments) {
            deletePhysicalFile(att.getFileKey());
        }

        // 3. 删除附件记录
        attachmentService.remove(wrapper);

        // 4. 删除ICN记录（逻辑删除）
        this.removeById(icnId);

        log.info("删除ICN成功，ID：{}", icnId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeBatchWithAttachments(List<String> icnIds) {
        for (String icnId : icnIds) {
            removeWithAttachments(icnId);
        }
    }

    @Override
    public String getNextUniqueId(String cmnodeId) {
        String maxUniqueId = this.baseMapper.getMaxUniqueIdByCmnodeId(cmnodeId);

        if (StringUtils.isBlank(maxUniqueId)) {
            return "00001";
        }

        try {
            int nextNumber = Integer.parseInt(maxUniqueId) + 1;
            return String.format("%05d", nextNumber);
        } catch (NumberFormatException e) {
            log.error("解析uniqueId失败：{}", maxUniqueId, e);
            return "00001";
        }
    }

    @Override
    public IcnProjectInfoVO getProjectInfo(String cmnodeId) {
        // 1. 获取构型节点
        IetmProjectConfigurationManagement config = configurationService.getById(cmnodeId);
        if (config == null) {
            throw new JeecgBootException("构型节点不存在");
        }

        // 2. 获取项目信息
        IetmProject project = projectService.getById(config.getProjectId());
        if (project == null) {
            throw new JeecgBootException("项目信息不存在");
        }

        // 3. 计算SNS编码
        String sns = calculateSns(cmnodeId, project.getCodeRule());

        // 4. 获取下一个uniqueId
        String uniqueId = getNextUniqueId(cmnodeId);

        // 5. 组装返回对象
        IcnProjectInfoVO vo = new IcnProjectInfoVO();
        vo.setProjectId(project.getId());
        vo.setSecurity(project.getSecurity());
        vo.setUniqueId(uniqueId);
        vo.setSns(sns);
        vo.setCodeRule(project.getCodeRule());

        return vo;
    }

    @Override
    public String generateIcnCode(IetmIcnManage icnManage) {
        StringBuilder icnCode = new StringBuilder("ICN");

        if (StringUtils.isNotBlank(icnManage.getSns())) {
            icnCode.append("-").append(icnManage.getSns());
        }
        if (StringUtils.isNotBlank(icnManage.getRpc())) {
            icnCode.append("-").append(icnManage.getRpc());
        }
        if (StringUtils.isNotBlank(icnManage.getOriginator())) {
            icnCode.append("-").append(icnManage.getOriginator());
        }
        if (StringUtils.isNotBlank(icnManage.getUniqueId())) {
            icnCode.append("-").append(icnManage.getUniqueId());
        }
        if (StringUtils.isNotBlank(icnManage.getVariantCode())) {
            icnCode.append("-").append(icnManage.getVariantCode());
        }
        if (StringUtils.isNotBlank(icnManage.getIssueNo())) {
            icnCode.append("-").append(icnManage.getIssueNo());
        }
        if (icnManage.getSecurity() != null) {
            icnCode.append("-0").append(icnManage.getSecurity());
        }

        return icnCode.toString();
    }

    @Override
    public String calculateSns(String cmnodeId, String codeRule) {
        // 1. 递归获取构型路径
        List<String> configPath = new ArrayList<>();
        buildConfigPath(cmnodeId, configPath);

        // 2. 反转路径（从根到叶）
        Collections.reverse(configPath);

        if (configPath.size() < 2) {
            return "";
        }

        // 3. 构建SNS
        StringBuilder sns = new StringBuilder();
        sns.append(configPath.get(0)).append("-").append(configPath.get(1)).append("-");

        // 4. 根据编码规则补充
        String[] ruleParts = codeRule.split("-");
        for (int i = 1; i < ruleParts.length - 2; i++) {
            if (i + 1 < configPath.size()) {
                sns.append(configPath.get(i + 1));
            } else {
                sns.append(ruleParts[i]);
            }
        }

        return sns.toString();
    }

    @Override
    public List<IetmIcnManage> listWithAttachments(String cmnodeId, String includeChildren) {
        if ("1".equals(includeChildren)) {
            // 查询当前节点及所有子节点的ICN
            return this.baseMapper.listWithAttachmentsIncludeChildren(cmnodeId);
        } else {
            // 只查询当前节点的ICN
            return this.baseMapper.listWithAttachments(cmnodeId);
        }
    }

    @Override
    public void downloadFile(String fileKey, HttpServletResponse response) throws IOException {
        String filePath = fileStorageLocation + fileKey;
        File file = new File(filePath);

        if (!file.exists()) {
            throw new JeecgBootException("文件不存在");
        }

        // 解密并输出
        try (InputStream encryptedInput = new FileInputStream(file);
             OutputStream output = response.getOutputStream()) {

            DESUtils.decodeBase64File(encryptedInput, output, null);
            output.flush();
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new IOException("文件下载失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importExcel(MultipartFile file) throws Exception {
        // TODO: 实现Excel导入逻辑
        throw new JeecgBootException("导入功能待实现");
    }

    // ========== 私有方法 ==========

    /**
     * 保存附件记录并加密存储文件
     */
    private void saveAttachment(String icnId, MultipartFile file, String fileType,
                                Integer security, String createBy) throws IOException {
        // 1. 生成文件唯一标识
        String fileName = file.getOriginalFilename();
        String fileKey = UUID.randomUUID().toString();
        if (fileName != null && fileName.contains(".")) {
            fileKey += fileName.substring(fileName.lastIndexOf("."));
        }

        // 2. 加密存储文件
        File storageDir = new File(fileStorageLocation);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        String filePath = fileStorageLocation + fileKey;
        try (InputStream input = file.getInputStream()) {
            DESUtils.encodeBase64File(input, filePath, null);
        } catch (Exception e) {
            log.error("文件加密存储失败", e);
            throw new IOException("文件加密存储失败", e);
        }

        // 3. 保存附件记录
        IetmAttachment attachment = new IetmAttachment();
        attachment.setId(String.valueOf(IdWorker.getId()));
        attachment.setPid(icnId);
        attachment.setFileName(fileName);
        attachment.setFileKey(fileKey);
        attachment.setFileSize(new BigDecimal(file.getSize()).divide(new BigDecimal(1024), 2, BigDecimal.ROUND_HALF_UP));
        attachment.setFileType(fileType);
        attachment.setSecurity(security);
        attachment.setCreateBy(createBy);
        attachment.setCreateTime(new Date());

        attachmentService.save(attachment);
    }

    /**
     * 删除物理文件
     */
    private void deletePhysicalFile(String fileKey) {
        if (StringUtils.isBlank(fileKey)) {
            return;
        }

        String filePath = fileStorageLocation + fileKey;
        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                log.warn("物理文件删除失败：{}", filePath);
            }
        }
    }

    /**
     * 递归构建构型路径
     */
    private void buildConfigPath(String cmnodeId, List<String> path) {
        IetmProjectConfigurationManagement config = configurationService.getById(cmnodeId);
        if (config != null) {
            path.add(config.getCode());
            if (StringUtils.isNotBlank(config.getPid()) && !"0".equals(config.getPid())) {
                buildConfigPath(config.getPid(), path);
            }
        }
    }

    /**
     * 批量新增ICN
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAddIcn(IetmIcnManage template) {
        Integer count = template.getCount();
        String cmnodeId = template.getCmnodeId();

        // 获取项目信息和SNS
        IcnProjectInfoVO projectInfo = getProjectInfo(cmnodeId);

        // 获取当前最大的uniqueId
        String currentMaxUniqueId = getNextUniqueId(cmnodeId);
        int startUniqueId = Integer.parseInt(currentMaxUniqueId);

        int successCount = 0;

        for (int i = 0; i < count; i++) {
            try {
                IetmIcnManage icnManage = new IetmIcnManage();

                // 复制模板字段
                icnManage.setCmnodeId(cmnodeId);
                icnManage.setSns(projectInfo.getSns());
                icnManage.setUniqueId(String.format("%05d", startUniqueId + i));
                icnManage.setVariantCode(template.getVariantCode());
                icnManage.setIssueNo(template.getIssueNo());
                icnManage.setSecurity(template.getSecurity());
                icnManage.setIcnType(template.getIcnType());
                icnManage.setOriginator(template.getOriginator());
                icnManage.setOriginatorName(template.getOriginatorName());
                icnManage.setRpc(template.getRpc());
                icnManage.setRpcName(template.getRpcName());

                // 生成ICN完整编码
                String icnCode = generateIcnCode(icnManage);
                icnManage.setIcn(icnCode);

                // 保存记录
                this.save(icnManage);
                successCount++;

            } catch (Exception e) {
                log.error("批量新增第 {} 条记录失败", i + 1, e);
                throw new RuntimeException("批量新增失败，已成功创建 " + successCount + " 条记录", e);
            }
        }

        return successCount;
    }
}

