package org.jeecg.modules.ietm.ietmimport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.ietm.common.util.FileNameUtils;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.service.IIetmAttachmentService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmComment;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmCommentMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.service.IIetmDataModuleService;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmXmlHelper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmcUtils;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.ietmimport.constants.DmImportConstants;
import org.jeecg.modules.ietm.ietmimport.service.IIetmDmImportService;
import org.jeecg.modules.ietm.ietmimport.vo.DmImportResultVO;
import org.jeecg.modules.ietm.ietmimport.vo.DmValidateResultVO;
import org.jeecg.modules.ietm.ietmimport.vo.ImportFileItemVO;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 数据模块导入Service实现类（完整版 - 第1批：基础框架+XML校验）
 *
 * @author IETM Team
 * @date 2026-09-03
 */
@Service
@Slf4j
public class IetmDmImportServiceImpl extends ServiceImpl<IetmDataModuleMapper, IetmDataModule> implements IIetmDmImportService {

    // ========== 常量定义 ==========

    /**
     * 正则表达式：版本号格式（3位数字-2位数字）
     * 示例：001-03, 002-00
     */
    private static final String VERSION_PATTERN = "\\d{3}-\\d{2}";

    /**
     * 正则表达式：语言代码格式（2小写字母-2大写字母）
     * 示例：zh-CN, en-US
     */
    private static final String LANGUAGE_PATTERN = "[a-z]{2}-[A-Z]{2}";

    /**
     * 正则表达式：雪花ID格式（13-19位纯数字）
     * 示例：1234567890123, 1234567890123456789
     */
    private static final String SNOWFLAKE_ID_PATTERN = "\\d{13,19}";

    /**
     * 元数据段在文件名中的最大位置（从后向前数）
     * 版本号和语言代码通常在前4段内
     */
    private static final int METADATA_MAX_POSITION = 3;

    @Autowired
    private IIetmDataModuleService dataModuleService;

    @Autowired
    private IIetmIcnManageService icnManageService;

    @Autowired
    private IIetmProjectConfigurationManagementService configurationService;

    @Autowired
    private IIetmAttachmentService attachmentService;

    @Autowired
    private IetmDmCommentMapper dmCommentMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${accessFile.location:D:/workspace/IETM/file}")
    private String fileStorageLocation;

