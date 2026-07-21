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
import java.text.SimpleDateFormat;
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

    @Autowired
    private org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnReferenceMapper referenceMapper;

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Value("${accessFile.icnLocation}")
    private String fileStorageLocation;

    @Value("${jeecg.uploadType}")
    private String uploadType;

    // Redis缓存key前缀
    private static final String DOWNLOAD_TASK_KEY_PREFIX = "ietm:download:task:";

    // 任务缓存过期时间（24小时，单位：秒）
    private static final long TASK_CACHE_EXPIRE_SECONDS = 24 * 60 * 60L;

    // 异步任务线程池
    private static final java.util.concurrent.ExecutorService asyncExecutor =
        java.util.concurrent.Executors.newFixedThreadPool(5, new java.util.concurrent.ThreadFactory() {
            private final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("async-download-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });


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

        // 如果newUniqueId为空，则自动生成新的uniqueId（当前节点最大值+1）
        // 差异上传时必须生成新的uniqueId，避免违反唯一性约束
        if (StringUtils.isBlank(newUniqueId)) {
            newUniqueId = getNextUniqueId(originalIcn.getCmnodeId());
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
    public IetmIcnManage getByIdWithAttachment(String id) {
        return this.baseMapper.getByIdWithAttachment(id);
    }

    @Override
    public void downloadFile(String fileKey, HttpServletResponse response) throws IOException {
        outputDecryptedFile(fileKey, response, false);
    }

    @Override
    public void viewFile(String fileKey, HttpServletResponse response) throws IOException {
        outputDecryptedFile(fileKey, response, true);
    }

    /**
     * 输出解密后的文件
     * @param fileKey 文件Key（相对路径，如：icn\xxxxx.jpg）
     * @param response HTTP响应
     * @param setContentType 是否设置Content-Type（预览需要，下载不需要）
     */
    private void outputDecryptedFile(String fileKey, HttpServletResponse response, boolean setContentType) throws IOException {
        // 提取文件名
        String fileName = extractFileName(fileKey);

        // 构建文件物理路径
        String filePath = fileStorageLocation + File.separator + fileName;
        File file = new File(filePath);

        if (!file.exists()) {
            log.error("文件不存在: {}", filePath);
            if (setContentType) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            } else {
                throw new JeecgBootException("文件不存在: " + filePath);
            }
        }

        // 预览模式：设置Content-Type和缓存
        if (setContentType) {
            String fileExt = "";
            if (fileName.contains(".")) {
                fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            }
            response.setContentType(getContentTypeByExtension(fileExt));
            response.setHeader("Cache-Control", "max-age=3600");
        }

        // 解密并输出文件
        try (InputStream encryptedInput = new FileInputStream(file);
             OutputStream output = response.getOutputStream()) {
            DESUtils.decodeBase64File(encryptedInput, output, null);
            output.flush();
        } catch (Exception e) {
            String errorMsg = setContentType ? "文件预览失败" : "文件下载失败";
            log.error(errorMsg, e);
            throw new IOException(errorMsg, e);
        }
    }

    /**
     * 从fileKey中提取文件名
     * @param fileKey 文件Key（可能包含路径，如：icn\xxxxx.jpg 或 icn/xxxxx.jpg）
     * @return 文件名（如：xxxxx.jpg）
     */
    private String extractFileName(String fileKey) {
        String fileName = fileKey;
        if (fileKey.contains(File.separator)) {
            fileName = fileKey.substring(fileKey.lastIndexOf(File.separator) + 1);
        } else if (fileKey.contains("/")) {
            fileName = fileKey.substring(fileKey.lastIndexOf("/") + 1);
        }
        return fileName;
    }

    /**
     * 根据文件扩展名获取Content-Type
     */
    private String getContentTypeByExtension(String fileExt) {
        switch (fileExt) {
            // 图片
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "tif":
            case "tiff":
                return "image/tiff";
            case "svg":
                return "image/svg+xml";
            case "cgm":
                return "image/cgm";

            // 视频
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
            case "ogg":
                return "video/ogg";

            // 音频
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";

            // 其他
            case "swf":
                return "application/x-shockwave-flash";
            case "pdf":
                return "application/pdf";
            case "wrl":
                return "model/vrml";

            default:
                return "application/octet-stream";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importExcel(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new JeecgBootException("导入文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx"))) {
            throw new JeecgBootException("只支持.xls和.xlsx格式的Excel文件");
        }

        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String username = loginUser.getUsername();

        try {
            // 使用POI框架解析Excel
            List<IetmIcnManage> importList = org.jeecgframework.poi.excel.ExcelImportUtil.importExcel(
                file.getInputStream(),
                IetmIcnManage.class,
                new org.jeecgframework.poi.excel.entity.ImportParams()
            );

            if (importList == null || importList.isEmpty()) {
                throw new JeecgBootException("Excel文件中没有数据");
            }

            int successCount = 0;
            int failCount = 0;
            List<String> errorMessages = new ArrayList<>();

            for (int i = 0; i < importList.size(); i++) {
                IetmIcnManage icn = importList.get(i);
                int rowNum = i + 2; // Excel行号从2开始（第1行是表头）

                try {
                    // 数据校验
                    if (StringUtils.isBlank(icn.getCmnodeId())) {
                        errorMessages.add("第" + rowNum + "行：构型节点ID不能为空");
                        failCount++;
                        continue;
                    }

                    // 生成主键
                    icn.setId(String.valueOf(IdWorker.getId()));

                    // 生成唯一识别码（如果为空）
                    if (StringUtils.isBlank(icn.getUniqueId())) {
                        String uniqueId = getNextUniqueId(icn.getCmnodeId());
                        icn.setUniqueId(uniqueId);
                    }

                    // 生成ICN完整编码
                    String icnCode = generateIcnCode(icn);
                    icn.setIcn(icnCode);

                    // 设置创建信息
                    icn.setCreateBy(username);
                    icn.setCreateTime(new Date());

                    // 保存ICN记录
                    this.save(icn);
                    successCount++;

                } catch (Exception e) {
                    errorMessages.add("第" + rowNum + "行导入失败：" + e.getMessage());
                    failCount++;
                    log.error("导入第{}行失败", rowNum, e);
                }
            }

            // 记录导入结果
            log.info("Excel导入完成：成功{}条，失败{}条", successCount, failCount);

            if (failCount > 0) {
                String errorMsg = "导入完成：成功" + successCount + "条，失败" + failCount + "条。\n" +
                    "失败详情：\n" + String.join("\n", errorMessages.subList(0, Math.min(10, errorMessages.size())));
                if (errorMessages.size() > 10) {
                    errorMsg += "\n...还有" + (errorMessages.size() - 10) + "条错误";
                }
                throw new JeecgBootException(errorMsg);
            }

        } catch (Exception e) {
            log.error("Excel导入失败", e);
            if (e instanceof JeecgBootException) {
                throw e;
            }
            throw new JeecgBootException("Excel导入失败：" + e.getMessage());
        }
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

        // 2. 加密存储文件到ICN专用目录
        // fileStorageLocation = D:\workspace\IETM\file\icn
        File storageDir = new File(fileStorageLocation);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // 物理文件保存路径：D:\workspace\IETM\file\icn\xxxxx.jpg
        String filePath = fileStorageLocation + File.separator + fileKey;
        try (InputStream input = file.getInputStream()) {
            DESUtils.encodeBase64File(input, filePath, null);
        } catch (Exception e) {
            log.error("文件加密存储失败", e);
            throw new IOException("文件加密存储失败", e);
        }

        // 3. 保存附件记录
        // fileKey存储相对于基础路径(D:\workspace\IETM\file)的相对路径：icn/xxxxx.jpg
        // 这样预览时 CommonController 会拼接为：D:\workspace\IETM\file + icn/xxxxx.jpg
        String relativeFileKey = "icn" + File.separator + fileKey;

        IetmAttachment attachment = new IetmAttachment();
        attachment.setId(String.valueOf(IdWorker.getId()));
        attachment.setPid(icnId);
        attachment.setFileName(fileName);
        attachment.setFileKey(relativeFileKey);
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

        // fileKey是相对路径（例如：icn/xxxxx.jpg）
        // 需要从ICN专用目录中删除（从fileKey中提取文件名）
        String fileName = fileKey;
        if (fileKey.contains(File.separator)) {
            fileName = fileKey.substring(fileKey.lastIndexOf(File.separator) + 1);
        } else if (fileKey.contains("/")) {
            fileName = fileKey.substring(fileKey.lastIndexOf("/") + 1);
        }

        String filePath = fileStorageLocation + File.separator + fileName;
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

    @Override
    public org.jeecg.modules.ietm.icnmanage.vo.PreviewInfoVO getPreviewInfo(String id) throws Exception {
        // 查询ICN及附件信息
        IetmIcnManage icn = baseMapper.getByIdWithAttachment(id);
        if (icn == null) {
            throw new JeecgBootException("ICN记录不存在");
        }

        IetmAttachment attachment = icn.getIetmAttachment();
        if (attachment == null) {
            throw new JeecgBootException("ICN实体文件不存在");
        }

        // 构建预览信息VO
        org.jeecg.modules.ietm.icnmanage.vo.PreviewInfoVO vo = new org.jeecg.modules.ietm.icnmanage.vo.PreviewInfoVO();
        vo.setIcnId(icn.getId());
        vo.setIcn(icn.getIcn());
        vo.setFileName(attachment.getFileName());
        vo.setFileType(attachment.getFileType());
        vo.setFileSize(attachment.getFileSize() != null ? attachment.getFileSize().longValue() : 0L);
        vo.setFilePath(attachment.getFileKey());
        vo.setIssueNo(icn.getIssueNo());
        vo.setSecurity(icn.getSecurity());
        // 格式化创建时间为 YYYY-MM-DD
        if (icn.getCreateTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            vo.setCreateTime(sdf.format(icn.getCreateTime()));
        } else {
            vo.setCreateTime("");
        }

        // 获取文件扩展名
        String fileName = attachment.getFileName();
        String fileExt = "";
        if (fileName != null && fileName.contains(".")) {
            fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        vo.setFileExt(fileExt);

        // 判断预览类型
        String previewType = determinePreviewType(fileExt);
        vo.setPreviewType(previewType);
        vo.setCanPreview(!"OTHER".equals(previewType));

        // 根据uploadType生成文件访问URL
        String fileUrl;
        String fileKey = attachment.getFileKey();

        if ("alioss".equalsIgnoreCase(uploadType)) {
            // 阿里云OSS：使用完整的OSS URL
            // fileKey已经包含完整的OSS URL
            fileUrl = fileKey;
        } else if ("minio".equalsIgnoreCase(uploadType)) {
            // MinIO：使用MinIO URL
            fileUrl = fileKey;
        } else {
            // 本地存储：使用ICN专用的预览接口（支持解密）
            // 将Windows路径分隔符转换为URL格式
            String normalizedFileKey = fileKey.replace("\\", "/");
            fileUrl = "/icnmanage/ietmIcnManage/viewFile?fileKey=" + normalizedFileKey;
        }

        vo.setFileUrl(fileUrl);

        return vo;
    }

    @Override
    public org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO getReferenceInfo(String id) throws Exception {
        IetmIcnManage icn = this.getById(id);
        if (icn == null) {
            throw new JeecgBootException("ICN记录不存在");
        }

        org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO vo = new org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO();
        vo.setIcnId(icn.getId());
        vo.setIcn(icn.getIcn());

        // 查询正向引用（该ICN引用的其他ICN）
        List<IetmIcnManage> referencedList = baseMapper.getReferencedIcnList(id);
        List<org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO.IcnReferenceItem> referencedItems = new ArrayList<>();
        for (IetmIcnManage ref : referencedList) {
            org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO.IcnReferenceItem item =
                new org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO.IcnReferenceItem();
            item.setId(ref.getId());
            item.setIcn(ref.getIcn());
            item.setIssueNo(ref.getIssueNo());
            if (ref.getIetmAttachment() != null) {
                item.setFileName(ref.getIetmAttachment().getFileName());
                item.setFileType(ref.getIetmAttachment().getFileType());
            }
            referencedItems.add(item);
        }
        vo.setReferencedIcnList(referencedItems);

        // 查询反向引用（引用该ICN的其他ICN）
        List<IetmIcnManage> referencingList = baseMapper.getReferencingIcnList(id);
        List<org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO.IcnReferenceItem> referencingItems = new ArrayList<>();
        for (IetmIcnManage ref : referencingList) {
            org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO.IcnReferenceItem item =
                new org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO.IcnReferenceItem();
            item.setId(ref.getId());
            item.setIcn(ref.getIcn());
            item.setIssueNo(ref.getIssueNo());
            if (ref.getIetmAttachment() != null) {
                item.setFileName(ref.getIetmAttachment().getFileName());
                item.setFileType(ref.getIetmAttachment().getFileType());
            }
            referencingItems.add(item);
        }
        vo.setReferencingIcnList(referencingItems);

        // 查询DM引用
        List<Map<String, Object>> dmList = baseMapper.getReferencedByDmList(id);
        List<org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO.DmReferenceItem> dmItems = new ArrayList<>();
        for (Map<String, Object> dm : dmList) {
            org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO.DmReferenceItem item =
                new org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO.DmReferenceItem();
            item.setId(String.valueOf(dm.get("id")));
            item.setDmCode(String.valueOf(dm.get("dm_code")));
            item.setDmTitle(String.valueOf(dm.get("dm_title")));
            item.setDmType(String.valueOf(dm.get("dm_type")));
            dmItems.add(item);
        }
        vo.setDmReferenceList(dmItems);

        // 统计信息
        org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO.ReferenceStatistics statistics =
            new org.jeecg.modules.ietm.icnmanage.vo.ReferenceInfoVO.ReferenceStatistics();
        statistics.setReferencedIcnCount(referencedItems.size());
        statistics.setReferencingIcnCount(referencingItems.size());
        statistics.setDmReferenceCount(dmItems.size());
        statistics.setTotalReferenceCount(referencedItems.size() + referencingItems.size() + dmItems.size());
        vo.setStatistics(statistics);

        return vo;
    }

    @Override
    public void downloadSingle(String id, Boolean includeRelated, HttpServletResponse response) throws Exception {
        List<String> ids = Arrays.asList(id);
        downloadBatch(ids, includeRelated, response);
    }

    @Override
    public void downloadBatch(List<String> ids, Boolean includeRelated, HttpServletResponse response) throws Exception {
        if (ids == null || ids.isEmpty()) {
            throw new JeecgBootException("请选择要下载的ICN");
        }

        // 查询ICN及附件
        String includeRelatedStr = (includeRelated != null && includeRelated) ? "1" : "0";
        List<IetmIcnManage> icnList = baseMapper.listByIdsWithAttachments(ids, includeRelatedStr);

        if (icnList.isEmpty()) {
            throw new JeecgBootException("未找到相关ICN数据");
        }

        // 如果只有一个文件，直接下载
        if (icnList.size() == 1 && icnList.get(0).getIetmAttachment() != null) {
            IetmAttachment attachment = icnList.get(0).getIetmAttachment();
            downloadFile(attachment.getFileKey(), response);
            return;
        }

        // 多个文件，打包成ZIP下载
        downloadAsZip(icnList, response);
    }

    @Override
    public String downloadBatchAsync(List<String> ids, Boolean includeRelated) throws Exception {
        if (ids == null || ids.isEmpty()) {
            throw new JeecgBootException("请选择要下载的ICN");
        }

        // 生成任务ID
        String taskId = String.valueOf(IdWorker.getId());
        String redisKey = DOWNLOAD_TASK_KEY_PREFIX + taskId;

        // 创建任务状态VO
        org.jeecg.modules.ietm.icnmanage.vo.DownloadTaskVO taskVO =
            new org.jeecg.modules.ietm.icnmanage.vo.DownloadTaskVO();
        taskVO.setTaskId(taskId);
        taskVO.setStatus("PROCESSING");
        taskVO.setProgress(0);
        taskVO.setTotalFiles(ids.size());
        taskVO.setProcessedFiles(0);
        taskVO.setCreateTime(new Date());

        // 放入Redis缓存（24小时过期）
        redisTemplate.opsForValue().set(redisKey, taskVO, TASK_CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

        // 获取当前用户
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String username = loginUser.getUsername();

        // 提交异步任务
        asyncExecutor.submit(() -> {
            try {
                log.info("开始异步下载任务：{}, 用户：{}, ICN数量：{}", taskId, username, ids.size());

                // 更新状态为处理中
                taskVO.setStatus("PROCESSING");
                taskVO.setStartTime(new Date());
                redisTemplate.opsForValue().set(redisKey, taskVO, TASK_CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

                // 查询ICN及附件
                String includeRelatedStr = (includeRelated != null && includeRelated) ? "1" : "0";
                List<IetmIcnManage> icnList = baseMapper.listByIdsWithAttachments(ids, includeRelatedStr);

                if (icnList.isEmpty()) {
                    taskVO.setStatus("FAILED");
                    taskVO.setErrorMessage("未找到相关ICN数据");
                    taskVO.setEndTime(new Date());
                    redisTemplate.opsForValue().set(redisKey, taskVO, TASK_CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                    log.warn("异步下载任务{}失败：未找到ICN数据", taskId);
                    return;
                }

                // 生成临时ZIP文件路径
                String tempDir = fileStorageLocation + "/temp/downloads";
                File tempDirFile = new File(tempDir);
                if (!tempDirFile.exists()) {
                    tempDirFile.mkdirs();
                }

                String zipFileName = "ICN_batch_" + taskId + ".zip";
                File zipFile = new File(tempDir, zipFileName);

                // 创建ZIP文件
                try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                    new FileOutputStream(zipFile))) {

                    java.util.Set<String> addedFileNames = new java.util.HashSet<>();
                    int processedCount = 0;

                    for (IetmIcnManage icn : icnList) {
                        if (icn.getIetmAttachment() == null) {
                            log.warn("ICN {} 没有附件，跳过", icn.getIcn());
                            continue;
                        }

                        IetmAttachment attachment = icn.getIetmAttachment();
                        String fileName = attachment.getFileName();
                        String fileKey = attachment.getFileKey();

                        // 处理文件名重复
                        String uniqueFileName = fileName;
                        int counter = 1;
                        while (addedFileNames.contains(uniqueFileName)) {
                            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
                            String ext = fileName.substring(fileName.lastIndexOf('.'));
                            uniqueFileName = nameWithoutExt + "_" + counter + ext;
                            counter++;
                        }
                        addedFileNames.add(uniqueFileName);

                        // 从fileKey中提取实际文件名
                        String actualFileName = fileKey;
                        if (fileKey.contains(File.separator)) {
                            actualFileName = fileKey.substring(fileKey.lastIndexOf(File.separator) + 1);
                        } else if (fileKey.contains("/")) {
                            actualFileName = fileKey.substring(fileKey.lastIndexOf("/") + 1);
                        }

                        // 添加文件到ZIP
                        File file = new File(fileStorageLocation, actualFileName);
                        if (file.exists()) {
                            try (FileInputStream fis = new FileInputStream(file)) {
                                java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(uniqueFileName);
                                zos.putNextEntry(zipEntry);

                                byte[] buffer = new byte[4096];
                                int length;
                                while ((length = fis.read(buffer)) > 0) {
                                    zos.write(buffer, 0, length);
                                }
                                zos.closeEntry();
                            }
                        }

                        // 更新进度（每处理5个文件更新一次Redis，减少IO）
                        processedCount++;
                        if (processedCount % 5 == 0 || processedCount == icnList.size()) {
                            taskVO.setProcessedFiles(processedCount);
                            taskVO.setProgress((int) ((processedCount * 100.0) / icnList.size()));
                            redisTemplate.opsForValue().set(redisKey, taskVO, TASK_CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                        }
                    }

                    zos.finish();
                }

                // 任务完成
                taskVO.setStatus("COMPLETED");
                taskVO.setProgress(100);
                taskVO.setProcessedFiles(icnList.size());
                taskVO.setDownloadUrl("/sys/common/download/temp/downloads/" + zipFileName);
                taskVO.setEndTime(new Date());
                redisTemplate.opsForValue().set(redisKey, taskVO, TASK_CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

                log.info("异步下载任务{}完成，共处理{}个文件", taskId, icnList.size());

            } catch (Exception e) {
                log.error("异步下载任务{}执行失败", taskId, e);
                taskVO.setStatus("FAILED");
                taskVO.setErrorMessage(e.getMessage());
                taskVO.setEndTime(new Date());
                redisTemplate.opsForValue().set(redisKey, taskVO, TASK_CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            }
        });

        log.info("创建异步下载任务：{}, ICN数量：{}", taskId, ids.size());
        return taskId;
    }

    @Override
    public org.jeecg.modules.ietm.icnmanage.vo.DownloadTaskVO getDownloadTaskStatus(String taskId) throws Exception {
        if (StringUtils.isBlank(taskId)) {
            throw new JeecgBootException("任务ID不能为空");
        }

        String redisKey = DOWNLOAD_TASK_KEY_PREFIX + taskId;

        // 从Redis中获取任务状态
        org.jeecg.modules.ietm.icnmanage.vo.DownloadTaskVO taskVO =
            (org.jeecg.modules.ietm.icnmanage.vo.DownloadTaskVO) redisTemplate.opsForValue().get(redisKey);

        if (taskVO == null) {
            // 任务不存在或已过期
            taskVO = new org.jeecg.modules.ietm.icnmanage.vo.DownloadTaskVO();
            taskVO.setTaskId(taskId);
            taskVO.setStatus("NOT_FOUND");
            taskVO.setErrorMessage("任务不存在或已过期（任务缓存保留24小时）");
        }

        return taskVO;
    }

    /**
     * 清理过期的下载任务（可由定时任务调用）
     * Redis会自动过期，但为了清理磁盘上的临时文件，仍需定时扫描
     */
    public void cleanExpiredTasks() {
        try {
            log.info("开始清理过期下载任务...");

            // 清理临时文件目录中超过24小时的ZIP文件
            String tempDir = fileStorageLocation + "/temp/downloads";
            File tempDirFile = new File(tempDir);

            if (!tempDirFile.exists() || !tempDirFile.isDirectory()) {
                log.info("临时下载目录不存在，无需清理");
                return;
            }

            File[] files = tempDirFile.listFiles();
            if (files == null || files.length == 0) {
                log.info("临时下载目录为空，无需清理");
                return;
            }

            long now = System.currentTimeMillis();
            long expireTime = 24 * 60 * 60 * 1000L; // 24小时
            int deletedCount = 0;

            for (File file : files) {
                if (file.isFile() && file.getName().startsWith("ICN_batch_") && file.getName().endsWith(".zip")) {
                    long fileAge = now - file.lastModified();
                    if (fileAge > expireTime) {
                        if (file.delete()) {
                            deletedCount++;
                            log.info("删除过期下载文件：{}", file.getName());
                        } else {
                            log.warn("删除过期下载文件失败：{}", file.getName());
                        }
                    }
                }
            }

            log.info("清理过期下载任务完成，共删除 {} 个文件", deletedCount);

        } catch (Exception e) {
            log.error("清理过期下载任务失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addReference(String sourceIcnId, String targetIcnId, String referenceType, String remark) throws Exception {
        // 参数校验
        if (StringUtils.isBlank(sourceIcnId)) {
            throw new JeecgBootException("源ICN ID不能为空");
        }
        if (StringUtils.isBlank(targetIcnId)) {
            throw new JeecgBootException("目标ID不能为空");
        }
        if (StringUtils.isBlank(referenceType)) {
            throw new JeecgBootException("引用类型不能为空");
        }

        // 校验引用类型
        if (!"ICN_TO_ICN".equals(referenceType) && !"ICN_TO_DM".equals(referenceType)) {
            throw new JeecgBootException("引用类型必须是 ICN_TO_ICN 或 ICN_TO_DM");
        }

        // 检查引用关系是否已存在
        int count = referenceMapper.checkReferenceExists(sourceIcnId, targetIcnId, referenceType);
        if (count > 0) {
            throw new JeecgBootException("该引用关系已存在");
        }

        // 创建引用关系
        org.jeecg.modules.ietm.icnmanage.entity.IetmIcnReference reference =
            new org.jeecg.modules.ietm.icnmanage.entity.IetmIcnReference();
        reference.setSourceIcnId(sourceIcnId);
        reference.setReferenceType(referenceType);
        reference.setRemark(remark);

        // 根据引用类型设置不同字段
        if ("ICN_TO_ICN".equals(referenceType)) {
            reference.setTargetIcnId(targetIcnId);
        } else if ("ICN_TO_DM".equals(referenceType)) {
            reference.setDmCode(targetIcnId);
        }

        // 设置创建信息
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        reference.setCreateBy(loginUser.getUsername());
        reference.setCreateTime(new Date());

        // 保存引用关系
        referenceMapper.insert(reference);

        log.info("添加引用关系成功：{} -> {}, 类型：{}", sourceIcnId, targetIcnId, referenceType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReference(String referenceId) throws Exception {
        // 参数校验
        if (StringUtils.isBlank(referenceId)) {
            throw new JeecgBootException("引用关系ID不能为空");
        }

        // 检查引用关系是否存在
        org.jeecg.modules.ietm.icnmanage.entity.IetmIcnReference reference =
            referenceMapper.selectById(referenceId);
        if (reference == null) {
            throw new JeecgBootException("引用关系不存在");
        }

        // 删除引用关系
        referenceMapper.deleteById(referenceId);

        log.info("删除引用关系成功：{}", referenceId);
    }

    /**
     * 判断文件预览类型
     */
    private String determinePreviewType(String fileExt) {
        if (StringUtils.isBlank(fileExt)) {
            return "OTHER";
        }

        fileExt = fileExt.toLowerCase();

        // 图片
        if (Arrays.asList("bmp", "jpg", "jpeg", "png", "gif", "tif", "tiff", "svg").contains(fileExt)) {
            return "IMAGE";
        }
        // CGM
        if ("cgm".equals(fileExt)) {
            return "CGM";
        }
        // 视频
        if (Arrays.asList("mp4", "webm", "ogg", "avi", "wmv", "mov", "rm", "mpg").contains(fileExt)) {
            return "VIDEO";
        }
        // 音频
        if ("mp3".equals(fileExt)) {
            return "AUDIO";
        }
        // Flash
        if ("swf".equals(fileExt)) {
            return "FLASH";
        }
        // 3D模型
        if ("wrl".equals(fileExt)) {
            return "3D";
        }
        // SMG
        if ("smg".equals(fileExt)) {
            return "SMG";
        }

        return "OTHER";
    }

    /**
     * 打包下载为ZIP
     */
    private void downloadAsZip(List<IetmIcnManage> icnList, HttpServletResponse response) throws Exception {
        if (icnList == null || icnList.isEmpty()) {
            throw new JeecgBootException("没有可下载的文件");
        }

        // 设置响应头
        String zipFileName = "ICN_batch_" + System.currentTimeMillis() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
            new String(zipFileName.getBytes("UTF-8"), "ISO-8859-1") + "\"");

        // 使用ZipOutputStream创建ZIP文件
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(response.getOutputStream())) {

            // 用于避免文件名重复
            java.util.Set<String> addedFileNames = new java.util.HashSet<>();
            int fileCount = 0;

            for (IetmIcnManage icn : icnList) {
                if (icn.getIetmAttachment() == null) {
                    log.warn("ICN {} 没有附件，跳过", icn.getIcn());
                    continue;
                }

                IetmAttachment attachment = icn.getIetmAttachment();
                String fileName = attachment.getFileName();
                String fileKey = attachment.getFileKey();

                // 处理文件名重复问题
                String uniqueFileName = fileName;
                int counter = 1;
                while (addedFileNames.contains(uniqueFileName)) {
                    String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
                    String ext = fileName.substring(fileName.lastIndexOf('.'));
                    uniqueFileName = nameWithoutExt + "_" + counter + ext;
                    counter++;
                }
                addedFileNames.add(uniqueFileName);

                // 从fileKey中提取实际文件名
                String actualFileName = fileKey;
                if (fileKey.contains(File.separator)) {
                    actualFileName = fileKey.substring(fileKey.lastIndexOf(File.separator) + 1);
                } else if (fileKey.contains("/")) {
                    actualFileName = fileKey.substring(fileKey.lastIndexOf("/") + 1);
                }

                // 构建文件路径
                File file = new File(fileStorageLocation, actualFileName);
                if (!file.exists()) {
                    log.warn("文件不存在：{}", file.getAbsolutePath());
                    continue;
                }

                // 添加文件到ZIP
                try (FileInputStream fis = new FileInputStream(file)) {
                    java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(uniqueFileName);
                    zos.putNextEntry(zipEntry);

                    // 写入文件内容
                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }

                    zos.closeEntry();
                    fileCount++;
                    log.info("添加文件到ZIP: {}", uniqueFileName);
                } catch (Exception e) {
                    log.error("添加文件到ZIP失败: {}", uniqueFileName, e);
                }
            }

            zos.finish();
            log.info("ZIP打包完成，共打包 {} 个文件", fileCount);

        } catch (Exception e) {
            log.error("ZIP打包下载失败", e);
            throw new Exception("ZIP打包下载失败: " + e.getMessage());
        }
    }
}