    @Override
    public DmValidateResultVO validateFile(MultipartFile file, HttpServletRequest request) throws Exception {
        // 1. 基础校验
        if (file == null || file.isEmpty()) {
            throw new JeecgBootException("文件不能为空");
        }

        // 2. 获取项目上下文（从Redis读取，对齐openProject的设计）
        String projectId = getProjectIdFromRedis();
        if (projectId == null || projectId.isEmpty()) {
            throw new JeecgBootException("请先打开项目");
        }

        // 3. 文件大小校验
        if (file.getSize() > DmImportConstants.MAX_FILE_SIZE) {
            throw new JeecgBootException("文件大小超过限制（1024MB）");
        }

        // 4. 文件类型判断
        String fileName = extractFileName(file);
        String extension = FileNameUtils.getExtension(fileName).toLowerCase();

        List<ImportFileItemVO> fileItems = new ArrayList<>();

        if (DmImportConstants.FILE_TYPE_XML.equals(extension)) {
            // 单一XML文件
            ImportFileItemVO item = validateXmlFile(file, fileName, projectId, request);
            fileItems.add(item);

        } else if (DmImportConstants.FILE_TYPE_ZIP.equals(extension)) {
            // ZIP压缩包
            fileItems = validateZipFile(file, projectId, request);

        } else {
            throw new JeecgBootException("不支持的文件类型，仅支持.xml和.zip");
        }

        // 5. 统计结果
        DmValidateResultVO result = new DmValidateResultVO();
        result.setFiles(fileItems);
        result.setTotalCount(fileItems.size());
        result.setSuccessCount((int) fileItems.stream().filter(ImportFileItemVO::canImport).count());
        result.setFailureCount(result.getTotalCount() - result.getSuccessCount());

        log.info("校验完成：总{}个文件，成功{}个，失败{}个",
                result.getTotalCount(), result.getSuccessCount(), result.getFailureCount());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DmImportResultVO importFiles(List<ImportFileItemVO> files, HttpServletRequest request) throws Exception {
        // 1. 获取项目上下文（从Redis读取）
        String projectId = getProjectIdFromRedis();
        if (projectId == null || projectId.isEmpty()) {
            throw new JeecgBootException("请先打开项目");
        }

        // 2. 筛选可导入的文件
        List<ImportFileItemVO> validFiles = new ArrayList<>();
        for (ImportFileItemVO file : files) {
            if (file.canImport()) {
                validFiles.add(file);
            }
        }

        if (validFiles.isEmpty()) {
            throw new JeecgBootException("没有可导入的文件");
        }

        log.info("开始导入文件：{}个, 项目ID：{}", validFiles.size(), projectId);

        // 3. 分类文件（DM vs ICN vs RESOURCE）
        List<ImportFileItemVO> dmFiles = new ArrayList<>();
        List<ImportFileItemVO> icnFiles = new ArrayList<>();
        List<ImportFileItemVO> resourceFiles = new ArrayList<>();
        for (ImportFileItemVO file : validFiles) {
            if ("DM".equals(file.getFileType())) {
                dmFiles.add(file);
            } else if ("ICN".equals(file.getFileType())) {
                icnFiles.add(file);
            } else if ("RESOURCE".equals(file.getFileType())) {
                resourceFiles.add(file);
            }
        }

        // 4. 执行导入（P2-2修复：添加try-finally确保临时文件清理）
        int dmSuccess = 0;
        int icnSuccess = 0;
        int resourceSuccess = 0;
        int failure = 0;
        List<String> errorMessages = new ArrayList<>();

        try {
            // P1-1修复：预加载构型节点映射表，避免N+1查询
            Map<String, IetmProjectConfigurationManagement> pathToNodeMap = buildPathToNodeMap(projectId);
            Map<String, String> snsToNodeIdMap = buildSnsToNodeIdMap(projectId);

            // 4.1 导入DM（必须先导入DM，资源文件需要关联DM ID）
            for (ImportFileItemVO dmFile : dmFiles) {
                try {
                    // ✅ 修复：只导入校验成功的DM
                    if (!DmImportConstants.SUCCESS.equals(dmFile.getResultCode())) {
                        failure++;
                        String errorMsg = "导入DM失败：" + dmFile.getFileName() + " - " + dmFile.getResultMessage();
                        errorMessages.add(errorMsg);
                        log.warn("跳过校验失败的DM：{} - {}", dmFile.getFileName(), dmFile.getResultMessage());
                        continue;
                    }

                    importSingleDm(dmFile, projectId, pathToNodeMap, request);
                    dmSuccess++;
                    log.info("成功导入DM：{}", dmFile.getDmcCode());
            } catch (Exception e) {
                failure++;
                String errorMsg = "导入DM失败：" + dmFile.getFileName() + " - " + e.getMessage();
                errorMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }

        // 4.2 导入ICN
        for (ImportFileItemVO icnFile : icnFiles) {
            try {
                // ✅ 修复：只导入校验成功的ICN
                if (!DmImportConstants.SUCCESS.equals(icnFile.getResultCode())) {
                    failure++;
                    String errorMsg = "导入ICN失败：" + icnFile.getFileName() + " - " + icnFile.getResultMessage();
                    errorMessages.add(errorMsg);
                    log.warn("跳过校验失败的ICN：{} - {}", icnFile.getFileName(), icnFile.getResultMessage());
                    continue;
                }

                importSingleIcn(icnFile, projectId, snsToNodeIdMap, request);
                icnSuccess++;
                log.info("成功导入ICN：{}", icnFile.getFileName());
            } catch (Exception e) {
                failure++;
                String errorMsg = "导入ICN失败：" + icnFile.getFileName() + " - " + e.getMessage();
                errorMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }

        // 4.3 导入资源文件（必须在DM导入后执行）
        for (ImportFileItemVO resourceFile : resourceFiles) {
            try {
                // ✅ 修复：只导入校验成功的资源文件
                if (!DmImportConstants.SUCCESS.equals(resourceFile.getResultCode())) {
                    failure++;
                    String errorMsg = "导入资源文件失败：" + resourceFile.getFileName() + " - " + resourceFile.getResultMessage();
                    errorMessages.add(errorMsg);
                    log.warn("跳过校验失败的资源文件：{} - {}", resourceFile.getFileName(), resourceFile.getResultMessage());
                    continue;
                }

                importSingleResource(resourceFile, projectId, request);
                resourceSuccess++;
                log.info("成功导入资源文件：{} -> {}", resourceFile.getFileName(), resourceFile.getAssociatedDmcCode());
            } catch (Exception e) {
                failure++;
                String errorMsg = "导入资源文件失败：" + resourceFile.getFileName() + " - " + e.getMessage();
                errorMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }

        // 5. 构建返回结果
        DmImportResultVO result = new DmImportResultVO();
        result.setDmSuccessCount(dmSuccess);
        result.setIcnSuccessCount(icnSuccess);
        result.setResourceSuccessCount(resourceSuccess);
        result.setFailureCount(failure);

        if (failure == 0) {
            result.setMessage(String.format("全部导入成功！DM: %d个, ICN: %d个, 资源: %d个",
                dmSuccess, icnSuccess, resourceSuccess));
        } else {
            result.setMessage(String.format("导入完成：成功%d个（DM:%d, ICN:%d, 资源:%d）, 失败%d个",
                (dmSuccess + icnSuccess + resourceSuccess), dmSuccess, icnSuccess, resourceSuccess, failure));
            result.setErrors(errorMessages);
        }

        log.info("导入完成：DM成功{}个, ICN成功{}个, 资源成功{}个, 失败{}个",
                dmSuccess, icnSuccess, resourceSuccess, failure);

        return result;

        } finally {
            // P2-2修复：清理临时文件（无论导入成功或失败）
            cleanupTempFiles(validFiles);
        }
    }

    // ========== 私有方法：校验相关 ==========

    /**
     * 校验单个XML文件（DM）
     */
    private ImportFileItemVO validateXmlFile(MultipartFile file, String fileName,
                                              String projectId, HttpServletRequest request) {
        ImportFileItemVO item = new ImportFileItemVO();
        item.setFileName(fileName);
        item.setFileType("DM");

        try {
            // 1. 读取XML内容
            String xmlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            item.setXmlContent(xmlContent);

            // 2. 基础XML解析校验（使用dom4j直接解析，参考DmXmlHelper内部实现）
            org.dom4j.io.SAXReader reader = createSecureXmlReader();

            org.dom4j.Document doc;
            try {
                doc = reader.read(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
                item.setResultMessage("XML解析失败：" + e.getMessage());
                return item;
            }

            // 3. 提取dmCode属性（手动提取，因为DmXmlHelper没有公开extractDmCode方法）
            Map<String, String> dmCodeAttrs = extractDmCodeFromXml(doc);
            if (dmCodeAttrs.isEmpty()) {
                item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
                item.setResultMessage("无法提取dmCode");
                return item;
            }

            // 构建DMC编码
            String dmcCode = buildDmcCode(dmCodeAttrs);
            item.setDmcCode(dmcCode);

            // 4. 执行14种校验规则
            String resultCode = validate14Rules(dmcCode, dmCodeAttrs, xmlContent, fileName,
                    projectId, request);
            item.setResultCode(resultCode);
            item.setResultMessage(DmImportConstants.getErrorMessage(resultCode));

            log.info("XML文件校验完成：文件名={}, DMC={}, 结果码={}, 结果消息={}",
                    fileName, dmcCode, resultCode, item.getResultMessage());

            // 5. 保存临时文件（用于导入阶段）
            if (DmImportConstants.SUCCESS.equals(resultCode)) {
                String tempPath = saveTempFile(file, fileName);
                item.setTempFilePath(tempPath);
            }

        } catch (Exception e) {
            log.error("校验XML文件失败：{}", fileName, e);
            item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
            item.setResultMessage("校验异常：" + e.getMessage());
        }

        return item;
    }

    /**
     * 校验ZIP文件
     * 修复P0-1：支持S1000D 4.0标准的三级目录结构（DM/、ICN/、MM/）+ 旧系统的扁平结构
     */
    private List<ImportFileItemVO> validateZipFile(MultipartFile file, String projectId,
                                                    HttpServletRequest request) throws Exception {
        List<ImportFileItemVO> items = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(file.getInputStream()), StandardCharsets.UTF_8)) {

            ZipEntry entry;
            int fileCount = 0;

            while ((entry = zis.getNextEntry()) != null) {
                // 跳过目录
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();
                fileCount++;

                // P1-6修复：ZIP炸弹防护 - 检查压缩比
                long compressedSize = entry.getCompressedSize();
                long uncompressedSize = entry.getSize();

                if (compressedSize > 0 && uncompressedSize > 0) {
                    double ratio = (double) uncompressedSize / compressedSize;
                    // ZIP炸弹防护：压缩比超过阈值视为可疑
                    if (ratio > DmImportConstants.MAX_COMPRESSION_RATIO) {
                        log.warn("检测到疑似ZIP炸弹：{}, 压缩比={}:1, 跳过", entryName, String.format("%.1f", ratio));
                        zis.closeEntry();
                        continue;
                    }
                }

                // 过滤DDN元数据文件（S1000D标准：DDN是数据交换凭证，不是数据模块）
                String fileName = new File(entryName).getName();
                if (fileName.toUpperCase().startsWith("DDN-")) {
                    log.debug("跳过DDN元数据文件：{}", entryName);
                    zis.closeEntry();
                    continue;
                }

                // 修复P0-1：判断文件类型（支持S1000D 4.0标准的三级目录结构 + 旧系统的扁平结构）
                String extension = FileNameUtils.getExtension(entryName).toLowerCase();
                boolean isXml = DmImportConstants.FILE_TYPE_XML.equals(extension);
                boolean hasImageExt = entryName.matches(".*\\.(png|jpg|jpeg|gif|bmp|svg|tif|tiff|cgm)$");

                String fileType = null;

                // 优先识别S1000D 4.0标准目录结构（DM/、ICN/、MM/）
                if (entryName.startsWith("DM/") || entryName.startsWith("dm/")) {
                    // DM/目录下的XML文件
                    if (isXml) {
                        fileType = "DM";
                    } else {
                        log.warn("DM/目录下发现非XML文件，跳过：{}", entryName);
                        zis.closeEntry();
                        continue;
                    }
                } else if (entryName.startsWith("ICN/") || entryName.startsWith("icn/")) {
                    // ICN/目录下的图片文件
                    if (hasImageExt) {
                        fileType = "ICN";
                    } else {
                        log.warn("ICN/目录下发现非图片文件，跳过：{}", entryName);
                        zis.closeEntry();
                        continue;
                    }
                } else if (entryName.startsWith("MM/") || entryName.startsWith("mm/")) {
                    // MM/目录下的资源文件
                    fileType = "RESOURCE";
                } else {
                    // 向后兼容：根目录的文件（旧系统扁平结构）
                    if (isXml) {
                        fileType = "DM";
                    } else if (hasImageExt) {
                        fileType = "ICN";
                    } else {
                        log.warn("根目录下发现不支持的文件类型，跳过：{}", entryName);
                        zis.closeEntry();
                        continue;
                    }
                }

                // 读取文件内容
                byte[] bytes = readZipEntry(zis);

                // 根据文件类型校验
                ImportFileItemVO item = null;
                if ("DM".equals(fileType)) {
                    item = validateXmlFromZip(entryName, bytes, projectId, request);
                } else if ("ICN".equals(fileType)) {
                    item = validateIcnFromZip(entryName, bytes, projectId);
                } else if ("RESOURCE".equals(fileType)) {
                    item = validateResourceFromZip(entryName, bytes, projectId);
                }

                if (item != null) {
                    items.add(item);
                }

                zis.closeEntry();
            }

            // 检查：ZIP包是否为空
            if (fileCount == 0) {
                ImportFileItemVO item = new ImportFileItemVO();
                item.setFileName(file.getOriginalFilename());
                item.setFileType("ZIP");
                item.setResultCode(DmImportConstants.ERROR_NO_FILE);
                item.setResultMessage(DmImportConstants.getErrorMessage(DmImportConstants.ERROR_NO_FILE));
                items.add(item);
            } else {
                // 日志：统计解析结果
                long dmCount = items.stream().filter(i -> "DM".equals(i.getFileType())).count();
                long icnCount = items.stream().filter(i -> "ICN".equals(i.getFileType())).count();
                long resCount = items.stream().filter(i -> "RESOURCE".equals(i.getFileType())).count();
                log.info("ZIP文件解析完成：共{}个有效文件（DM:{}, ICN:{}, 资源:{}）",
                        items.size(), dmCount, icnCount, resCount);
            }

        }

        return items;
    }

    /**
     * 从ZIP中读取条目内容（8KB缓冲区）
     */
    /**
     * 从ZIP中读取单个文件内容
     * P1-5修复：添加文件大小限制，防止OOM
     */
    private byte[] readZipEntry(ZipInputStream zis) throws IOException {
        long totalRead = 0;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[DmImportConstants.BUFFER_SIZE];
        int len;

        while ((len = zis.read(buffer)) > 0) {
            totalRead += len;

            // 检查文件大小
            if (totalRead > DmImportConstants.MAX_SINGLE_FILE_SIZE) {
                throw new IOException("文件过大，超过50MB限制。请拆分后重新导入。");
            }

            baos.write(buffer, 0, len);
        }

        return baos.toByteArray();
    }

    /**
     * 从ZIP中校验XML（DM）
     */
    private ImportFileItemVO validateXmlFromZip(String fileName, byte[] content,
                                                 String projectId, HttpServletRequest request) {
        ImportFileItemVO item = new ImportFileItemVO();
        item.setFileName(fileName);
        item.setFileType("DM");

        try {
            // 1. 解析XML
            String xmlContent = new String(content, StandardCharsets.UTF_8);
            item.setXmlContent(xmlContent);

            org.dom4j.io.SAXReader reader = createSecureXmlReader();

            org.dom4j.Document doc;
            try {
                doc = reader.read(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
                item.setResultMessage("XML解析失败");
                return item;
            }

            // 2. 提取dmCode
            Map<String, String> dmCodeAttrs = extractDmCodeFromXml(doc);
            if (dmCodeAttrs.isEmpty()) {
                item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
                item.setResultMessage("无法提取dmCode");
                return item;
            }

            String dmcCode = buildDmcCode(dmCodeAttrs);
            item.setDmcCode(dmcCode);

            // 3. 执行校验规则
            String resultCode = validate14Rules(dmcCode, dmCodeAttrs, xmlContent, fileName,
                    projectId, request);
            item.setResultCode(resultCode);
            item.setResultMessage(DmImportConstants.getErrorMessage(resultCode));

            // 4. 保存临时文件
            if (DmImportConstants.SUCCESS.equals(resultCode)) {
                String tempPath = saveTempFile(fileName, content);
                item.setTempFilePath(tempPath);
            }

        } catch (Exception e) {
            log.error("校验ZIP中XML失败：{}", fileName, e);
            item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
            item.setResultMessage("校验异常：" + e.getMessage());
        }

        return item;
    }

    /**
     * 从XML中提取dmCode属性（手动实现，因为DmXmlHelper没有公开此方法）
     */
    /**
     * 从XML中提取dmCode属性和language信息
     *
     * @param doc XML文档对象
     * @return 属性Map，包含dmCode的11个属性 + languageIsoCode + countryIsoCode
     */
    private Map<String, String> extractDmCodeFromXml(org.dom4j.Document doc) {
        Map<String, String> attrs = new HashMap<>();
        try {
            org.dom4j.Element root = doc.getRootElement();
            if (root == null) {
                log.error("XML根元素为空，无法提取dmCode");
                return attrs;
            }

            // 查找dmCode元素
            org.dom4j.Element identSection = root.element("identAndStatusSection");
            if (identSection == null) {
                log.error("缺少必需元素：identAndStatusSection");
                return attrs;
            }

            org.dom4j.Element dmAddress = identSection.element("dmAddress");
            if (dmAddress == null) {
                log.error("缺少必需元素：dmAddress");
                return attrs;
            }

            org.dom4j.Element dmIdent = dmAddress.element("dmIdent");
            if (dmIdent == null) {
                log.error("缺少必需元素：dmIdent");
                return attrs;
            }

            org.dom4j.Element dmCode = dmIdent.element("dmCode");
            if (dmCode == null) {
                log.error("缺少必需元素：dmCode");
                return attrs;
            }

            // 提取dmCode属性
            attrs.put("modelIdentCode", dmCode.attributeValue("modelIdentCode", ""));
            attrs.put("systemDiffCode", dmCode.attributeValue("systemDiffCode", ""));
            attrs.put("systemCode", dmCode.attributeValue("systemCode", ""));
            attrs.put("subSystemCode", dmCode.attributeValue("subSystemCode", ""));
            attrs.put("subSubSystemCode", dmCode.attributeValue("subSubSystemCode", ""));
            attrs.put("assyCode", dmCode.attributeValue("assyCode", ""));
            attrs.put("disassyCode", dmCode.attributeValue("disassyCode", ""));
            attrs.put("disassyCodeVariant", dmCode.attributeValue("disassyCodeVariant", ""));
            attrs.put("infoCode", dmCode.attributeValue("infoCode", ""));
            attrs.put("infoCodeVariant", dmCode.attributeValue("infoCodeVariant", ""));
            attrs.put("itemLocationCode", dmCode.attributeValue("itemLocationCode", ""));

            // 提取language信息（用于DM存在性检查）
            // 注意：language是可选元素，缺失不应导致整体失败
            try {
                org.dom4j.Element language = (org.dom4j.Element) root.selectSingleNode("//language");
                if (language != null) {
                    attrs.put("languageIsoCode", language.attributeValue("languageIsoCode", ""));
                    attrs.put("countryIsoCode", language.attributeValue("countryIsoCode", ""));
                } else {
                    log.debug("XML中未找到language元素，使用空值");
                    attrs.put("languageIsoCode", "");
                    attrs.put("countryIsoCode", "");
                }
            } catch (Exception e) {
                // language提取失败不应影响dmCode提取
                log.warn("提取language信息失败（非致命错误）", e);
                attrs.put("languageIsoCode", "");
                attrs.put("countryIsoCode", "");
            }

        } catch (NullPointerException e) {
            // 结构性问题：必需的XML元素缺失
            log.error("XML结构不符合S1000D标准，缺少必需元素", e);
        } catch (Exception e) {
            // 其他未预期的异常
            log.error("提取dmCode属性时发生未预期异常", e);
        }
        return attrs;
    }

    /**
     * 构建DM存在性检查的查询条件（校验和导入共用）
     * <p>
     * 数据库唯一约束基于6个字段：
     * sns + info_code + info_code_variant + ietm_location_code + language_iso_code + country_iso_code + is_latest='1'
     *
     * @param projectId 项目ID
     * @param sns SNS编码
     * @param infoCode 信息代码
     * @param infoCodeVariant 信息代码变体（可选）
     * @param itemLocationCode 位置代码（可选）
     * @param languageIsoCode 语言代码（可选）
     * @param countryIsoCode 国家代码（可选）
     * @return 查询条件
     */
    private QueryWrapper<IetmDataModule> buildDmExistCheckQuery(
            String projectId, String sns, String infoCode,
            String infoCodeVariant, String itemLocationCode,
            String languageIsoCode, String countryIsoCode) {

        QueryWrapper<IetmDataModule> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId);
        qw.eq("sns", sns != null ? sns : "");
        qw.eq("info_code", infoCode);

        // 可选字段：用 isNull 或 eq 处理
        if (infoCodeVariant != null && !infoCodeVariant.isEmpty()) {
            qw.eq("info_code_variant", infoCodeVariant);
        } else {
            qw.and(w -> w.isNull("info_code_variant").or().eq("info_code_variant", ""));
        }

        if (itemLocationCode != null && !itemLocationCode.isEmpty()) {
            qw.eq("ietm_location_code", itemLocationCode);
        } else {
            qw.and(w -> w.isNull("ietm_location_code").or().eq("ietm_location_code", ""));
        }

        if (languageIsoCode != null && !languageIsoCode.isEmpty()) {
            qw.eq("language_iso_code", languageIsoCode);
        } else {
            qw.and(w -> w.isNull("language_iso_code").or().eq("language_iso_code", ""));
        }

        if (countryIsoCode != null && !countryIsoCode.isEmpty()) {
            qw.eq("country_iso_code", countryIsoCode);
        } else {
            qw.and(w -> w.isNull("country_iso_code").or().eq("country_iso_code", ""));
        }

        qw.eq("is_latest", "1");  // 只查最新版本
        qw.eq("status", "1");     // 只查有效记录（未逻辑删除）

        return qw;
    }

    /**
     * 检查DM是否存在（通用6字段组合查询）
     * 【P1-1修复】消除代码重复，统一DM存在性检查逻辑
     *
     * @param projectId 项目ID
     * @param dmCodeAttrs dmCode属性Map（包含8个SNS字段）
     * @param infoCode 信息代码
     * @param infoCodeVariant 信息代码变体
     * @param itemLocationCode 位置代码
     * @param languageIsoCode 语言代码
     * @param countryIsoCode 国家代码
     * @return true-存在，false-不存在
     */
    private boolean isDmExistsBySixFields(String projectId,
                                           Map<String, String> dmCodeAttrs,
                                           String infoCode,
                                           String infoCodeVariant,
                                           String itemLocationCode,
                                           String languageIsoCode,
                                           String countryIsoCode) {
        // 1. 构建SNS
        String sns = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmcUtils.composeSns(
            safeStr(dmCodeAttrs.get("modelIdentCode")),
            safeStr(dmCodeAttrs.get("systemDiffCode")),
            safeStr(dmCodeAttrs.get("systemCode")),
            safeStr(dmCodeAttrs.get("subSystemCode")),
            safeStr(dmCodeAttrs.get("subSubSystemCode")),
            safeStr(dmCodeAttrs.get("assyCode")),
            safeStr(dmCodeAttrs.get("disassyCode")),
            safeStr(dmCodeAttrs.get("disassyCodeVariant"))
        );

        // 2. 构建6字段查询条件
        QueryWrapper<IetmDataModule> qw = buildDmExistCheckQuery(
            projectId, sns, infoCode, infoCodeVariant,
            itemLocationCode, languageIsoCode, countryIsoCode
        );

        // 3. 执行查询
        long count = dataModuleService.count(qw);

        log.debug("DM存在性检查（6字段组合）：projectId={}, sns={}, infoCode={}, " +
                 "infoCodeVariant={}, itemLocationCode={}, languageIsoCode={}, countryIsoCode={}, 结果={}",
                 projectId, sns, infoCode, infoCodeVariant,
                 itemLocationCode, languageIsoCode, countryIsoCode, count > 0);

        return count > 0;
    }

    /**
     * 执行14种校验规则（核心逻辑 - 完整版）
     */
    private String validate14Rules(String dmcCode, Map<String, String> dmCodeAttrs,
                                    String xmlContent, String fileName,
                                    String projectId, HttpServletRequest request) {
        try {
            log.info("开始执行14种校验规则：文件名={}, DMC={}, projectId={}", fileName, dmcCode, projectId);

            // 规则-1：DM是否已存在
            // 【关键修复】：必须与导入时的检查逻辑保持一致，使用 6 字段组合而非简单的 dmc_code
            // 数据库唯一约束基于：sns + info_code + info_code_variant + ietm_location_code + language_iso_code + country_iso_code + is_latest='1'

            // 1.1 提取必需字段
            String infoCode = safeStr(dmCodeAttrs.get("infoCode"));
            if (infoCode == null || infoCode.isEmpty()) {
                infoCode = "000";
            }

            // 1.2 提取可选字段（已在extractDmCodeFromXml中提取，直接使用）
            String infoCodeVariant = safeStr(dmCodeAttrs.get("infoCodeVariant"));
            String itemLocationCode = safeStr(dmCodeAttrs.get("itemLocationCode"));
            String languageIsoCode = safeStr(dmCodeAttrs.get("languageIsoCode"));
            String countryIsoCode = safeStr(dmCodeAttrs.get("countryIsoCode"));

            // 1.3 使用公共方法检查DM是否存在（P1-1修复：消除代码重复）
            if (isDmExistsBySixFields(projectId, dmCodeAttrs, infoCode,
                                      infoCodeVariant, itemLocationCode,
                                      languageIsoCode, countryIsoCode)) {
                log.warn("DM已存在，无法导入：DMC={}, infoCode={}, projectId={}, 文件名={}",
                        dmcCode, infoCode, projectId, fileName);
                return DmImportConstants.ERROR_DM_EXISTS;  // -1
            }

            // 规则-2：SNS是否在构型中
            // ✅ 正确逻辑：构型表的path字段存储8段格式（每段用"-"连接）
            // 从dmCode的8个属性构建path，匹配构型表的path字段
            if (!isDmCodePathInConfiguration(dmCodeAttrs, projectId)) {
                log.warn("SNS不在构型中：dmCodeAttrs={}, projectId={}, 文件名={}",
                         dmCodeAttrs, projectId, fileName);
                return DmImportConstants.ERROR_SNS_NOT_IN_CM;  // -2
            }

            // 规则-4：文件名与DM内容编码是否一致
            String fileBaseName = FilenameUtils.getBaseName(fileName);
            // 提取纯文件名（去除路径）
            if (fileBaseName.contains("/")) {
                fileBaseName = fileBaseName.substring(fileBaseName.lastIndexOf("/") + 1);
            }

            // ⚠️ 关键修复：文件名可能包含版本和语言后缀
            // 格式：DMC-{sns}-{info}{infoVar}-{loc}_{issueNo}-{inWork}_{lang}-{country}
            // 示例：DMC-ZB1-A-00-00-00-00A-007A-A_003-00_zh-CN.xml
            // 需要提取基础DMC部分（去掉版本和语言后缀）进行对比
            String fileBaseDmc = extractBaseDmcFromFileName(fileBaseName);

            if (!fileBaseDmc.equals(dmcCode)) {
                log.warn("文件名与DM内容编码不一致：文件名基础DMC={}, XML内部DMC={}, 原始文件名={}",
                        fileBaseDmc, dmcCode, fileBaseName);
                return DmImportConstants.ERROR_CODE_MISMATCH;  // -4
            }

            // 规则-5：型号是否匹配
            String modelCode = dmCodeAttrs.get("modelIdentCode");
            // 【P0-1修复】从Redis获取项目modelCode，避免Session伪造风险
            String projectModelCode = getProjectModelCodeFromRedis();
            if (projectModelCode != null && !projectModelCode.isEmpty()
                && modelCode != null && !modelCode.equals(projectModelCode)) {
                return DmImportConstants.ERROR_MODEL_MISMATCH;  // -5
            }

            // 规则-6：密级值是否存在（检查是否为0-5范围）
            // 注意：XML中密级值为两位数字格式（01, 02, 03, 04, 05）或单位数字格式（0, 1, 2, 3, 4, 5）
            String security = extractSecurityFromXml(xmlContent);
            if (security != null && !security.isEmpty()) {
                // 支持两种格式：单位数 "0"-"5" 或两位数 "01"-"05"
                if (!security.matches("^0?[0-5]$")) {
                    return DmImportConstants.ERROR_SECURITY_NOT_EXISTS;  // -6
                }

                // 规则-7：密级是否超限（检查用户权限）
                String userMaxSecurity = (String) request.getSession().getAttribute("userMaxSecurity");
                if (userMaxSecurity != null && !userMaxSecurity.isEmpty()) {
                    try {
                        int dmSecurityLevel = Integer.parseInt(security);
                        int userSecurityLevel = Integer.parseInt(userMaxSecurity);
                        if (dmSecurityLevel > userSecurityLevel) {
                            return DmImportConstants.ERROR_SECURITY_EXCEED;  // -7
                        }
                    } catch (NumberFormatException e) {
                        log.warn("密级格式错误：dm={}, user={}", security, userMaxSecurity);
                    }
                }
            }

            // 规则-3：DDN文件列表校验（二期功能，当前版本暂不实施）
            // DDN校验需要DDN模块提供接口支持，已在二期需求中规划

            // 全部通过
            return DmImportConstants.SUCCESS;  // 1

        } catch (Exception e) {
            log.error("校验规则执行失败", e);
            return DmImportConstants.ERROR_UNKNOWN;  // -10
        }
    }

    /**
     * 从dmCode属性构建8段path格式（对齐构型表的path字段）
     *
     * 构型表的path字段格式：{model}-{sysDiff}-{sys}-{subSys}-{subSub}-{assy}-{disassy}-{disassyVar}
     * 例如：ZBBM33-D-01-A-1-00-00-A
     *
     * 注意：这与DmcUtils.composeSns()生成的6段SNS格式不同
     * SNS格式：{model}-{sysDiff}-{sys}-{subSys+subSub}-{assy}-{disassy+disassyVar}
     * 例如：ZBBM33-D-01-A1-00-00A
     */
    private String buildPathFromDmCode(Map<String, String> dmCodeAttrs) {
        StringBuilder path = new StringBuilder();

        // 按构型树的8段结构拼接
        String[] segments = {
            safeStr(dmCodeAttrs.get("modelIdentCode")),      // [0] model
            safeStr(dmCodeAttrs.get("systemDiffCode")),      // [1] systemDiff
            safeStr(dmCodeAttrs.get("systemCode")),          // [2] systemCode (2位)
            safeStr(dmCodeAttrs.get("subSystemCode")),       // [3] subSystem (1位)
            safeStr(dmCodeAttrs.get("subSubSystemCode")),    // [4] subSubSystem (1位)
            safeStr(dmCodeAttrs.get("assyCode")),            // [5] assy (2位)
            safeStr(dmCodeAttrs.get("disassyCode")),         // [6] disassy (2位)
            safeStr(dmCodeAttrs.get("disassyCodeVariant"))   // [7] disassyVar (1位)
        };

        // 用"-"连接所有段
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                path.append("-");
            }
            path.append(segments[i]);
        }

        return path.toString();
    }

    /**
     * 检查dmCode对应的path是否在项目构型中
     *
     * ✅ 正确的校验逻辑：
     * 1. 从dmCode的8个属性构建8段path（每段用"-"连接）
     * 2. 匹配构型表的path字段
     * 3. path字段存储的是完整的构型路径，可以直接匹配
     */
    private boolean isDmCodePathInConfiguration(Map<String, String> dmCodeAttrs, String projectId) {
        String path = buildPathFromDmCode(dmCodeAttrs);

        if (path == null || path.isEmpty()) {
            return false;
        }

        // 直接匹配构型表的path字段
        QueryWrapper<IetmProjectConfigurationManagement> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId);
        qw.eq("path", path);  // ✅ 匹配8段path格式
        long count = configurationService.count(qw);

        log.debug("构型路径校验：path={}, projectId={}, 查询结果={}", path, projectId, count);

        return count > 0;
    }

    /**
     * 校验SNS是否在构型树中存在
     * 使用SNS映射表查询，对齐旧系统实现
     *
     * @param sns SNS路径（如：ZB1-A-05-00-00-00A）
     * @param projectId 项目ID
     * @return true=存在，false=不存在
     */
    private boolean isSnsInConfiguration(String sns, String projectId) {
        if (sns == null || sns.isEmpty()) {
            return false;
        }

        // 使用SNS映射表查询
        Map<String, String> snsMap = buildSnsToNodeIdMap(projectId);
        boolean exists = snsMap.containsKey(sns);

        log.debug("校验SNS是否存在：sns={}, projectId={}, 结果={}", sns, projectId, exists);

        return exists;
    }

    /**
     * 构建SNS到cmNodeId的映射表
     * 对齐旧系统的 getProjectCmPath() 方法
     *
     * @param projectId 项目ID
     * @return Map<SNS路径, cmNodeId>
     */
    private Map<String, String> buildSnsToNodeIdMap(String projectId) {
        Map<String, String> snsMap = new HashMap<>();

        // 1. 查询项目所有构型节点
        QueryWrapper<IetmProjectConfigurationManagement> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId);
        qw.orderByAsc("seq");
        List<IetmProjectConfigurationManagement> cmList = configurationService.list(qw);

        if (cmList == null || cmList.isEmpty()) {
            log.warn("项目[{}]没有构型节点", projectId);
            return snsMap;
        }

        // 2. 为每个节点构建完整SNS路径
        for (IetmProjectConfigurationManagement cm : cmList) {
            String snsPath = buildFullSnsPath(cm, cmList);
            if (snsPath != null && !snsPath.isEmpty()) {
                snsMap.put(snsPath, cm.getId());
                log.debug("构型节点映射：SNS={} -> nodeId={}", snsPath, cm.getId());
            }
        }

        log.info("项目[{}]构建SNS映射表完成，共{}个节点", projectId, snsMap.size());

        return snsMap;
    }

    /**
     * 构建节点的完整SNS路径（递归获取父节点code）
     * 对齐旧系统算法
     *
     * @param node 当前节点
     * @param allNodes 所有节点列表
     * @return 完整SNS路径（如：ZB1-A-05-00-00-00A）
     */
    private String buildFullSnsPath(IetmProjectConfigurationManagement node,
                                     List<IetmProjectConfigurationManagement> allNodes) {
        if (node == null || node.getCode() == null || node.getCode().isEmpty()) {
            return null;
        }

        List<String> pathParts = new ArrayList<>();
        pathParts.add(node.getCode());

        // 递归查找父节点
        String currentPid = node.getPid();
        while (currentPid != null && !"0".equals(currentPid)) {
            IetmProjectConfigurationManagement parent = findNodeById(currentPid, allNodes);
            if (parent == null) {
                break;
            }
            pathParts.add(parent.getCode());
            currentPid = parent.getPid();
        }

        // 反转列表（从根到叶）
        Collections.reverse(pathParts);

        // 拼接成SNS路径
        return String.join("-", pathParts);
    }

    /**
     * 从节点列表中查找指定ID的节点
     */
    private IetmProjectConfigurationManagement findNodeById(String nodeId,
                                                             List<IetmProjectConfigurationManagement> nodes) {
        if (nodeId == null || nodes == null) {
            return null;
        }

        for (IetmProjectConfigurationManagement node : nodes) {
            if (nodeId.equals(node.getId())) {
                return node;
            }
        }

        return null;
    }

    // ========== 私有方法：工具函数 ==========

    /**
     * 从Redis获取当前项目ID（对齐openProject的设计）
     */
    private String getProjectIdFromRedis() {
        try {
            // 获取当前登录用户
            LoginUser sysUser = getCurrentLoginUser();

            // 从Redis获取项目信息
            String redisKey = "ietm:current_project:" + sysUser.getId();
            Object projectInfoObj = redisTemplate.opsForValue().get(redisKey);

            if (projectInfoObj == null) {
                log.warn("用户[{}]未打开项目，Redis Key: {}", sysUser.getUsername(), redisKey);
                return null;
            }

            // 提取projectId
            if (projectInfoObj instanceof Map) {
                Map<String, Object> projectInfo = (Map<String, Object>) projectInfoObj;
                Object projectIdObj = projectInfo.get("projectId");
                if (projectIdObj != null) {
                    return projectIdObj.toString();
                }
            }

            log.warn("Redis中的项目信息格式异常: {}", projectInfoObj.getClass().getName());
            return null;

        } catch (Exception e) {
            log.error("从Redis获取项目ID失败", e);
            return null;
        }
    }

    /**
     * 从Redis获取项目的equipmentCode（modelIdentCode）
     * 【P0-1修复】避免Session伪造风险，统一使用Shiro+Redis获取项目信息
     *
     * @return equipmentCode（对应DMC中的modelIdentCode），失败返回null
     */
    private String getProjectModelCodeFromRedis() {
        try {
            // 获取当前登录用户
            LoginUser sysUser = getCurrentLoginUser();

            // 从Redis获取项目信息
            String redisKey = "ietm:current_project:" + sysUser.getId();
            Object projectInfoObj = redisTemplate.opsForValue().get(redisKey);

            if (projectInfoObj == null) {
                log.warn("用户[{}]未打开项目，Redis Key: {}", sysUser.getUsername(), redisKey);
                return null;
            }

            // 提取equipmentCode
            if (projectInfoObj instanceof Map) {
                Map<String, Object> projectInfo = (Map<String, Object>) projectInfoObj;
                Object equipmentCode = projectInfo.get("equipmentCode");
                if (equipmentCode != null) {
                    return equipmentCode.toString();
                }
            }

            log.warn("Redis中的项目信息格式异常: {}", projectInfoObj.getClass().getName());
            return null;

        } catch (Exception e) {
            log.error("从Redis获取项目modelCode失败", e);
            return null;
        }
    }

    /**
     * 提取文件名（兼容IE浏览器全路径）
     */
    private String extractFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "unknown";
        }

        // 兼容IE浏览器：C:\path\to\file.xml → file.xml
        if (originalFilename.contains("\\")) {
            return originalFilename.substring(originalFilename.lastIndexOf("\\") + 1);
        }

        return originalFilename;
    }

    /**
     * 构建基础DMC编码（从dmCode属性）
     *
     * ⚠️ 最大程度复用已有代码：
     * 1. 使用DmcUtils.composeSns()构建SNS（与新建DM功能完全一致）
     * 2. 拼接格式对标generateDmc()的前半部分
     *
     * 基础DMC格式：DMC-{sns}-{infoCode}{infoCodeVariant}-{itemLocationCode}
     * 完整DMC格式：DMC-{sns}-{infoCode}{infoCodeVariant}-{itemLocationCode}_{issueNo}-{inWork}_{lang}-{country}
     *
     * @param dmCodeAttrs dmCode元素的属性Map
     * @return 基础DMC编码字符串（不含版本和语言后缀）
     */
    private String buildDmcCode(Map<String, String> dmCodeAttrs) {
        // 提取dmCode的11个属性
        String modelIdentCode = safeStr(dmCodeAttrs.get("modelIdentCode"));
        String systemDiffCode = safeStr(dmCodeAttrs.get("systemDiffCode"));
        String systemCode = safeStr(dmCodeAttrs.get("systemCode"));
        String subSystemCode = safeStr(dmCodeAttrs.get("subSystemCode"));
        String subSubSystemCode = safeStr(dmCodeAttrs.get("subSubSystemCode"));
        String assyCode = safeStr(dmCodeAttrs.get("assyCode"));
        String disassyCode = safeStr(dmCodeAttrs.get("disassyCode"));
        String disassyCodeVariant = safeStr(dmCodeAttrs.get("disassyCodeVariant"));
        String infoCode = safeStr(dmCodeAttrs.get("infoCode"));
        String infoCodeVariant = safeStr(dmCodeAttrs.get("infoCodeVariant"));
        String itemLocationCode = safeStr(dmCodeAttrs.get("itemLocationCode"));

        // ✅ 复用DmcUtils.composeSns()构建SNS（与新建DM功能完全一致）
        String sns = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmcUtils.composeSns(
            modelIdentCode, systemDiffCode, systemCode,
            subSystemCode, subSubSystemCode,
            assyCode, disassyCode, disassyCodeVariant
        );

        // 构建基础DMC（对标generateDmc的格式，但不含版本和语言）
        // 格式：DMC-{sns}-{infoCode}{infoCodeVariant}-{itemLocationCode}
        String baseDmc = "DMC-" + sns + "-" + infoCode + infoCodeVariant;

        // ⚠️ 重要设计决策：不添加默认值
        // 原因：
        // 1. 导入校验的目的是验证XML与文件名是否一致
        // 2. 应该以XML的实际内容为准，不应该添加默认值
        // 3. generateDmc()的默认值（loc="A"）是为了新建DM时的便利性
        // 4. 旧数据中itemLocationCode可能真的为空，强制添加默认值会导致校验失败
        //
        // 示例：
        // - 文件名：DMC-ZB1-A-00-00-00A-007A-A（最后的A是infoCode）
        // - XML中：itemLocationCode为空
        // - 如果添加默认"A"：生成DMC-ZB1-A-00-00-00A-007A-A-A（多一个-A）
        // - 校验结果：不匹配 ❌
        //
        // 因此，只有当XML中itemLocationCode不为空时，才拼接
        if (!itemLocationCode.isEmpty()) {
            baseDmc += "-" + itemLocationCode;
        }

        return baseDmc;
    }

    /**
     * 安全获取字符串（null转为空字符串，与DmXmlHelper.safeStr()逻辑一致）
     */
    private String safeStr(String s) {
        return s != null ? s : "";
    }

    /**
     * 从文件名提取基础DMC编码（去除版本和语言后缀）
     *
     * DMC文件名格式（对标generateDmc方法）：
     * DMC-{sns}-{infoCode}{infoCodeVariant}-{itemLocationCode}_{issueNo}-{inWork}_{lang}-{country}
     *
     * 示例：
     * - DMC-ZB1-A-00-00-00-00A-007A-A_003-00_zh-CN.xml
     * - DMC-A-00-00-00-00A-040A-A-00-00A-A_001-00_zh-CN.xml
     *
     * 提取基础DMC（11段，不含版本/语言）：
     * - DMC-ZB1-A-00-00-00-00A-007A-A
     * - DMC-A-00-00-00-00A-040A-A-00-00A-A
     *
     * @param fileName 文件名（已去除.xml扩展名）
     * @return 基础DMC编码（不含版本和语言后缀）
     */
    private String extractBaseDmcFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        // 如果文件名不包含下划线，说明没有版本/语言后缀，直接返回
        if (!fileName.contains("_")) {
            return fileName;
        }

        // 找到第一个下划线位置，之前的部分是基础DMC
        // DMC-{sns}-{info}{infoVar}-{loc}_{issueNo}-{inWork}_{lang}-{country}
        //                                  ↑ 第一个下划线
        int firstUnderscoreIndex = fileName.indexOf("_");
        String baseDmc = fileName.substring(0, firstUnderscoreIndex);

        log.debug("从文件名提取基础DMC：原始={}, 基础DMC={}", fileName, baseDmc);
        return baseDmc;
    }

    /**
     * 保存临时文件（MultipartFile版本）
     */
    private String saveTempFile(MultipartFile file, String fileName) throws IOException {
        File tempDir = new File(fileStorageLocation, "temp/import");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        String safeFileName = FileNameUtils.sanitize(fileName);
        File tempFile = new File(tempDir, System.currentTimeMillis() + "_" + safeFileName);

        // 路径遍历防护
        Path targetPath = tempFile.toPath().toAbsolutePath().normalize();
        Path basePath = tempDir.toPath().toAbsolutePath().normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new JeecgBootException("文件路径非法");
        }

        file.transferTo(tempFile);
        return tempFile.getAbsolutePath();
    }

    /**
     * 保存临时文件（字节数组版本）
     */
    private String saveTempFile(String fileName, byte[] content) throws IOException {
        File tempDir = new File(fileStorageLocation, "temp/import");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        String safeFileName = FileNameUtils.sanitize(fileName);
        File tempFile = new File(tempDir, System.currentTimeMillis() + "_" + safeFileName);

        FileUtils.writeByteArrayToFile(tempFile, content);
        return tempFile.getAbsolutePath();
    }

    /**
     * 从XML中提取密级（从securityClassification元素）
     */
    private String extractSecurityFromXml(String xmlContent) {
        try {
            org.dom4j.io.SAXReader reader = createSecureXmlReader();

            org.dom4j.Document doc = reader.read(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            org.dom4j.Element root = doc.getRootElement();

            // 查找security元素
            org.dom4j.Element identAndStatus = root.element("identAndStatusSection");
            if (identAndStatus != null) {
                org.dom4j.Element dmStatus = identAndStatus.element("dmStatus");
                if (dmStatus != null) {
                    org.dom4j.Element security = dmStatus.element("security");
                    if (security != null) {
                        return security.attributeValue("securityClassification", "");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取密级失败", e);
        }
        return null;
    }

    // ========== ICN校验相关方法 ==========

    /**
     * 从ZIP中校验ICN文件
     *
     * 实现规则：
     * - 规则-11：ICN文件名格式校验
     * - 规则-12：ICN的SNS是否在构型中
     * - 规则-13：ICN是否已存在
     */
    private ImportFileItemVO validateIcnFromZip(String fileName, byte[] content, String projectId) {
        ImportFileItemVO item = new ImportFileItemVO();
        item.setFileName(fileName);
        item.setFileType("ICN");

        try {
            // 规则-11：ICN文件名格式校验
            if (!isValidIcnFileName(fileName)) {
                item.setResultCode(DmImportConstants.ERROR_ICN_NAME_INVALID);
                item.setResultMessage(DmImportConstants.getErrorMessage(DmImportConstants.ERROR_ICN_NAME_INVALID));
                return item;
            }

            // 提取ICN编码（从文件名）
            String icnCode = extractIcnCode(fileName);

            // 规则-13：ICN是否已存在
            QueryWrapper<IetmIcnManage> qw = new QueryWrapper<>();
            qw.eq("icn", icnCode);  // ⚠️ 注意：字段名是icn，不是icn_code
            long count = icnManageService.count(qw);
            if (count > 0) {
                item.setResultCode(DmImportConstants.ERROR_ICN_EXISTS);
                item.setResultMessage(DmImportConstants.getErrorMessage(DmImportConstants.ERROR_ICN_EXISTS));
                return item;
            }

            // 规则-12：ICN的SNS是否在构型中
            String icnSns = extractSnsFromIcnCode(icnCode);
            if (icnSns != null && !icnSns.isEmpty()
                && !isSnsInConfiguration(icnSns, projectId)) {
                item.setResultCode(DmImportConstants.ERROR_ICN_SNS_NOT_IN_CM);
                item.setResultMessage(DmImportConstants.getErrorMessage(DmImportConstants.ERROR_ICN_SNS_NOT_IN_CM));
                return item;
            }

            // 保存临时文件
            String tempPath = saveTempFile(fileName, content);
            item.setTempFilePath(tempPath);

            // 全部通过
            item.setResultCode(DmImportConstants.SUCCESS);
            item.setResultMessage(DmImportConstants.getErrorMessage(DmImportConstants.SUCCESS));

        } catch (Exception e) {
            log.error("校验ICN文件失败：{}", fileName, e);
            item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
            item.setResultMessage("校验异常：" + e.getMessage());
        }

        return item;
    }

    /**
     * 从ZIP中校验资源文件（DM资源）
     *
     * 资源文件命名规范：{DMC前缀}_{原始文件名}
     * 例如：DMC-XXX-XXX-XXX_资源文件.pdf
     */
    private ImportFileItemVO validateResourceFromZip(String entryName, byte[] content, String projectId) {
        ImportFileItemVO item = new ImportFileItemVO();
        String fileName = new File(entryName).getName();
        item.setFileName(fileName);
        item.setFileType("RESOURCE");
        item.setFileSize((long) content.length);

        try {
            // 提取关联的DMC编码（从文件名前缀）
            String dmcPrefix = extractDmcPrefixFromResourceName(fileName);
            if (dmcPrefix == null || dmcPrefix.isEmpty()) {
                item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
                item.setResultMessage("无法从资源文件名提取DMC前缀");
                return item;
            }

            item.setAssociatedDmcCode(dmcPrefix);

            // 【关键修复】校验：关联的DM是否存在（对齐导入逻辑，提前发现问题）
            // 注意：此处检查的是**已存在的DM**，如果DM在同一个ZIP包中但还未导入，会提示失败
            // 这是预期行为：资源文件应该在关联的DM导入成功后再导入
            // 【P0修复】使用6字段组合查询，与DM校验逻辑完全一致
            log.info("【调试】开始校验资源文件：fileName={}, dmcPrefix={}, projectId={}", fileName, dmcPrefix, projectId);
            IetmDataModule dm = findDmForResource(fileName, projectId);
            if (dm == null) {
                item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
                item.setResultMessage("关联的DM不存在：" + dmcPrefix + "（请先导入对应的DM文件）");
                log.info("资源文件校验失败：关联的DM不存在，dmcPrefix={}, projectId={}, fileName={}",
                         dmcPrefix, projectId, fileName);
                return item;
            }
            log.info("【调试】找到关联DM：dmId={}, dmcCode={}", dm.getId(), dm.getDmcCode());

            // 【P0关键修复】先保存临时文件（无论是否重复，都需要tempFilePath用于显示）
            log.info("【调试】开始保存临时文件：fileName={}", fileName);
            String tempPath = saveTempFile(fileName, content);
            item.setTempFilePath(tempPath);
            log.info("【调试】临时文件已保存：tempPath={}", tempPath);

            // 【P0修复-3】资源文件重复性检查（对齐DM和ICN的处理逻辑）
            // 【P0修复-5】使用智能算法提取原始文件名，支持多种格式
            String originalFileName = extractOriginalFileName(fileName);
            log.info("【调试】开始重复检查：dmId={}, fileName={}, originalFileName={}",
                    dm.getId(), fileName, originalFileName);
            QueryWrapper<IetmDmComment> qw = new QueryWrapper<>();
            qw.eq("dm_id", dm.getId());
            qw.eq("file_name", originalFileName);
            long count = dmCommentMapper.selectCount(qw);
            log.info("【调试】重复检查结果：count={}", count);
            if (count > 0) {
                item.setResultCode(DmImportConstants.ERROR_RESOURCE_EXISTS);
                item.setResultMessage("资源文件已存在：" + originalFileName + "（关联DM：" + dmcPrefix + "）");
                log.info("资源文件校验失败：资源已存在，dmId={}, fileName={}, projectId={}",
                    dm.getId(), originalFileName, projectId);
                return item;
            }

            // 全部通过
            item.setResultCode(DmImportConstants.SUCCESS);
            item.setResultMessage(DmImportConstants.getErrorMessage(DmImportConstants.SUCCESS));

        } catch (Exception e) {
            log.error("校验资源文件失败：{}", fileName, e);
            item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
            item.setResultMessage("校验异常：" + e.getMessage());
        }

        return item;
    }

    /**
     * 从资源文件名中提取DMC前缀
     *
     * 文件名格式：{DMC前缀}_{原始文件名}
     * 例如：DMC-XXX-XXX-XXX_资源文件.pdf -> DMC-XXX-XXX-XXX
     */
    private String extractDmcPrefixFromResourceName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        // 查找第一个下划线的位置
        int underscoreIndex = fileName.indexOf('_');
        if (underscoreIndex > 0) {
            return fileName.substring(0, underscoreIndex);
        }

        // 如果没有下划线，返回null
        return null;
    }

    /**
     * 从资源文件名中智能提取原始文件名
     *
     * 支持格式：
     * 1. 标准格式：DMC-XXX_原始名.扩展名 → 原始名.扩展名
     * 2. 带版本格式：DMC-XXX_版本_语言_原始名.扩展名 → 原始名.扩展名
     * 3. 带ID格式：原始名_雪花ID1_雪花ID2.扩展名 → 原始名.扩展名
     * 4. 复杂格式：DMC-XXX_001-03_zh-CN_data-05_analysis.pdf → data-05_analysis.pdf
     * 5. 多段文件名：DMC-XXX_test_file_1234567890123.jpg → test_file.jpg
     * 6. 极端情况：DMC-XXX_001-03_zh-CN_en-US_report.pdf → en-US_report.pdf
     *
     * 解析策略：
     * - 从前向后找到第一个原始文件名段的起始位置
     * - 从后向前找到最后一个非雪花ID段的结束位置
     * - 拼接中间所有段（保留下划线）
     * - 【P1修复】支持原始文件名本身包含下划线的情况
     * - 【P1修复】元数据只识别一次（避免误判用户文件名如 en-US_report.pdf）
     *
     * @param fileName 完整文件名
     * @return 原始文件名
     */
    private String extractOriginalFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        // 1. 移除DMC/ICN前缀
        int firstUnderscore = fileName.indexOf('_');
        if (firstUnderscore < 0) {
            return fileName;
        }
        String withoutPrefix = fileName.substring(firstUnderscore + 1);

        // 2. 提取扩展名
        int lastDot = withoutPrefix.lastIndexOf('.');
        String extension = (lastDot > 0) ? withoutPrefix.substring(lastDot) : "";
        String nameWithoutExt = (lastDot > 0) ? withoutPrefix.substring(0, lastDot) : withoutPrefix;

        // 3. 按下划线分段
        String[] segments = nameWithoutExt.split("_");
        if (segments.length == 0) {
            return fileName;
        }

        // 4. 从前向后找到第一个原始文件名段的起始位置
        int startIdx = -1;
        boolean foundVersion = false;
        boolean foundLanguage = false;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];

            // 跳过雪花ID
            if (segment.matches(SNOWFLAKE_ID_PATTERN)) {
                continue;
            }

            // 跳过元数据（版本号、语言代码）
            // 元数据通常在前3段内，且每种类型只出现一次
            if (i < 3) {
                if (!foundVersion && segment.matches(VERSION_PATTERN)) {
                    foundVersion = true;
                    log.debug("跳过版本号段：position={}, segment={}", i, segment);
                    continue;
                }
                if (!foundLanguage && segment.matches(LANGUAGE_PATTERN)) {
                    foundLanguage = true;
                    log.debug("跳过语言代码段：position={}, segment={}", i, segment);
                    continue;
                }
            }

            // 找到第一个原始文件名段
            startIdx = i;
            log.debug("找到原始文件名起始段：position={}, segment={}", i, segment);
            break;
        }

        if (startIdx < 0) {
            // 所有段都是元数据或雪花ID，返回最后一段
            log.debug("所有段都是元数据，使用兜底策略");
            return segments[segments.length - 1] + extension;
        }

        // 5. 从后向前找到最后一个非雪花ID段的结束位置
        int endIdx = segments.length - 1;
        for (int i = segments.length - 1; i >= startIdx; i--) {
            String segment = segments[i];

            // 跳过雪花ID
            if (segment.matches(SNOWFLAKE_ID_PATTERN)) {
                log.debug("跳过尾部雪花ID段：position={}, segment={}", i, segment);
                continue;
            }

            // 找到最后一个非雪花ID段
            endIdx = i;
            log.debug("找到原始文件名结束段：position={}, segment={}", i, segment);
            break;
        }

        // 6. 拼接原始文件名（保留中间的下划线）
        StringBuilder originalName = new StringBuilder();
        for (int i = startIdx; i <= endIdx; i++) {
            if (i > startIdx) {
                originalName.append("_");
            }
            originalName.append(segments[i]);
        }

        String result = originalName.toString() + extension;
        log.debug("最终提取结果：startIdx={}, endIdx={}, result={}", startIdx, endIdx, result);
        return result;
    }

    /**
     * 检查字符串是否包含中文字符
     */
    private boolean containsChinese(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches(".*[\\u4e00-\\u9fa5]+.*");
    }

    /**
     * 检查文件扩展名是否在允许的白名单中
     *
     * 【P0修复】新增方法，防止恶意文件上传
     *
     * @param extension 文件扩展名（含点，如 ".pdf"）
     * @return true-允许，false-不允许
     */
    /**
     * 检查DM是否存在
     * 【P2修复】提取公共方法，消除代码重复
     *
     * @param dmcCode DM编码
     * @param projectId 项目ID
     * @return true-存在，false-不存在
     */
    private boolean isDmExists(String dmcCode, String projectId) {
        QueryWrapper<IetmDataModule> qw = new QueryWrapper<>();
        qw.eq("dmc_code", dmcCode);
        qw.eq("project_id", projectId);
        qw.eq("status", "1");  // 只查有效记录
        return dataModuleService.count(qw) > 0;
    }

    /**
     * 从DMC字符串解析各个字段
     * 【P0修复】用于资源文件校验，确保与DM校验逻辑一致
     *
     * DMC格式：DMC-{sns}-{infoCode}{infoCodeVariant}-{itemLocationCode}
     * SNS格式：modelIdentCode-systemDiffCode-systemCode-subSystemCode+subSubSystemCode-assyCode-disassyCode+disassyCodeVariant
     *
     * 示例1（无itemLocationCode）：DMC-ZB1-A-05-00-00-00A-007A-A
     *   - SNS: ZB1-A-05-00-00-00A-007A-A (8段)
     *   - infoCode: "A" (实际是disassyCodeVariant，需要特殊处理)
     *
     * 示例2（有itemLocationCode）：DMC-ZB1-A-05-00-00-00A-007A-A-B
     *   - SNS: ZB1-A-05-00-00-00A-007A-A (8段)
     *   - itemLocationCode: B
     *
     * @param dmcCode DMC编码字符串
     * @return 字段Map
     */
    private Map<String, String> parseDmcCode(String dmcCode) {
        Map<String, String> attrs = new HashMap<>();

        if (dmcCode == null || dmcCode.isEmpty() || !dmcCode.startsWith("DMC-")) {
            return attrs;
        }

        // 去掉 "DMC-" 前缀
        String withoutPrefix = dmcCode.substring(4);

        // 按 "-" 分割
        String[] parts = withoutPrefix.split("-", -1);  // -1保留尾部空字符串

        // SNS固定是前8段（根据composeSns的实现）
        // 格式：[0]modelIdentCode - [1]systemDiffCode - [2]systemCode - [3]subSystem+subSubSystem
        //      - [4]assyCode - [5]disassyCode+disassyCodeVariant - [6]infoCode+infoCodeVariant - [7]itemLocationCode(可选)
        //
        // 但实际上从buildDmcCode可知：
        // DMC = "DMC-" + sns + "-" + infoCode + infoCodeVariant + ("-" + itemLocationCode)
        //
        // 关键问题：infoCode和infoCodeVariant是连在一起的，没有用"-"分割！
        // 所以：DMC-ZB1-A-05-00-00-00A-007A-A 中，最后的 "A" 是 disassyCodeVariant，不是infoCode
        //
        // 正确的理解：
        // - 如果parts.length == 8: DMC只有SNS部分，没有infoCode（不符合标准）
        // - 如果parts.length == 9: 最后一段可能是 infoCode+variant，或者 infoCode+variant+itemLocationCode
        // - 如果parts.length == 10: 最后一段是itemLocationCode

        if (parts.length < 8) {
            log.warn("DMC格式不正确，段数不足8（SNS不完整）：{}", dmcCode);
            return attrs;
        }

        // 提取SNS的8个字段
        attrs.put("modelIdentCode", parts[0]);
        attrs.put("systemDiffCode", parts[1]);
        attrs.put("systemCode", parts[2]);

        // parts[3] 是 subSystemCode + subSubSystemCode（连在一起）
        // 根据composeSns，subSystemCode是1位，subSubSystemCode是0-N位
        String subSystemPart = parts[3];
        if (subSystemPart.length() >= 1) {
            attrs.put("subSystemCode", subSystemPart.substring(0, 1));
            if (subSystemPart.length() > 1) {
                attrs.put("subSubSystemCode", subSystemPart.substring(1));
            } else {
                attrs.put("subSubSystemCode", "");
            }
        } else {
            attrs.put("subSystemCode", "");
            attrs.put("subSubSystemCode", "");
        }

        attrs.put("assyCode", parts[4]);

        // parts[5] 是 disassyCode + disassyCodeVariant（连在一起）
        // 根据composeSns，disassyCode是2位，disassyCodeVariant是0-N位
        String disassyPart = parts[5];
        if (disassyPart.length() >= 2) {
            attrs.put("disassyCode", disassyPart.substring(0, 2));
            if (disassyPart.length() > 2) {
                attrs.put("disassyCodeVariant", disassyPart.substring(2));
            } else {
                attrs.put("disassyCodeVariant", "");
            }
        } else {
            attrs.put("disassyCode", disassyPart);
            attrs.put("disassyCodeVariant", "");
        }

        // parts[6] 是 infoCode + infoCodeVariant（连在一起）
        // 根据S1000D标准，infoCode是3位，variant是0-N位
        if (parts.length >= 7) {
            String infoCodePart = parts[6];
            if (infoCodePart.length() >= 3) {
                attrs.put("infoCode", infoCodePart.substring(0, 3));
                if (infoCodePart.length() > 3) {
                    attrs.put("infoCodeVariant", infoCodePart.substring(3));
                } else {
                    attrs.put("infoCodeVariant", "");
                }
            } else {
                // infoCode长度不足3位，使用默认值
                log.warn("infoCode长度不足3位，DMC可能不完整：{}", dmcCode);
                attrs.put("infoCode", "000");
                attrs.put("infoCodeVariant", "");
            }
        } else {
            // 没有infoCode部分，使用默认值
            attrs.put("infoCode", "000");
            attrs.put("infoCodeVariant", "");
        }

        // parts[7] 是 itemLocationCode（可选）
        if (parts.length >= 8) {
            attrs.put("itemLocationCode", parts[7]);
        } else {
            attrs.put("itemLocationCode", "");
        }

        return attrs;
    }

    /**
     * 从资源文件名提取语言和国家代码
     * 【P0修复】用于资源文件校验
     *
     * 资源文件名格式：DMC-xxx_版本_语言-国家_原始名.ext
     * 示例：DMC-ZB1-A-05-00-00-00A-007A-A_001-03_zh-CN_金波.jpg
     *
     * @param fileName 资源文件名
     * @return [languageIsoCode, countryIsoCode]
     */
    private String[] extractLanguageFromResourceName(String fileName) {
        String[] result = new String[]{"", ""};

        if (fileName == null || fileName.isEmpty()) {
            return result;
        }

        // 按下划线分割
        String[] parts = fileName.split("_");

        // 至少需要3部分：DMC前缀_版本_语言-国家
        if (parts.length < 3) {
            return result;
        }

        // parts[2] 应该是 "zh-CN" 或 "zh-CN.jpg" 或 "zh-CN_金波.jpg"
        String langPart = parts[2];

        // 去掉扩展名和其他后缀
        if (langPart.contains(".")) {
            langPart = langPart.substring(0, langPart.indexOf("."));
        }
        if (langPart.contains("_")) {
            langPart = langPart.substring(0, langPart.indexOf("_"));
        }

        // 按 "-" 分割语言和国家
        String[] langParts = langPart.split("-");
        if (langParts.length >= 1) {
            result[0] = langParts[0];  // languageIsoCode
        }
        if (langParts.length >= 2) {
            result[1] = langParts[1];  // countryIsoCode
        }

        return result;
    }

    /**
     * 检查DM是否存在（资源文件专用）
     * 【P0修复】使用与DM校验相同的6字段组合查询，确保逻辑一致
     *
     * @param resourceFileName 资源文件名
     * @param projectId 项目ID
     * @return true-存在，false-不存在
     */
    private boolean isDmExistsForResource(String resourceFileName, String projectId) {
        IetmDataModule dm = findDmForResource(resourceFileName, projectId);
        return dm != null;
    }

    /**
     * 根据资源文件名查找关联的DM
     * 【P0修复】使用与校验相同的6字段组合查询，确保校验和导入逻辑一致
     *
     * @param resourceFileName 资源文件名
     * @param projectId 项目ID
     * @return 关联的DM，不存在返回null
     */
    private IetmDataModule findDmForResource(String resourceFileName, String projectId) {
        try {
            // 1. 提取DMC前缀
            String dmcPrefix = extractDmcPrefixFromResourceName(resourceFileName);
            if (dmcPrefix == null || dmcPrefix.isEmpty()) {
                log.warn("无法从资源文件名提取DMC前缀：{}", resourceFileName);
                return null;
            }

            // 2. 解析DMC，提取各个字段
            Map<String, String> dmCodeAttrs = parseDmcCode(dmcPrefix);
            if (dmCodeAttrs.isEmpty()) {
                log.warn("DMC解析失败：{}", dmcPrefix);
                return null;
            }

            // 3. 提取语言代码
            String[] langInfo = extractLanguageFromResourceName(resourceFileName);
            String languageIsoCode = langInfo[0];
            String countryIsoCode = langInfo[1];

            // 4. 构建SNS
            String sns = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmcUtils.composeSns(
                safeStr(dmCodeAttrs.get("modelIdentCode")),
                safeStr(dmCodeAttrs.get("systemDiffCode")),
                safeStr(dmCodeAttrs.get("systemCode")),
                safeStr(dmCodeAttrs.get("subSystemCode")),
                safeStr(dmCodeAttrs.get("subSubSystemCode")),
                safeStr(dmCodeAttrs.get("assyCode")),
                safeStr(dmCodeAttrs.get("disassyCode")),
                safeStr(dmCodeAttrs.get("disassyCodeVariant"))
            );

            String infoCode = safeStr(dmCodeAttrs.get("infoCode"));
            if (infoCode.isEmpty()) {
                infoCode = "000";
            }

            // 5. 使用6字段组合查询（与DM校验逻辑完全一致）
            QueryWrapper<IetmDataModule> qw = buildDmExistCheckQuery(
                projectId, sns, infoCode,
                safeStr(dmCodeAttrs.get("infoCodeVariant")),
                safeStr(dmCodeAttrs.get("itemLocationCode")),
                languageIsoCode,
                countryIsoCode
            );

            IetmDataModule dm = dataModuleService.getOne(qw);

            log.debug("资源文件关联DM查询：fileName={}, dmcPrefix={}, sns={}, infoCode={}, " +
                     "languageIsoCode={}, countryIsoCode={}, 结果={}",
                     resourceFileName, dmcPrefix, sns, infoCode,
                     languageIsoCode, countryIsoCode, dm != null ? dm.getId() : "null");

            return dm;

        } catch (Exception e) {
            log.error("资源文件关联DM查询异常：fileName={}", resourceFileName, e);
            return null;
        }
    }

    /**
     * 校验ICN文件名格式
     *
     * ICN文件名格式：ICN-<modelIdentCode>-<systemCode>-<图符编号>.<扩展名>
     * 例如：ICN-MODEL001-SNS001-00001.png
     */
    private boolean isValidIcnFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        // 提取文件名（去掉路径）
        String baseName = FileNameUtils.getBaseName(fileName);

        // 简单校验：必须以ICN-开头
        if (!baseName.toUpperCase().startsWith("ICN-")) {
            return false;
        }

        // 分段校验（至少4段：ICN-model-sns-number）
        String[] parts = baseName.split("-");
        if (parts.length < 4) {
            return false;
        }

        return true;
    }

    /**
     * 从文件名提取ICN编码
     *
     * ICN-MODEL001-SNS001-00001.png → ICN-MODEL001-SNS001-00001
     */
    private String extractIcnCode(String fileName) {
        return FileNameUtils.getBaseName(fileName);
    }

    /**
     * 从ICN编码中提取SNS
     *
     * ICN-MODEL001-SNS001-00001 → SNS001
     *
     * ⚠️ 当前实现可能不正确，需要根据ICN的实际格式确认
     */
    private String extractSnsFromIcnCode(String icnCode) {
        if (icnCode == null || icnCode.isEmpty()) {
            return null;
        }

        String[] parts = icnCode.split("-");
        if (parts.length < 3) {
            return null;
        }

        // 第3段是SNS（ICN-model-sns-number）
        return parts[2];
    }

    // ========== 导入实现方法 ==========

    /**
     * 构建Path到构型节点的映射表（P1-1修复：避免N+1查询）
     *
     * @param projectId 项目ID
     * @return Map<path, 构型节点>
     */
    private Map<String, IetmProjectConfigurationManagement> buildPathToNodeMap(String projectId) {
        Map<String, IetmProjectConfigurationManagement> pathMap = new HashMap<>();

        // 批量查询项目所有构型节点
        QueryWrapper<IetmProjectConfigurationManagement> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId);
        List<IetmProjectConfigurationManagement> nodes = configurationService.list(qw);

        // 构建path到节点的映射
        for (IetmProjectConfigurationManagement node : nodes) {
            if (node.getPath() != null && !node.getPath().isEmpty()) {
                pathMap.put(node.getPath(), node);
            }
        }

        log.info("项目[{}]构建Path映射表完成，共{}个节点", projectId, pathMap.size());
        return pathMap;
    }

    /**
     * 从XML提取DM所有字段
     * 【P1-2修复】从importSingleDm拆分，单一职责
     *
     * @param dm DM实体（输出参数，会被填充字段）
     * @param xmlContent XML内容
     * @param projectId 项目ID
     * @param pathToNodeMap 构型节点映射表
     * @return dmCodeAttrs（用于后续校验）
     * @throws Exception 解析失败
     */
    private Map<String, String> extractFieldsFromDmXml(IetmDataModule dm,
                                                       String xmlContent,
                                                       String projectId,
                                                       Map<String, IetmProjectConfigurationManagement> pathToNodeMap)
            throws Exception {
        org.dom4j.io.SAXReader reader = createSecureXmlReader();

        org.dom4j.Document doc = reader.read(
                new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
        org.dom4j.Element root = doc.getRootElement();

        // 提取dmCode属性
        Map<String, String> dmCodeAttrs = extractDmCodeFromXml(doc);
        if (dmCodeAttrs != null) {
            // ✅ 生成SNS（必填）
            String sns = org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmcUtils.composeSns(
                    safeStr(dmCodeAttrs.get("modelIdentCode")),
                    safeStr(dmCodeAttrs.get("systemDiffCode")),
                    safeStr(dmCodeAttrs.get("systemCode")),
                    safeStr(dmCodeAttrs.get("subSystemCode")),
                    safeStr(dmCodeAttrs.get("subSubSystemCode")),
                    safeStr(dmCodeAttrs.get("assyCode")),
                    safeStr(dmCodeAttrs.get("disassyCode")),
                    safeStr(dmCodeAttrs.get("disassyCodeVariant"))
            );
            dm.setSns(sns != null && !sns.isEmpty() ? sns : "");

            // ✅ 设置info_code（必填）
            String infoCode = safeStr(dmCodeAttrs.get("infoCode"));
            dm.setInfoCode(infoCode != null && !infoCode.isEmpty() ? infoCode : "000");

            // ✅ 设置info_code_variant（可选）
            String infoCodeVariant = safeStr(dmCodeAttrs.get("infoCodeVariant"));
            if (infoCodeVariant != null && !infoCodeVariant.isEmpty()) {
                dm.setInfoCodeVariant(infoCodeVariant);
            }

            // ✅ 设置ietm_location_code（可选）
            String itemLocationCode = safeStr(dmCodeAttrs.get("itemLocationCode"));
            if (itemLocationCode != null && !itemLocationCode.isEmpty()) {
                dm.setIetmLocationCode(itemLocationCode);
            }
        }

        // ✅ 提取language（可选但重要）
        org.dom4j.Element language = (org.dom4j.Element) root.selectSingleNode("//language");
        if (language != null) {
            String languageIsoCode = language.attributeValue("languageIsoCode", "");
            if (!languageIsoCode.isEmpty()) {
                dm.setLanguageIsoCode(languageIsoCode);
            }
            String countryIsoCode = language.attributeValue("countryIsoCode", "");
            if (!countryIsoCode.isEmpty()) {
                dm.setCountryIsoCode(countryIsoCode);
            }
        }

        // ✅ 提取issueInfo（必填：inWork）
        org.dom4j.Element issueInfo = (org.dom4j.Element) root.selectSingleNode("//issueInfo");
        if (issueInfo != null) {
            String issueNumber = issueInfo.attributeValue("issueNumber", "001");
            dm.setIssueNo(issueNumber);

            String inWork = issueInfo.attributeValue("inWork", "00");
            dm.setInWork(inWork);
        } else {
            dm.setIssueNo("001");
            dm.setInWork("00");
        }

        // ✅ 提取dmStatus的issueType（可选但重要）
        org.dom4j.Element dmStatus = (org.dom4j.Element) root.selectSingleNode("//dmStatus");
        if (dmStatus != null) {
            String issueType = dmStatus.attributeValue("issueType", "");
            if (!issueType.isEmpty()) {
                dm.setIssueType(issueType);
            }
        }

        // ✅ 提取originator（必填）
        org.dom4j.Element originator = (org.dom4j.Element) root.selectSingleNode("//originator");
        if (originator != null) {
            String originatorCode = originator.attributeValue("enterpriseCode", "");
            dm.setOriginator(!originatorCode.isEmpty() ? originatorCode : "DEFAULT");

            org.dom4j.Element enterpriseName = originator.element("enterpriseName");
            if (enterpriseName != null && enterpriseName.getText() != null && !enterpriseName.getText().isEmpty()) {
                dm.setOriginatorName(enterpriseName.getText());
            }
        } else {
            dm.setOriginator("DEFAULT");
        }

        // ✅ 提取responsiblePartnerCompany（可选）
        org.dom4j.Element rpc = (org.dom4j.Element) root.selectSingleNode("//responsiblePartnerCompany");
        if (rpc != null) {
            String rpcCode = rpc.attributeValue("enterpriseCode", "");
            if (!rpcCode.isEmpty()) {
                dm.setRpc(rpcCode);
            }

            org.dom4j.Element rpcName = rpc.element("enterpriseName");
            if (rpcName != null && rpcName.getText() != null && !rpcName.getText().isEmpty()) {
                dm.setRpcName(rpcName.getText());
            }
        }

        // ✅ 提取dmTitle（可选但重要）
        org.dom4j.Element dmTitle = (org.dom4j.Element) root.selectSingleNode("//dmTitle");
        if (dmTitle != null) {
            org.dom4j.Element techName = dmTitle.element("techName");
            if (techName != null && techName.getText() != null && !techName.getText().isEmpty()) {
                dm.setTechName(techName.getText());
            }

            org.dom4j.Element infoName = dmTitle.element("infoName");
            if (infoName != null && infoName.getText() != null && !infoName.getText().isEmpty()) {
                dm.setInfoName(infoName.getText());
            }
        }

        // ✅ 提取密级（可选但重要）
        String security = extractSecurityFromXml(xmlContent);
        if (security != null && !security.isEmpty()) {
            dm.setSecurity(security);
        }

        // ✅ 查询构型节点ID（必填）
        if (dmCodeAttrs != null) {
            String path = buildPathFromDmCode(dmCodeAttrs);
            IetmProjectConfigurationManagement config = pathToNodeMap.get(path);

            if (config != null) {
                dm.setCmNodeId(config.getId());
                dm.setCmNodeName(config.getTitle());
                dm.setCmNodePath(config.getPath());
                log.debug("从映射表匹配构型节点：path={} -> nodeId={}", path, config.getId());
            } else {
                log.warn("找不到构型节点：projectId={}, path={}", projectId, path);
                throw new JeecgBootException("找不到对应的构型节点，请检查SNS是否在项目构型中");
            }
        }

        return dmCodeAttrs;
    }

    /**
     * 导入前二次校验DM是否已存在
     * 【P1-2修复】从importSingleDm拆分，单一职责
     */
    private void validateDmBeforeImport(String projectId, IetmDataModule dm, Map<String, String> dmCodeAttrs)
            throws JeecgBootException {
        if (isDmExistsBySixFields(projectId, dmCodeAttrs,
                                  dm.getInfoCode(), dm.getInfoCodeVariant(),
                                  dm.getIetmLocationCode(),
                                  dm.getLanguageIsoCode(), dm.getCountryIsoCode())) {
            log.warn("导入失败：DM已存在（规则-1校验），dmcCode={}, sns={}, infoCode={}, projectId={}",
                    dm.getDmcCode(), dm.getSns(), dm.getInfoCode(), projectId);
            throw new JeecgBootException("该DM已存在，不能导入。DMC编码：" + dm.getDmcCode() +
                    "。如需更新，请先删除旧版本后再导入。");
        }
    }

    /**
     * 保存DM到数据库
     * 【P1-2修复】从importSingleDm拆分，单一职责
     */
    private void saveDmToDatabase(IetmDataModule dm) throws JeecgBootException {
        dm.setStatus("1");
        dm.setIsLatest("1");

        String username = getCurrentUsername();
        dm.setCreateBy(username);
        dm.setCreateTime(new java.util.Date());

        boolean success = dataModuleService.save(dm);
        if (!success) {
            throw new JeecgBootException("保存DM到数据库失败");
        }
    }

    /**
     * 保存DM XML文件到服务器
     * 【P1-2修复】从importSingleDm拆分，单一职责
     */
    private String saveDmXmlFile(String dmcCode, String tempFilePath, String projectId) throws Exception {
        if (tempFilePath == null || tempFilePath.isEmpty()) {
            return null;
        }
        String targetPath = buildDmFilePath(projectId, dmcCode);
        copyFileFromTemp(tempFilePath, targetPath);
        return targetPath;
    }

    /**
     * 回滚DM导入（删除数据库记录和物理文件）
     * 【P1-2修复】从importSingleDm拆分，统一回滚逻辑
     * 【P0-2优化】回滚机制幂等性增强
     */
    private void rollbackDmImport(String dmId, String targetPath) {
        try {
            dataModuleService.removeById(dmId);
            log.info("回滚：删除DM数据库记录 dmId={}", dmId);
        } catch (Exception dbEx) {
            log.warn("回滚时删除DM数据库记录失败：dmId={}", dmId, dbEx);
        }

        if (targetPath != null) {
            try {
                File targetFile = new File(targetPath);
                if (targetFile.exists()) {
                    boolean deleted = targetFile.delete();
                    log.info("回滚：删除DM物理文件 {} - {}", targetPath, deleted ? "成功" : "失败");
                } else {
                    log.debug("回滚：DM物理文件不存在，跳过删除：{}", targetPath);
                }
            } catch (Exception deleteEx) {
                log.warn("回滚时删除DM物理文件失败：{}", targetPath, deleteEx);
            }
        }
    }

    /**
     * 导入单个DM文件
     * 【P1-2修复】拆分为子方法，主方法负责流程编排
     * 【P2-3修复】文件保存失败时清理数据库
     * 【P1-1修复】使用预加载的构型节点映射表，避免N+1查询
     */
    private void importSingleDm(ImportFileItemVO dmFile, String projectId,
                                Map<String, IetmProjectConfigurationManagement> pathToNodeMap,
                                HttpServletRequest request) throws Exception {
        // 1. 构建DM实体
        IetmDataModule dm = new IetmDataModule();
        dm.setId(UUID.randomUUID().toString().replace("-", ""));
        dm.setDmcCode(dmFile.getDmcCode());
        dm.setDmContent(dmFile.getXmlContent());
        dm.setProjectId(projectId);

        // 2. 从XML提取所有字段
        Map<String, String> dmCodeAttrs;
        try {
            dmCodeAttrs = extractFieldsFromDmXml(dm, dmFile.getXmlContent(), projectId, pathToNodeMap);
        } catch (JeecgBootException e) {
            throw e;  // 重新抛出业务异常
        } catch (Exception e) {
            log.error("从XML提取字段失败", e);
            throw new JeecgBootException("解析DM文件失败：" + e.getMessage());
        }

        // 3. 导入前二次校验DM是否已存在（防止校验后、导入前有其他用户导入相同DM）
        validateDmBeforeImport(projectId, dm, dmCodeAttrs);

        // 4. 保存到数据库
        saveDmToDatabase(dm);

        // 5. 保存XML文件到服务器（失败时回滚）
        String targetPath = null;
        try {
            targetPath = saveDmXmlFile(dmFile.getDmcCode(), dmFile.getTempFilePath(), projectId);
        } catch (Exception e) {
            log.error("保存DM文件失败，执行回滚：dmId={}, targetPath={}", dm.getId(), targetPath, e);
            rollbackDmImport(dm.getId(), targetPath);
            throw new JeecgBootException("保存DM文件失败：" + e.getMessage());
        }
    }



    /**
     * 导入单个ICN文件（P2-3修复：文件保存失败时清理数据库）
     * P1-1修复：使用预加载的SNS映射表，避免N+1查询
     * P1-2修复：拆分为多个子方法，降低复杂度
     */
    private void importSingleIcn(ImportFileItemVO icnFile, String projectId,
                                 Map<String, String> snsToNodeIdMap,
                                 HttpServletRequest request) throws Exception {
        String icnCode = extractIcnCode(icnFile.getFileName());

        // 1. 构建ICN实体并解析字段
        IetmIcnManage icn = buildIcnEntity(icnCode, snsToNodeIdMap);
        parseIcnFields(icn, icnCode, icnFile.getFileName());

        // 2. 保存到数据库
        String username = saveIcnToDatabase(icn);

        // 3. 保存文件到服务器（失败时回滚数据库）
        if (icnFile.getTempFilePath() != null && !icnFile.getTempFilePath().isEmpty()) {
            saveIcnFile(icnFile, icn, projectId, username);
        }
    }

    /**
     * 构建ICN实体并匹配构型节点
     */
    private IetmIcnManage buildIcnEntity(String icnCode, Map<String, String> snsToNodeIdMap) {
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(UUID.randomUUID().toString().replace("-", ""));
        icn.setIcn(icnCode);

        // 从ICN编码提取SNS并自动匹配cmNodeId
        String sns = extractSnsFromIcnCode(icnCode);
        if (sns != null && !sns.isEmpty()) {
            icn.setSns(sns);

            String cmNodeId = snsToNodeIdMap.get(sns);
            if (cmNodeId == null) {
                throw new JeecgBootException("ICN的SNS [" + sns + "] 在项目构型中不存在，无法自动关联构型节点");
            }

            icn.setCmNodeId(cmNodeId);
            log.info("自动匹配构型节点：ICN={}, SNS={} -> cmNodeId={}", icnCode, sns, cmNodeId);
        } else {
            throw new JeecgBootException("无法从ICN编码中提取SNS：" + icnCode);
        }

        return icn;
    }

    /**
     * 从ICN文件名解析必需字段
     * ICN文件名格式：ICN-[0]-[1]-[2]-[3]-[4]-[5]-[6]-[7]-[8]
     */
    private void parseIcnFields(IetmIcnManage icn, String icnCode, String fileName) {
        String[] parts = icnCode.split("-");
        if (parts.length >= 9) {
            // 必需字段
            icn.setOriginator(parts[4]);
            icn.setUniqueId(parts[5]);

            try {
                int securityLevel = Integer.parseInt(parts[8]);
                icn.setSecurity(securityLevel);
            } catch (NumberFormatException e) {
                log.warn("ICN密级解析失败，使用默认值0：{}", parts[8]);
                icn.setSecurity(0);
            }

            // 可选字段
            if (parts.length > 3) icn.setRpc(parts[3]);
            if (parts.length > 6) icn.setVariantCode(parts[6]);
            if (parts.length > 7) icn.setIssueNo(parts[7]);

            log.info("解析ICN字段：originator={}, uniqueId={}, security={}",
                    icn.getOriginator(), icn.getUniqueId(), icn.getSecurity());
        } else {
            throw new JeecgBootException("ICN文件名格式不正确，无法解析必需字段：" + icnCode);
        }

        // 设置文件信息
        icn.setFileName(fileName);
        String extension = FileNameUtils.getExtension(fileName);
        if (extension != null && !extension.isEmpty()) {
            icn.setFileType(extension);
        }

        // 设置默认值
        if (icn.getIssueNo() == null || icn.getIssueNo().isEmpty()) {
            icn.setIssueNo("001");
        }
        icn.setIspublished("0");
        icn.setIsdeleted("0");
    }

    /**
     * 保存ICN到数据库
     * @return 当前用户名
     */
    private String saveIcnToDatabase(IetmIcnManage icn) {
        String username = getCurrentUsername();
        icn.setCreateBy(username);
        icn.setCreateTime(new java.util.Date());

        boolean success = icnManageService.save(icn);
        if (!success) {
            throw new JeecgBootException("保存ICN到数据库失败");
        }

        return username;
    }

    /**
     * 保存ICN文件到服务器
     */
    private void saveIcnFile(ImportFileItemVO icnFile, IetmIcnManage icn,
                            String projectId, String username) {
        String attachmentId = null;
        File targetFile = null;

        try {
            // 构建相对路径
            String relativeDir = "project/" + projectId + "/icn";
            File icnDir = new File(fileStorageLocation, relativeDir);
            if (!icnDir.exists()) {
                icnDir.mkdirs();
            }

            String safeFileName = FileNameUtils.sanitize(icnFile.getFileName());
            targetFile = new File(icnDir, safeFileName);
            String relativePath = relativeDir + "/" + safeFileName;

            // 路径遍历防护
            Path targetFilePath = targetFile.toPath().toAbsolutePath().normalize();
            Path baseFilePath = new File(fileStorageLocation).toPath().toAbsolutePath().normalize();
            if (!targetFilePath.startsWith(baseFilePath)) {
                throw new JeecgBootException("目标文件路径非法");
            }

            // 复制文件
            copyFileFromTemp(icnFile.getTempFilePath(), targetFile.getAbsolutePath());

            // 保存附件记录
            IetmAttachment attachment = new IetmAttachment();
            attachmentId = UUID.randomUUID().toString().replace("-", "");
            attachment.setId(attachmentId);
            attachment.setPid(icn.getId());
            attachment.setFileKey(relativePath);
            attachment.setFileName(safeFileName);
            attachment.setFileType(icn.getFileType());
            attachment.setCreateBy(username);
            attachment.setCreateTime(new java.util.Date());

            boolean attachSuccess = attachmentService.save(attachment);
            if (!attachSuccess) {
                throw new JeecgBootException("保存ICN附件记录失败");
            }

            log.info("成功保存ICN文件：{} -> {}", icn.getIcn(), relativePath);

        } catch (Exception e) {
            rollbackIcnImport(icn.getId(), attachmentId, targetFile, e);
        }
    }

    /**
     * 回滚ICN导入（删除数据库记录和物理文件）
     */
    private void rollbackIcnImport(String icnId, String attachmentId, File targetFile, Exception cause) {
        log.error("保存ICN文件失败，执行完整回滚：ICN={}, Attachment={}, targetFile={}",
                icnId, attachmentId, targetFile != null ? targetFile.getAbsolutePath() : null, cause);

        // 删除ICN数据库记录
        try {
            icnManageService.removeById(icnId);
            log.info("回滚：删除ICN数据库记录 icnId={}", icnId);
        } catch (Exception dbEx) {
            log.warn("回滚时删除ICN数据库记录失败：icnId={}", icnId, dbEx);
        }

        // 删除附件数据库记录
        if (attachmentId != null) {
            try {
                attachmentService.removeById(attachmentId);
                log.info("回滚：删除附件数据库记录 attachmentId={}", attachmentId);
            } catch (Exception dbEx) {
                log.warn("回滚时删除附件数据库记录失败：attachmentId={}", attachmentId, dbEx);
            }
        }

        // 删除物理文件（幂等性：检查文件是否存在）
        if (targetFile != null && targetFile.exists()) {
            try {
                boolean deleted = targetFile.delete();
                log.info("回滚：删除ICN物理文件 {} - {}", targetFile.getAbsolutePath(), deleted ? "成功" : "失败");
            } catch (Exception deleteEx) {
                log.warn("回滚时删除ICN物理文件失败：{}", targetFile.getAbsolutePath(), deleteEx);
            }
        }

        throw new JeecgBootException("保存ICN文件失败：" + cause.getMessage());
    }

    /**
     * 导入单个资源文件（P2-3修复：文件保存失败时清理数据库）
     * 【P0修复】使用6字段组合查询，与校验逻辑一致
     */
    private void importSingleResource(ImportFileItemVO resourceFile, String projectId, HttpServletRequest request) throws Exception {
        // 1. 查找关联的DM（使用与校验相同的6字段组合查询）
        String fileName = resourceFile.getFileName();
        IetmDataModule dm = findDmForResource(fileName, projectId);

        if (dm == null) {
            String dmcCode = resourceFile.getAssociatedDmcCode();
            throw new JeecgBootException("关联的DM不存在：" + dmcCode + "（请先导入对应的DM文件）");
        }

        // 2. 提取原始文件名（去掉DMC前缀和版本信息等）
        // 【P0修复-5】使用智能算法提取原始文件名，支持多种格式
        String originalFileName = extractOriginalFileName(fileName);

        // 3. 保存资源记录到ietm_dm_comment表（DM资源表）
        String username = getCurrentUsername();

        // 注意：ietm_dm_comment表用于存储DM的资源文件（对齐导出逻辑）
        IetmDmComment dmComment = new IetmDmComment();
        String commentId = UUID.randomUUID().toString().replace("-", "");
        dmComment.setId(commentId);
        dmComment.setDmId(dm.getId());
        dmComment.setResourceName(originalFileName);
        dmComment.setFileName(originalFileName);  // 原始文件名

        // 4. 保存文件到服务器（P2-3修复：失败时删除数据库记录）
        String relativeDir = "project/" + projectId + "/dm_resource";
        File resourceDir = new File(fileStorageLocation, relativeDir);
        if (!resourceDir.exists()) {
            resourceDir.mkdirs();
        }

        // 使用时间戳避免文件名冲突
        String safeFileName = System.currentTimeMillis() + "_" + FileNameUtils.sanitize(originalFileName);
        File targetFile = new File(resourceDir, safeFileName);
        String relativePath = relativeDir + "/" + safeFileName;

        // 路径遍历防护
        Path targetFilePath = targetFile.toPath().toAbsolutePath().normalize();
        Path baseFilePath = new File(fileStorageLocation).toPath().toAbsolutePath().normalize();
        if (!targetFilePath.startsWith(baseFilePath)) {
            throw new JeecgBootException("目标文件路径非法");
        }

        try {
            // 复制文件
            if (resourceFile.getTempFilePath() != null && !resourceFile.getTempFilePath().isEmpty()) {
                copyFileFromTemp(resourceFile.getTempFilePath(), targetFile.getAbsolutePath());
            } else {
                // ✅ 增强错误提示：检查校验状态
                if (!DmImportConstants.SUCCESS.equals(resourceFile.getResultCode())) {
                    throw new JeecgBootException("资源文件校验失败：" + resourceFile.getResultMessage());
                } else {
                    throw new JeecgBootException("资源文件临时路径为空（内部错误，请联系管理员）");
                }
            }

            // 更新文件路径并保存到数据库
            dmComment.setFilePath(relativePath);      // 相对路径
            dmComment.setCreateBy(username);
            dmComment.setCreateTime(new java.util.Date());

            // 使用dmCommentMapper保存（需要注入）
            int rows = dmCommentMapper.insert(dmComment);
            if (rows == 0) {
                throw new JeecgBootException("保存DM资源记录失败");
            }

            log.info("成功保存DM资源文件：{} -> {} (关联DM: {})", originalFileName, relativePath, dm.getId());

        } catch (Exception e) {
            // 文件保存失败，执行完整回滚：数据库记录 + 物理文件
            log.error("保存资源文件失败，执行完整回滚：Comment={}, targetFile={}", commentId,
                targetFile != null ? targetFile.getAbsolutePath() : null, e);

            // 【P0-2优化】增强回滚机制的幂等性和完整性

            // 1. 删除可能已保存的数据库记录
            try {
                dmCommentMapper.deleteById(commentId);
                log.info("回滚：删除DM资源记录 commentId={}", commentId);
            } catch (Exception dbEx) {
                log.warn("回滚时删除DM资源记录失败：commentId={}", commentId, dbEx);
            }

            // 2. 删除可能已保存的物理文件（幂等性：检查文件是否存在）
            if (targetFile != null) {
                if (targetFile.exists()) {
                    try {
                        boolean deleted = targetFile.delete();
                        log.info("回滚：删除资源物理文件 {} - {}", targetFile.getAbsolutePath(), deleted ? "成功" : "失败");
                    } catch (Exception deleteEx) {
                        log.warn("回滚时删除资源物理文件失败：{}", targetFile.getAbsolutePath(), deleteEx);
                    }
                } else {
                    log.debug("回滚：资源物理文件不存在，跳过删除：{}", targetFile.getAbsolutePath());
                }
            }

            throw new JeecgBootException("保存资源文件失败：" + e.getMessage());
        }
    }

    /**
     * 构建DM文件存储路径
     */
    private String buildDmFilePath(String projectId, String dmcCode) {
        // 路径格式：{fileStorageLocation}/project/{projectId}/dm/{dmcCode}.xml
        File dmDir = new File(fileStorageLocation, "project/" + projectId + "/dm");
        if (!dmDir.exists()) {
            dmDir.mkdirs();
        }
        return new File(dmDir, dmcCode + ".xml").getAbsolutePath();
    }

    /**
     * 构建ICN文件存储路径
     */
    private String buildIcnFilePath(String projectId, String icnCode, String fileName) {
        // 路径格式：{fileStorageLocation}/project/{projectId}/icn/{fileName}
        File icnDir = new File(fileStorageLocation, "project/" + projectId + "/icn");
        if (!icnDir.exists()) {
            icnDir.mkdirs();
        }
        String safeFileName = FileNameUtils.sanitize(fileName);
        return new File(icnDir, safeFileName).getAbsolutePath();
    }

    /**
     * 从临时文件复制到目标路径
     */
    private void copyFileFromTemp(String sourcePath, String targetPath) throws IOException {
        File sourceFile = new File(sourcePath);
        File targetFile = new File(targetPath);

        // 路径遍历防护
        Path targetFilePath = targetFile.toPath().toAbsolutePath().normalize();
        Path baseFilePath = new File(fileStorageLocation).toPath().toAbsolutePath().normalize();
        if (!targetFilePath.startsWith(baseFilePath)) {
            throw new JeecgBootException("目标文件路径非法");
        }

        FileUtils.copyFile(sourceFile, targetFile);
    }

    /**
     * 清理临时文件（P2-2修复）
     *
     * 导入完成后（无论成功或失败）自动清理临时文件，避免磁盘空间浪费
     */
    private void cleanupTempFiles(List<ImportFileItemVO> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        int cleanedCount = 0;
        int failedCount = 0;

        for (ImportFileItemVO file : files) {
            String tempFilePath = file.getTempFilePath();
            if (tempFilePath != null && !tempFilePath.isEmpty()) {
                try {
                    File tempFile = new File(tempFilePath);
                    if (tempFile.exists() && tempFile.delete()) {
                        cleanedCount++;
                        log.debug("清理临时文件：{}", tempFilePath);
                    }
                } catch (Exception e) {
                    failedCount++;
                    log.warn("清理临时文件失败：{} - {}", tempFilePath, e.getMessage());
                }
            }
        }

        if (cleanedCount > 0) {
            log.info("临时文件清理完成：成功{}个，失败{}个", cleanedCount, failedCount);
        }
    }

    // ========== ICN文件名校验方法（轻量级API） ==========

    /**
     * ICN文件名校验（轻量级）
     *
     * 只传文件名到后端，不传二进制数据，执行3条校验规则：
     * - 规则-11：ICN文件名格式校验
     * - 规则-12：ICN的SNS是否在构型中
     * - 规则-13：ICN是否已存在
     *
     * 对标旧系统：validateIcnFiles() 函数
     *
     * @param fileName ICN文件名（例如：ICN-MODEL-SNS-00001.png）
     * @param request HTTP请求（用于获取项目上下文）
     * @return 校验结果
     */
    @Override
    public ImportFileItemVO validateIcnByFileName(String fileName, HttpServletRequest request) throws Exception {
        ImportFileItemVO item = new ImportFileItemVO();
        item.setFileName(fileName);
        item.setFileType("ICN");

        try {
            // 获取项目上下文
            String projectId = getProjectIdFromRedis();
            if (projectId == null || projectId.isEmpty()) {
                throw new JeecgBootException("请先打开项目");
            }

            // 规则-11：ICN文件名格式校验
            if (!isValidIcnFileName(fileName)) {
                item.setResultCode(DmImportConstants.ERROR_ICN_NAME_INVALID);
                item.setResultMessage(DmImportConstants.getErrorMessage(DmImportConstants.ERROR_ICN_NAME_INVALID));
                return item;
            }

            // 提取ICN编码（从文件名）
            String icnCode = extractIcnCode(fileName);

            // 规则-13：ICN是否已存在
            QueryWrapper<IetmIcnManage> qw = new QueryWrapper<>();
            qw.eq("icn", icnCode);
            long count = icnManageService.count(qw);
            if (count > 0) {
                item.setResultCode(DmImportConstants.ERROR_ICN_EXISTS);
                item.setResultMessage(DmImportConstants.getErrorMessage(DmImportConstants.ERROR_ICN_EXISTS));
                return item;
            }

            // 规则-12：ICN的SNS是否在构型中
            String icnSns = extractSnsFromIcnCode(icnCode);
            if (icnSns != null && !icnSns.isEmpty()
                && !isSnsInConfiguration(icnSns, projectId)) {
                item.setResultCode(DmImportConstants.ERROR_ICN_SNS_NOT_IN_CM);
                item.setResultMessage(DmImportConstants.getErrorMessage(DmImportConstants.ERROR_ICN_SNS_NOT_IN_CM));
                return item;
            }

            // 全部通过
            item.setResultCode(DmImportConstants.SUCCESS);
            item.setResultMessage(DmImportConstants.getErrorMessage(DmImportConstants.SUCCESS));

        } catch (JeecgBootException e) {
            // 业务异常：直接抛出
            throw e;
        } catch (Exception e) {
            log.error("校验ICN文件名失败：{}", fileName, e);
            item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
            item.setResultMessage("校验异常：" + e.getMessage());
        }

        return item;
    }

    /**
     * 资源文件名校验（轻量级）
     *
     * 只传文件名到后端，不传二进制数据，执行校验规则：
     * - 检查文件名格式（是否包含DMC前缀）
     * - 检查关联的DM是否存在
     *
     * @param fileName 资源文件名（例如：DMC-XXX-XXX-XXX_资源文件.pdf）
     * @param request HTTP请求（用于获取项目上下文）
     * @return 校验结果
     */
    @Override
    public ImportFileItemVO validateResourceByFileName(String fileName, HttpServletRequest request) throws Exception {
        ImportFileItemVO item = new ImportFileItemVO();
        item.setFileName(fileName);
        item.setFileType("RESOURCE");

        try {
            // 获取项目上下文
            String projectId = getProjectIdFromRedis();
            if (projectId == null || projectId.isEmpty()) {
                throw new JeecgBootException("请先打开项目");
            }

            // 提取关联的DMC编码（从文件名前缀）
            String dmcPrefix = extractDmcPrefixFromResourceName(fileName);
            if (dmcPrefix == null || dmcPrefix.isEmpty()) {
                item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
                item.setResultMessage("无法从资源文件名提取DMC前缀");
                return item;
            }

            item.setAssociatedDmcCode(dmcPrefix);

            // 检查：关联的DM是否存在
            // 【P0修复】使用6字段组合查询，与DM校验逻辑完全一致
            IetmDataModule dm = findDmForResource(fileName, projectId);
            if (dm == null) {
                item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
                item.setResultMessage("关联的DM不存在：" + dmcPrefix + "（请先导入对应的DM文件）");
                log.info("资源文件校验失败：关联的DM不存在，dmcPrefix={}, projectId={}, fileName={}",
                         dmcPrefix, projectId, fileName);
                return item;
            }

            // 【P0关键修复】资源文件重复性检查（对齐validateResourceFromZip逻辑）
            // 【P0修复-5】使用智能算法提取原始文件名，支持多种格式
            String originalFileName = extractOriginalFileName(fileName);
            QueryWrapper<IetmDmComment> qw = new QueryWrapper<>();
            qw.eq("dm_id", dm.getId());
            qw.eq("file_name", originalFileName);
            long count = dmCommentMapper.selectCount(qw);
            if (count > 0) {
                item.setResultCode(DmImportConstants.ERROR_RESOURCE_EXISTS);
                item.setResultMessage("资源文件已存在：" + originalFileName + "（关联DM：" + dmcPrefix + "）");
                log.info("资源文件校验失败：资源已存在，dmId={}, fileName={}, projectId={}",
                    dm.getId(), originalFileName, projectId);
                return item;
            }

            // 全部通过
            item.setResultCode(DmImportConstants.SUCCESS);
            item.setResultMessage(DmImportConstants.getErrorMessage(DmImportConstants.SUCCESS));

        } catch (JeecgBootException e) {
            // 业务异常：直接抛出
            throw e;
        } catch (Exception e) {
            log.error("校验资源文件名失败：{}", fileName, e);
            item.setResultCode(DmImportConstants.ERROR_UNKNOWN);
            item.setResultMessage("校验异常：" + e.getMessage());
        }

        return item;
    }

    /**
     * 获取当前登录用户（P2优化：提取公共方法，消除重复代码）
     *
     * @return 当前登录用户对象
     * @throws JeecgBootException 未登录时抛出异常
     */
    private LoginUser getCurrentLoginUser() {
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (sysUser == null) {
            throw new JeecgBootException("未登录或登录已过期，请重新登录");
        }
        return sysUser;
    }

    /**
     * 获取当前登录用户名
     *
     * @return 当前用户名
     * @throws JeecgBootException 未登录时抛出异常
     */
    private String getCurrentUsername() {
        return getCurrentLoginUser().getUsername();
    }

    /**
     * 创建安全的XML解析器（防止XXE攻击）
     *
     * @return 配置好安全特性的SAXReader
     */
    private org.dom4j.io.SAXReader createSecureXmlReader() {
        try {
            org.dom4j.io.SAXReader reader = new org.dom4j.io.SAXReader();
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return reader;
        } catch (Exception e) {
            log.error("创建安全XML解析器失败", e);
            throw new JeecgBootException("初始化XML解析器失败：" + e.getMessage());
        }
    }
}
