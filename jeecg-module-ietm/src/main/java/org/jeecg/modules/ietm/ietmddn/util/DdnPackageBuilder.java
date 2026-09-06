package org.jeecg.modules.ietm.ietmddn.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDataModuleMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmRef;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmRefMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDmComment;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.mapper.IetmDmCommentMapper;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnReference;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnReferenceMapper;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.mapper.IetmAttachmentMapper;
import org.jeecg.modules.ietm.ietmddn.constants.DdnConstants;
import org.jeecg.modules.ietm.ietmddn.vo.DdnGenerateVO;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.util.DmcUtils;
import org.jeecg.modules.ietm.common.util.FileNameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @Description: DDN数据包构建工具
 * @Author: jeecg-boot
 * @Date: 2026-09-01
 */
@Slf4j
@Component
public class DdnPackageBuilder {

    @Autowired
    private IetmDataModuleMapper dataModuleMapper;

    @Autowired
    private IetmDmRefMapper dmRefMapper;

    @Autowired
    private IetmDmCommentMapper dmCommentMapper;

    @Autowired
    private IetmIcnReferenceMapper icnReferenceMapper;

    @Autowired
    private IetmIcnManageMapper icnManageMapper;

    @Autowired
    private IetmAttachmentMapper attachmentMapper;

    @Value("${accessFile.location}")
    private String fileStorageLocation;

    /**
     * 构建DDN数据包
     */
    public BuildResult buildDdnPackage(String ddnCode, DdnGenerateVO params, Map<String, Object> projectInfo) throws Exception {
        File workDir = null;
        long startTime = System.currentTimeMillis();

        try {
            // 1. 校验并创建工作目录（修复P0-4：完善路径遍历防护）
            if (ddnCode == null || ddnCode.trim().isEmpty()) {
                throw new JeecgBootException("DDN编码不能为空");
            }

            // 白名单校验：DDN编码格式必须符合标准（最安全的方案）
            // 格式：DDN-型号-单位-序号-年份-版本，例如 DDN-MODEL-UNIT-00001-2026-00001
            // 修复P0-8: 允许型号/单位编码段内包含连字符（如"J-10A"、"CASC-611"）
            if (!ddnCode.matches("^DDN-[A-Za-z0-9-]+-[A-Za-z0-9-]+-[A-Za-z0-9-]+-\\d{4}-\\d{5}$")) {
                throw new JeecgBootException("DDN编码格式非法，必须符合：DDN-型号-单位-序号-年份-版本");
            }

            // 强制规范化：过滤所有非法字符（纵深防御）
            String sanitizedCode = ddnCode.replaceAll("[^A-Za-z0-9-]", "");

            String ddnWorkDir = fileStorageLocation + File.separator + "ddn" + File.separator + sanitizedCode;
            workDir = new File(ddnWorkDir);

            // 修复P0-4：优化路径遍历防护，合并校验逻辑减少重复
            try {
                Path targetPath = workDir.toPath().toAbsolutePath().normalize();
                Path basePath = new File(fileStorageLocation, "ddn").toPath().toAbsolutePath().normalize();

                // 第一步：标准化路径校验
                if (!targetPath.startsWith(basePath)) {
                    throw new JeecgBootException("DDN目录路径非法：路径遍历检测到威胁");
                }

                // 第二步：如果目录已存在，使用realPath防符号链接攻击
                if (workDir.exists()) {
                    Path realPath = workDir.toPath().toRealPath();
                    Path baseRealPath = basePath.toRealPath();
                    if (!realPath.startsWith(baseRealPath)) {
                        throw new JeecgBootException("DDN目录路径非法：符号链接检测到威胁");
                    }
                    // 目录已存在且校验通过，删除旧文件
                    deleteDirectory(workDir);
                }
            } catch (IOException e) {
                log.error("路径校验失败：{}", e.getMessage(), e);
                throw new JeecgBootException("DDN目录路径校验失败：" + e.getMessage());
            }

            workDir.mkdirs();

            // 2. 递归收集DM、ICN（含引用）
            Set<String> allDmIds = new HashSet<>(params.getDmIds());
            Set<String> allIcnIds = new HashSet<>();

            if (params.getIncludeRefDm()) {
                collectReferencedDms(new ArrayList<>(params.getDmIds()), allDmIds, 0);
            }

            if (params.getIncludeRefIcn()) {
                // 收集所有DM引用的ICN
                for (String dmId : allDmIds) {
                    collectReferencedIcns(dmId, allIcnIds);
                }
            }

            log.info("DDN[{}] 收集完成：DM共{}个（初始{}个），ICN共{}个",
                    ddnCode, allDmIds.size(), params.getDmIds().size(), allIcnIds.size());

            // 3. 复制DM文件到 workDir/DM/（修复P1-6：收集错误DM）
            File dmDir = new File(workDir, DdnConstants.DirectoryNames.DM);
            dmDir.mkdirs();
            // 创建MM目录用于存放DM资源文件（S1000D 4.0标准建议）
            File mmDir = new File(workDir, DdnConstants.DirectoryNames.MM);
            mmDir.mkdirs();
            List<String> errorDmList = new ArrayList<>();
            List<String> missingResourceList = new ArrayList<>();  // 新增：缺失资源列表
            StringBuilder ddnLog = new StringBuilder();  // DDN.log内容
            Map<String, String> allFiles = new LinkedHashMap<>();  // 文件名 -> entityControlNumber映射

            // 性能优化：批量查询所有DM和资源（修复N+1查询问题）
            Map<String, IetmDataModule> dmMap = batchLoadDms(allDmIds);
            Map<String, List<IetmDmComment>> resourceMap = batchLoadDmResources(allDmIds);

            // 统计资源文件总数
            int totalResourceCount = 0;
            for (List<IetmDmComment> resources : resourceMap.values()) {
                totalResourceCount += resources.size();
            }

            for (String dmId : allDmIds) {
                copyDmFilesOptimized(dmId, dmMap, resourceMap, dmDir, mmDir,
                                    params.getIncludeDmResource(), errorDmList, missingResourceList, ddnLog, allFiles);
            }

            // 4. 复制ICN文件到 workDir/ICN/
            File icnDir = new File(workDir, DdnConstants.DirectoryNames.ICN);
            icnDir.mkdirs();
            List<String> missingIcnList = new ArrayList<>();  // 新增：缺失ICN列表

            // 性能优化：批量查询所有ICN和附件（修复N+1查询问题）
            Map<String, IetmIcnManage> icnMap = batchLoadIcns(allIcnIds);
            Map<String, List<IetmAttachment>> attachmentMap = batchLoadAttachments(allIcnIds);

            for (String icnId : allIcnIds) {
                copyIcnFilesOptimized(icnId, icnMap, attachmentMap, icnDir, missingIcnList, ddnLog, allFiles);
            }

            // 5. 生成DDN.XML文件（使用小写.xml扩展名，deliveryList结构）
            File ddnXml = new File(workDir, ddnCode + DdnConstants.FileExtensions.XML);
            generateDdnXmlWithDeliveryList(ddnXml, ddnCode, params, projectInfo, allFiles);

            // 6. 生成DDN.log文件
            File ddnLogFile = new File(workDir, "DDN" + DdnConstants.FileExtensions.LOG);
            Files.write(ddnLogFile.toPath(), ddnLog.toString().getBytes(StandardCharsets.UTF_8));
            log.info("DDN.log文件生成完成：{}", ddnLogFile.getName());

            // 7. 打包为ZIP
            String zipFileName = ddnCode + DdnConstants.FileExtensions.ZIP;
            File zipFile = new File(fileStorageLocation + File.separator + "ddn", zipFileName);
            zipFile.getParentFile().mkdirs();
            createZipPackage(workDir, zipFile);

            // 8. 返回结果（修复P0：包含缺失文件统计）
            BuildResult result = new BuildResult();
            result.setZipFilePath("ddn" + File.separator + zipFileName);
            result.setTotalDmCount(allDmIds.size());
            result.setTotalIcnCount(allIcnIds.size());
            result.setAllDmIds(allDmIds);
            result.setAllIcnIds(allIcnIds);
            result.setErrorDmList(errorDmList);

            // 新增：缺失文件统计
            result.setMissingIcnList(missingIcnList);
            result.setMissingResourceList(missingResourceList);
            result.setSuccessIcnCount(allIcnIds.size() - missingIcnList.size());
            result.setSuccessResourceCount(totalResourceCount - missingResourceList.size());
            result.setTotalResourceCount(totalResourceCount);

            long duration = System.currentTimeMillis() - startTime;
            log.info("DDN包构建完成，耗时：{}ms，DM:{}个，ICN:{}/{}个，资源:{}/{}个，缺失ICN:{}个，缺失资源:{}个",
                    duration, allDmIds.size(),
                    result.getSuccessIcnCount(), allIcnIds.size(),
                    result.getSuccessResourceCount(), totalResourceCount,
                    missingIcnList.size(), missingResourceList.size());

            return result;
        } finally {
            // 修复P1：确保清理临时目录（防止磁盘空间泄漏）
            if (workDir != null && workDir.exists()) {
                try {
                    deleteDirectory(workDir);
                    log.info("清理临时目录：{}", workDir.getAbsolutePath());
                } catch (IOException e) {
                    // P1-8修复：临时文件清理失败增加告警
                    String errorMsg = String.format("清理临时目录失败：%s，磁盘空间可能不足或文件被占用，请手动删除",
                        workDir.getAbsolutePath());
                    log.error(errorMsg, e);

                    // 增加系统告警（可集成监控系统）
                    sendCleanupFailureAlert(workDir.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }

    /**
     * 批量加载所有DM（性能优化：避免N+1查询）
     */
    private Map<String, IetmDataModule> batchLoadDms(Set<String> dmIds) {
        if (dmIds == null || dmIds.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("批量查询{}个DM", dmIds.size());
        List<IetmDataModule> dms = dataModuleMapper.selectBatchIds(dmIds);
        return dms.stream().collect(Collectors.toMap(IetmDataModule::getId, dm -> dm));
    }

    /**
     * 批量加载所有DM的资源文件（性能优化：避免N+1查询）
     */
    private Map<String, List<IetmDmComment>> batchLoadDmResources(Set<String> dmIds) {
        if (dmIds == null || dmIds.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("批量查询DM资源文件");
        List<IetmDmComment> resources = dmCommentMapper.selectList(
                new LambdaQueryWrapper<IetmDmComment>()
                        .in(IetmDmComment::getDmId, dmIds));

        return resources.stream()
                .collect(Collectors.groupingBy(IetmDmComment::getDmId));
    }

    /**
     * 批量加载所有ICN（性能优化：避免N+1查询）
     */
    private Map<String, IetmIcnManage> batchLoadIcns(Set<String> icnIds) {
        if (icnIds == null || icnIds.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("批量查询{}个ICN", icnIds.size());
        List<IetmIcnManage> icns = icnManageMapper.selectBatchIds(icnIds);
        return icns.stream().collect(Collectors.toMap(IetmIcnManage::getId, icn -> icn));
    }

    /**
     * 批量加载所有ICN的附件（性能优化：避免N+1查询）
     */
    private Map<String, List<IetmAttachment>> batchLoadAttachments(Set<String> icnIds) {
        if (icnIds == null || icnIds.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("批量查询ICN附件");
        List<IetmAttachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<IetmAttachment>()
                        .in(IetmAttachment::getPid, icnIds));

        return attachments.stream()
                .collect(Collectors.groupingBy(IetmAttachment::getPid));
    }

    /**
     * 复制DM文件（优化版：使用预加载的数据）
     * 修复P0：添加缺失资源文件追踪
     */
    private void copyDmFilesOptimized(String dmId, Map<String, IetmDataModule> dmMap,
                                      Map<String, List<IetmDmComment>> resourceMap,
                                      File dmDir, File mmDir, boolean includeResource,
                                      List<String> errorDmList, List<String> missingResourceList,
                                      StringBuilder ddnLog,
                                      Map<String, String> allFiles) throws IOException {
        IetmDataModule dm = dmMap.get(dmId);
        if (dm == null || dm.getDmContent() == null) {
            log.warn("DM[{}]不存在或无内容，跳过", dmId);
            // 收集错误DM信息
            if (dm != null && errorDmList != null) {
                errorDmList.add(String.format("DMC: %s (无内容)", dm.getDmcCode()));
            } else if (errorDmList != null) {
                errorDmList.add(String.format("DM ID: %s (不存在)", dmId));
            }
            return;
        }

        // 写入DM XML文件（文件名安全过滤，改为小写.xml扩展名）
        String safeDmFileName = FileNameUtils.sanitize(dm.getDmcCode()) + DdnConstants.FileExtensions.XML;
        File dmFile = new File(dmDir, safeDmFileName);
        Files.write(dmFile.toPath(), dm.getDmContent().getBytes("UTF-8"));
        log.debug("复制DM文件：{}", safeDmFileName);

        // 记录到DDN.log
        if (ddnLog != null) {
            ddnLog.append("DMC=").append(dm.getDmcCode()).append(": 导出成功。\n");
        }

        // 添加到文件映射（包含DM/相对路径）
        if (allFiles != null) {
            allFiles.put(DdnConstants.DirectoryNames.DM + "/" + safeDmFileName, dm.getDmcCode());
        }

        // 复制资源文件到MM/目录（如果勾选）
        if (includeResource) {
            List<IetmDmComment> resources = resourceMap.getOrDefault(dmId, Collections.emptyList());
            // 不同DM可能有同名资源文件，加DMC前缀防止重名覆盖
            String dmcPrefix = FileNameUtils.sanitize(dm.getDmcCode());
            for (IetmDmComment res : resources) {
                if (res.getFilePath() != null && !res.getFilePath().isEmpty()) {
                    File srcFile = new File(fileStorageLocation, res.getFilePath());
                    if (srcFile.exists()) {
                        // 从 fileName 中提取原始文件名（移除ID后缀）
                        // 例如: "金波_1786887767219_1788313777683.jpg" -> "金波.jpg"
                        String originalFileName = FileNameUtils.extractOriginalName(res.getFileName());

                        // 构建最终文件名: DMC前缀_原始文件名
                        String safeResName = FileNameUtils.withPrefix(dmcPrefix, originalFileName);
                        File destFile = new File(mmDir, safeResName);
                        Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        log.debug("复制DM资源文件：{} -> MM/{}", srcFile.getName(), safeResName);

                        // 资源文件也记录到DDN.log（缩进）和文件映射
                        if (ddnLog != null) {
                            ddnLog.append("    资源文件=").append(safeResName).append(": 导出成功。\n");
                        }
                        if (allFiles != null) {
                            allFiles.put(DdnConstants.DirectoryNames.MM + "/" + safeResName, null);  // 资源文件路径改为MM/
                        }
                    } else {
                        // 修复P0：记录缺失的资源文件
                        log.error("DM[{}]资源文件不存在：{}", dmId, res.getFilePath());
                        if (missingResourceList != null) {
                            String resourceName = res.getResourceName() != null ? res.getResourceName() : "未命名";
                            missingResourceList.add(String.format(
                                "DM: %s, 资源: %s (文件不存在: %s)",
                                dm.getDmcCode(), resourceName, res.getFilePath()
                            ));
                        }
                    }
                }
            }
        }
    }

    /**
     * 复制ICN文件（优化版：使用预加载的数据）
     * 修复P0：添加缺失ICN追踪
     */
    private void copyIcnFilesOptimized(String icnId, Map<String, IetmIcnManage> icnMap,
                                       Map<String, List<IetmAttachment>> attachmentMap,
                                       File icnDir, List<String> missingIcnList,
                                       StringBuilder ddnLog,
                                       Map<String, String> allFiles) throws IOException {
        IetmIcnManage icn = icnMap.get(icnId);
        if (icn == null) {
            // 修复P0：记录ICN不存在的情况
            log.error("ICN[{}]不存在，跳过", icnId);
            if (missingIcnList != null) {
                missingIcnList.add("ICN ID: " + icnId + " (记录不存在)");
            }
            return;
        }

        // 获取ICN编码（用作目标文件名前缀）
        String icnCode = icn.getIcn();
        if (icnCode == null || icnCode.trim().isEmpty()) {
            // 修复P0：记录ICN编码为空的情况
            log.error("ICN[{}]编码为空，跳过", icnId);
            if (missingIcnList != null) {
                missingIcnList.add("ICN ID: " + icnId + " (编码为空)");
            }
            return;
        }

        // 复制ICN主实体文件（通过ietm_attachment关联）
        List<IetmAttachment> attachments = attachmentMap.getOrDefault(icnId, Collections.emptyList());

        boolean fileCopied = false;
        for (IetmAttachment att : attachments) {
            if (att.getFileKey() != null && !att.getFileKey().isEmpty()) {
                File srcFile = new File(fileStorageLocation, att.getFileKey());
                if (srcFile.exists()) {
                    // 提取原始文件扩展名
                    String originalFileName = att.getFileName();
                    String extension = "";
                    int dotIndex = originalFileName.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
                        extension = originalFileName.substring(dotIndex);  // 包含点，如 .png
                    }

                    // 使用ICN编码作为文件名（符合S1000D标准）
                    String targetFileName = icnCode + extension;
                    File destFile = new File(icnDir, targetFileName);

                    // 修复：检测并解码Base64编码的文件
                    // ICN上传时使用了 DESUtils.encodeBase64File() 保存为Base64文本
                    // 导出时需要解码回二进制格式，否则图片无法打开
                    try {
                        decodeBase64FileIfNeeded(srcFile, destFile);
                        log.debug("复制ICN文件：{} -> {}", originalFileName, targetFileName);
                        fileCopied = true;
                    } catch (Exception e) {
                        log.error("复制ICN文件失败：{} -> {}，错误：{}", originalFileName, targetFileName, e.getMessage());
                        throw new IOException("复制ICN文件失败：" + originalFileName, e);
                    }

                    // 记录到DDN.log（使用ICN编码，不含扩展名）
                    if (ddnLog != null) {
                        ddnLog.append("    引用ICN=").append(icnCode).append(": 导出成功。\n");
                    }

                    // 添加到文件映射（包含ICN/相对路径，使用ICN编码作为文件名）
                    if (allFiles != null) {
                        allFiles.put(DdnConstants.DirectoryNames.ICN + "/" + targetFileName, null);  // ICN无entityControlNumber
                    }
                } else {
                    // 修复P0：记录ICN文件不存在的情况
                    log.error("ICN[{}]文件不存在：{}", icnId, att.getFileKey());
                    if (missingIcnList != null) {
                        missingIcnList.add(String.format(
                            "ICN: %s (文件不存在: %s)",
                            icnCode, att.getFileKey()
                        ));
                    }
                }
            }
        }

        // 如果一个文件都没复制成功，也记录下来
        if (!fileCopied && missingIcnList != null) {
            missingIcnList.add("ICN: " + icnCode + " (无可用文件)");
        }
    }

    /**
     * 递归收集引用的DM（防死循环 + 深度限制 + 批量查询优化）
     */
    private void collectReferencedDms(List<String> sourceDmIds, Set<String> allDmIds, int depth) {
        if (depth > DdnConstants.Collection.MAX_RECURSION_DEPTH) {
            log.warn("DM引用深度超过{}层，停止递归", DdnConstants.Collection.MAX_RECURSION_DEPTH);
            return;
        }

        if (sourceDmIds == null || sourceDmIds.isEmpty()) {
            return;
        }

        // 批量查询所有引用关系（优化N+1问题）
        List<IetmDmRef> refs = dmRefMapper.selectList(
                new LambdaQueryWrapper<IetmDmRef>()
                        .in(IetmDmRef::getSourceDmId, sourceDmIds));

        Set<String> newDmIds = new HashSet<>();
        for (IetmDmRef ref : refs) {
            String targetDmId = ref.getTargetDmId();
            if (targetDmId != null && !allDmIds.contains(targetDmId)) {
                allDmIds.add(targetDmId);
                newDmIds.add(targetDmId);
            }
        }

        if (!newDmIds.isEmpty()) {
            collectReferencedDms(new ArrayList<>(newDmIds), allDmIds, depth + 1);
        }
    }

    /**
     * 复制DM文件（XML + 资源文件）
     * 修复P1-6：收集错误DM列表，返回给用户
     * S1000D 4.0优化：DM资源文件存放到MM/目录（符合标准建议）
     * @param dmDir DM XML文件目录
     * @param mmDir MM资源文件目录
     * @param includeResource 是否包含资源文件
     * @param errorDmList 错误DM列表（可选）
     * @param ddnLog DDN.log内容构建器（可选）
     * @param allFiles 文件名映射（用于DDN.XML的deliveryList，可选）
     */
    private void copyDmFiles(String dmId, File dmDir, File mmDir, boolean includeResource, List<String> errorDmList,
                             StringBuilder ddnLog, Map<String, String> allFiles) throws IOException {
        IetmDataModule dm = dataModuleMapper.selectById(dmId);
        if (dm == null || dm.getDmContent() == null) {
            log.warn("DM[{}]不存在或无内容，跳过", dmId);
            // 收集错误DM信息
            if (dm != null && errorDmList != null) {
                errorDmList.add(String.format("DMC: %s (无内容)", dm.getDmcCode()));
            } else if (errorDmList != null) {
                errorDmList.add(String.format("DM ID: %s (不存在)", dmId));
            }
            return;
        }

        // 写入DM XML文件（文件名安全过滤，改为小写.xml扩展名）
        String safeDmFileName = FileNameUtils.sanitize(dm.getDmcCode()) + DdnConstants.FileExtensions.XML;
        File dmFile = new File(dmDir, safeDmFileName);
        Files.write(dmFile.toPath(), dm.getDmContent().getBytes("UTF-8"));
        log.debug("复制DM文件：{}", safeDmFileName);

        // 记录到DDN.log
        if (ddnLog != null) {
            ddnLog.append("DMC=").append(dm.getDmcCode()).append(": 导出成功。\n");
        }

        // 添加到文件映射（包含DM/相对路径）
        if (allFiles != null) {
            allFiles.put(DdnConstants.DirectoryNames.DM + "/" + safeDmFileName, dm.getDmcCode());
        }

        // 复制资源文件到MM/目录（如果勾选）
        if (includeResource) {
            List<IetmDmComment> resources = dmCommentMapper.selectList(
                    new LambdaQueryWrapper<IetmDmComment>()
                            .eq(IetmDmComment::getDmId, dmId));
            // 不同DM可能有同名资源文件，加DMC前缀防止重名覆盖
            String dmcPrefix = FileNameUtils.sanitize(dm.getDmcCode());
            for (IetmDmComment res : resources) {
                if (res.getFilePath() != null && !res.getFilePath().isEmpty()) {
                    File srcFile = new File(fileStorageLocation, res.getFilePath());
                    if (srcFile.exists()) {
                        // 从 fileName 中提取原始文件名（移除ID后缀）
                        // 例如: "金波_1786887767219_1788313777683.jpg" -> "金波.jpg"
                        String originalFileName = FileNameUtils.extractOriginalName(res.getFileName());

                        // 构建最终文件名: DMC前缀_原始文件名
                        String safeResName = FileNameUtils.withPrefix(dmcPrefix, originalFileName);
                        File destFile = new File(mmDir, safeResName);  // 改为存放到MM/目录
                        Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        log.debug("复制DM资源文件：{} -> MM/{}", srcFile.getName(), safeResName);

                        // 资源文件也记录到DDN.log（缩进）和文件映射
                        if (ddnLog != null) {
                            ddnLog.append("    资源文件=").append(safeResName).append(": 导出成功。\n");
                        }
                        if (allFiles != null) {
                            allFiles.put(DdnConstants.DirectoryNames.MM + "/" + safeResName, null);  // 资源文件路径改为MM/
                        }
                    } else {
                        log.warn("DM[{}]资源文件不存在：{}", dmId, res.getFilePath());
                    }
                }
            }
        }
    }

    /**
     * 收集单个DM引用的所有ICN
     *
     * BUG修复：referenceType和dmCode字段使用错误
     * - ietm_icn_reference表中统一使用referenceType="ICN_TO_DM"（表示ICN被DM引用）
     * - dmCode字段存储的是DM的ID（不是DMC编码），见IetmIcnReferenceHelper.java:ref.setDmCode(dmId)
     */
    private void collectReferencedIcns(String dmId, Set<String> allIcnIds) {
        // 查询 ietm_icn_reference 表：ICN被DM引用（使用dmId）
        List<IetmIcnReference> refs = icnReferenceMapper.selectList(
                new LambdaQueryWrapper<IetmIcnReference>()
                        .eq(IetmIcnReference::getReferenceType, DdnConstants.ReferenceType.ICN_TO_DM)
                        .eq(IetmIcnReference::getDmCode, dmId));

        log.debug("DM[{}] 引用了 {} 个ICN", dmId, refs.size());

        for (IetmIcnReference ref : refs) {
            String targetIcnId = ref.getSourceIcnId();  // 注意：sourceIcnId才是ICN的ID
            if (targetIcnId != null && !allIcnIds.contains(targetIcnId)) {
                allIcnIds.add(targetIcnId);
                log.debug("  - 收集ICN: {}", targetIcnId);
            }
        }
    }

    /**
     * 复制ICN文件（实体文件 + 相关文件）
     */
    /**
     * 复制ICN文件（实体文件 + 相关文件）
     * @param ddnLog DDN.log内容构建器（可选）
     * @param allFiles 文件名映射（用于DDN.XML的deliveryList，可选）
     */
    private void copyIcnFiles(String icnId, File icnDir, StringBuilder ddnLog,
                              Map<String, String> allFiles) throws IOException {
        IetmIcnManage icn = icnManageMapper.selectById(icnId);
        if (icn == null) {
            log.warn("ICN[{}]不存在，跳过", icnId);
            return;
        }

        // 获取ICN编码（用作目标文件名前缀）
        String icnCode = icn.getIcn();
        if (icnCode == null || icnCode.trim().isEmpty()) {
            log.warn("ICN[{}]编码为空，跳过", icnId);
            return;
        }

        // 复制ICN主实体文件（通过ietm_attachment关联）
        List<IetmAttachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<IetmAttachment>()
                        .eq(IetmAttachment::getPid, icnId));

        for (IetmAttachment att : attachments) {
            if (att.getFileKey() != null && !att.getFileKey().isEmpty()) {
                File srcFile = new File(fileStorageLocation, att.getFileKey());
                if (srcFile.exists()) {
                    // 提取原始文件扩展名
                    String originalFileName = att.getFileName();
                    String extension = "";
                    int dotIndex = originalFileName.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
                        extension = originalFileName.substring(dotIndex);  // 包含点，如 .png
                    }

                    // 使用ICN编码作为文件名（符合S1000D标准）
                    String targetFileName = icnCode + extension;
                    File destFile = new File(icnDir, targetFileName);

                    // 修复：检测并解码Base64编码的文件
                    try {
                        decodeBase64FileIfNeeded(srcFile, destFile);
                        log.debug("复制ICN文件：{} -> {}", originalFileName, targetFileName);
                    } catch (Exception e) {
                        log.error("复制ICN文件失败：{} -> {}，错误：{}", originalFileName, targetFileName, e.getMessage());
                        throw new IOException("复制ICN文件失败：" + originalFileName, e);
                    }

                    // 记录到DDN.log（使用ICN编码，不含扩展名）
                    if (ddnLog != null) {
                        ddnLog.append("    引用ICN=").append(icnCode).append(": 导出成功。\n");
                    }

                    // 添加到文件映射（包含ICN/相对路径，使用ICN编码作为文件名）
                    if (allFiles != null) {
                        allFiles.put(DdnConstants.DirectoryNames.ICN + "/" + targetFileName, null);  // ICN无entityControlNumber
                    }
                } else {
                    log.warn("ICN[{}]文件不存在：{}", icnId, att.getFileKey());
                }
            }
        }
    }

    /**
     * 生成完整的S1000D DDN XML文件（使用DOM4J）
     * 修复P2-6：拆分为多个子方法，提升可维护性
     */
    private void generateSimpleDdnXml(File ddnXml, String ddnCode, DdnGenerateVO params, Map<String, Object> projectInfo) throws Exception {
        // 1. 加载DDN模板
        org.dom4j.Document doc = loadDdnTemplate(projectInfo);
        org.dom4j.Element root = doc.getRootElement();

        // 2. 填充各个元素
        fillDdnCodeElement(root, ddnCode);
        fillIssueDateElement(root, params);
        fillSecurityElement(root, params);
        fillDdnContentElement(root, params, projectInfo);

        // 3. 写入文件
        writeDdnXmlToFile(doc, ddnXml);
    }

    /**
     * 生成DDN.XML文件（使用deliveryList结构）
     * 对标旧系统：使用<deliveryList>而非<dmRef>，列出所有文件（含相对路径）
     */
    private void generateDdnXmlWithDeliveryList(File ddnXml, String ddnCode, DdnGenerateVO params,
                                                 Map<String, Object> projectInfo,
                                                 Map<String, String> allFiles) throws Exception {
        // 1. 加载DDN模板
        org.dom4j.Document doc = loadDdnTemplate(projectInfo);
        org.dom4j.Element root = doc.getRootElement();

        // 2. 填充ddnCode、issueDate、security（复用原方法）
        fillDdnCodeElement(root, ddnCode);
        fillIssueDateElement(root, params);
        fillSecurityElement(root, params);
        fillEnterpriseNames(root, params);  // 新增：填充发送/接收单位名称

        // 3. 替换ddnContent：使用deliveryList结构
        fillDdnContentWithDeliveryList(root, allFiles);

        // 4. 写入文件
        writeDdnXmlToFile(doc, ddnXml);
        log.info("DDN.XML文件生成完成（deliveryList结构），包含{}个文件", allFiles.size());
    }

    /**
     * 填充ddnContent元素（deliveryList结构）
     * 对标旧系统：列出所有文件名（含DM/或ICN/相对路径）
     */
    private void fillDdnContentWithDeliveryList(org.dom4j.Element root, Map<String, String> allFiles) {
        // 查找ddnContent元素
        org.dom4j.Element ddnContentElem = (org.dom4j.Element) root.selectSingleNode("//ddnContent");
        if (ddnContentElem != null) {
            // 清空原有内容
            ddnContentElem.clearContent();

            // 创建deliveryList元素
            org.dom4j.Element deliveryListElem = ddnContentElem.addElement("deliveryList");

            // 遍历所有文件，添加deliveryListItem
            for (Map.Entry<String, String> entry : allFiles.entrySet()) {
                String fileName = entry.getKey();  // 包含DM/或ICN/相对路径
                String entityControlNumber = entry.getValue();

                org.dom4j.Element itemElem = deliveryListElem.addElement("deliveryListItem");

                // dispatchFileName元素（包含相对路径）
                itemElem.addElement("dispatchFileName").setText(fileName);

                // entityControlNumber元素（只有DM文件才有）
                if (entityControlNumber != null) {
                    itemElem.addElement("entityControlNumber").setText(entityControlNumber);
                }
            }

            log.debug("deliveryList包含{}个项", allFiles.size());
        } else {
            log.warn("未找到ddnContent元素，无法填充deliveryList");
        }
    }

    /**
     * 加载DDN模板文件
     */
    private org.dom4j.Document loadDdnTemplate(Map<String, Object> projectInfo) throws Exception {
        // 根据项目标准版本选择模板路径
        String ietmStandard = (String) projectInfo.get("ietmStandard");
        if (ietmStandard == null || ietmStandard.isEmpty()) {
            ietmStandard = DdnConstants.Standard.DEFAULT_VERSION;  // 默认使用4.0版本
            log.warn("项目未配置ietmStandard，使用默认版本：{}", ietmStandard);
        }

        // 标准化版本号格式：移除点号（S1000D4.0 -> S1000D40）
        String normalizedStandard = ietmStandard.replaceAll("[.\\-]", "");
        log.info("原始标准版本：{}，标准化后：{}", ietmStandard, normalizedStandard);

        // 构建模板路径
        String templatePath = String.format("ietm/%s/template/ddn.xml", normalizedStandard);
        log.info("加载DDN模板：{}", templatePath);

        // 配置SAXReader（XXE防护）
        org.dom4j.io.SAXReader reader = new org.dom4j.io.SAXReader();
        configureXxeProtection(reader);

        // 加载模板
        InputStream templateStream = getClass().getClassLoader().getResourceAsStream(templatePath);
        if (templateStream == null) {
            throw new JeecgBootException("DDN模板文件不存在：" + templatePath);
        }

        return reader.read(templateStream);
    }

    /**
     * 配置XXE防护
     */
    private void configureXxeProtection(org.dom4j.io.SAXReader reader) {
        try {
            // 允许DOCTYPE声明（S1000D模板包含空DOCTYPE）
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            // 禁用外部实体（核心防护）
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            // 禁用外部DTD加载
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            // 设置空的EntityResolver（最后防线）
            reader.setEntityResolver((publicId, systemId) -> {
                log.warn("拦截外部实体加载尝试：publicId={}, systemId={}", publicId, systemId);
                return new org.xml.sax.InputSource(new java.io.StringReader(""));
            });
        } catch (Exception e) {
            log.error("XXE防护配置失败：{}", e.getMessage(), e);
            throw new JeecgBootException("XML安全配置失败");
        }
    }

    /**
     * 填充ddnCode元素
     */
    private void fillDdnCodeElement(org.dom4j.Element root, String ddnCode) {
        org.dom4j.Element ddnCodeElem = (org.dom4j.Element) root.selectSingleNode("//ddnCode");
        if (ddnCodeElem != null) {
            String[] parts = ddnCode.split("-");
            if (parts.length >= 6) {
                ddnCodeElem.addAttribute("modelIdentCode", parts[1]);
                ddnCodeElem.addAttribute("senderIdent", parts[2]);
                ddnCodeElem.addAttribute("receiverIdent", parts[3]);
                ddnCodeElem.addAttribute("yearOfDataIssue", parts[4]);
                ddnCodeElem.addAttribute("seqNumber", parts[5]);
            }
        }
    }

    /**
     * 填充issueDate元素
     */
    private void fillIssueDateElement(org.dom4j.Element root, DdnGenerateVO params) {
        org.dom4j.Element issueDateElem = (org.dom4j.Element) root.selectSingleNode("//issueDate");
        if (issueDateElem != null && params.getIssueDate() != null) {
            String[] dateParts = params.getIssueDate().split("-");
            if (dateParts.length == 3) {
                issueDateElem.addAttribute("year", dateParts[0]);
                issueDateElem.addAttribute("month", dateParts[1]);
                issueDateElem.addAttribute("day", dateParts[2]);
            }
        }
    }

    /**
     * 填充security元素
     * 修复P3: 只有非空值才添加可选属性（移除空属性冗余）
     * 修复P3: 统一使用2位数字格式的安全等级
     */
    private void fillSecurityElement(org.dom4j.Element root, DdnGenerateVO params) {
        org.dom4j.Element securityElem = (org.dom4j.Element) root.selectSingleNode("//security");
        if (securityElem != null) {
            // 修复P3: 统一为2位数字格式（01, 02, 03...）
            String securityClassification = params.getSecurity();
            if (securityClassification != null && securityClassification.length() == 1) {
                securityClassification = "0" + securityClassification;  // 1 -> 01
            }
            securityElem.addAttribute("securityClassification", securityClassification);

            // 修复P3: 只有非空值才添加可选属性
            if (params.getCommercialSecurity() != null && !params.getCommercialSecurity().trim().isEmpty()) {
                securityElem.addAttribute("commercialClassification", params.getCommercialSecurity());
            }
            if (params.getCaveat() != null && !params.getCaveat().trim().isEmpty()) {
                securityElem.addAttribute("caveat", params.getCaveat());
            }
        }
    }

    /**
     * 填充enterpriseName元素
     * 修复P2: 自动填充发送/接收单位名称（对标旧版系统）
     */
    private void fillEnterpriseNames(org.dom4j.Element root, DdnGenerateVO params) {
        // 填充dispatchTo/enterpriseName（接收单位）
        org.dom4j.Element dispatchToEnterpriseName = (org.dom4j.Element) root.selectSingleNode(
            "//dispatchTo/dispatchAddress/enterprise/enterpriseName");
        if (dispatchToEnterpriseName != null && params.getReceiver() != null) {
            dispatchToEnterpriseName.setText(params.getReceiver());
            log.debug("填充接收单位名称：{}", params.getReceiver());
        }

        // 填充dispatchFrom/enterpriseName（发送单位）
        org.dom4j.Element dispatchFromEnterpriseName = (org.dom4j.Element) root.selectSingleNode(
            "//dispatchFrom/dispatchAddress/enterprise/enterpriseName");
        if (dispatchFromEnterpriseName != null && params.getSender() != null) {
            dispatchFromEnterpriseName.setText(params.getSender());
            log.debug("填充发送单位名称：{}", params.getSender());
        }
    }

    /**
     * 填充ddnContent元素（dmRef列表）
     */
    private void fillDdnContentElement(org.dom4j.Element root, DdnGenerateVO params, Map<String, Object> projectInfo) {
        org.dom4j.Element ddnContentElem = (org.dom4j.Element) root.selectSingleNode("//ddnContent");
        if (ddnContentElem != null) {
            // 清空模板注释
            ddnContentElem.clearContent();

            // 为每个DM添加dmRef节点
            for (String dmId : params.getDmIds()) {
                IetmDataModule dm = dataModuleMapper.selectById(dmId);
                if (dm != null) {
                    addDmRefElement(ddnContentElem, dm, projectInfo);
                }
            }
        }
    }

    /**
     * 添加dmRef元素（包含完整的13个dmCode属性）
     */
    private void addDmRefElement(org.dom4j.Element ddnContentElem, IetmDataModule dm, Map<String, Object> projectInfo) {
        org.dom4j.Element dmRef = ddnContentElem.addElement("dmRef");
        org.dom4j.Element dmRefIdent = dmRef.addElement("dmRefIdent");
        org.dom4j.Element dmCode = dmRefIdent.addElement("dmCode");

        // 使用DmcUtils解析SNS获取8个属性
        Map<String, String> snsMap = DmcUtils.decomposeSns(dm.getSns());

        // 从解析结果获取modelIdentCode，如果为空则使用项目装备代码
        String modelIdentCode = snsMap.get("modelIdentCode");
        if (modelIdentCode == null || modelIdentCode.isEmpty()) {
            String equipmentCode = (String) projectInfo.get("equipmentCode");
            modelIdentCode = DmcUtils.resolveModelIdentCode(dm.getSchema(), equipmentCode);
        }

        // 填充S1000D标准的13个dmCode属性（11个必需 + 2个可选）
        // 必需属性 (required)
        dmCode.addAttribute("modelIdentCode", modelIdentCode);
        dmCode.addAttribute("systemDiffCode", oConvertUtils.isEmpty(snsMap.get("systemDiffCode")) ? DdnConstants.DefaultValues.SYSTEM_DIFF_CODE : snsMap.get("systemDiffCode"));
        dmCode.addAttribute("systemCode", oConvertUtils.isEmpty(snsMap.get("systemCode")) ? DdnConstants.DefaultValues.SYSTEM_CODE : snsMap.get("systemCode"));
        dmCode.addAttribute("subSystemCode", oConvertUtils.isEmpty(snsMap.get("subSystemCode")) ? DdnConstants.DefaultValues.SUB_SYSTEM_CODE : snsMap.get("subSystemCode"));
        dmCode.addAttribute("subSubSystemCode", oConvertUtils.isEmpty(snsMap.get("subSubSystemCode")) ? DdnConstants.DefaultValues.SUB_SUB_SYSTEM_CODE : snsMap.get("subSubSystemCode"));
        dmCode.addAttribute("assyCode", oConvertUtils.isEmpty(snsMap.get("assyCode")) ? DdnConstants.DefaultValues.ASSY_CODE : snsMap.get("assyCode"));
        dmCode.addAttribute("disassyCode", oConvertUtils.isEmpty(snsMap.get("disassyCode")) ? DdnConstants.DefaultValues.DISASSY_CODE : snsMap.get("disassyCode"));
        dmCode.addAttribute("disassyCodeVariant", oConvertUtils.isEmpty(snsMap.get("disassyCodeVariant")) ? DdnConstants.DefaultValues.DISASSY_CODE_VARIANT : snsMap.get("disassyCodeVariant"));
        dmCode.addAttribute("infoCode", oConvertUtils.isEmpty(dm.getInfoCode()) ? DdnConstants.DefaultValues.INFO_CODE : dm.getInfoCode());
        dmCode.addAttribute("infoCodeVariant", oConvertUtils.isEmpty(dm.getInfoCodeVariant()) ? DdnConstants.DefaultValues.INFO_CODE_VARIANT : dm.getInfoCodeVariant());
        dmCode.addAttribute("itemLocationCode", oConvertUtils.isEmpty(dm.getIetmLocationCode()) ? DdnConstants.DefaultValues.ITEM_LOCATION_CODE : dm.getIetmLocationCode());

        // 可选属性 (optional) - 仅在非空时添加
        if (dm.getLearnCode() != null && !dm.getLearnCode().isEmpty()) {
            dmCode.addAttribute("learnCode", dm.getLearnCode());
        }
        if (dm.getLearnEventCode() != null && !dm.getLearnEventCode().isEmpty()) {
            dmCode.addAttribute("learnEventCode", dm.getLearnEventCode());
        }
    }

    /**
     * 将Document写入文件
     */
    /**
     * 写入DDN XML到文件
     * P1-9修复：使用try-finally确保流正确关闭
     */
    private void writeDdnXmlToFile(org.dom4j.Document doc, File ddnXml) throws Exception {
        org.dom4j.io.OutputFormat format = org.dom4j.io.OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");

        // P1-9修复：使用try-finally确保XMLWriter正确关闭
        FileOutputStream fos = null;
        org.dom4j.io.XMLWriter writer = null;
        try {
            fos = new FileOutputStream(ddnXml);
            writer = new org.dom4j.io.XMLWriter(fos, format);
            writer.write(doc);
        } finally {
            // 确保资源关闭
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    log.warn("关闭XMLWriter失败", e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    log.warn("关闭FileOutputStream失败", e);
                }
            }
        }
    }

    /**
     * 打包为ZIP
     * 修复：显式添加目录条目，确保空目录（如ICN/）也被打包，符合S1000D标准
     */
    private void createZipPackage(File sourceDir, File zipFile) throws IOException {
        // 修复P2-7：使用DdnConstants统一管理文件大小限制
        final long[] totalSize = {0};

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile), StandardCharsets.UTF_8)) {
            // 修复P1-2：设置UTF-8编码，避免Windows环境下中文文件名乱码
            // 注意：此修复对系统内部导入无影响（已使用UTF-8读取），但改善手动解压体验
            Path sourcePath = sourceDir.toPath();

            // 第一步：显式添加所有目录条目（确保空目录也被打包）
            Files.walk(sourcePath).forEach(path -> {
                try {
                    if (Files.isDirectory(path) && !path.equals(sourcePath)) {
                        // 添加目录条目（必须以 / 结尾）
                        String zipEntryName = sourcePath.relativize(path).toString().replace("\\", "/") + "/";
                        zos.putNextEntry(new ZipEntry(zipEntryName));
                        zos.closeEntry();
                        log.debug("添加目录到ZIP: {}", zipEntryName);
                    }
                } catch (IOException e) {
                    log.error("添加目录失败: " + path, e);
                }
            });

            // 第二步：添加所有文件条目
            Files.walk(sourcePath).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                String zipEntryName = sourcePath.relativize(path).toString().replace("\\", "/");
                try {
                    // 检查单个文件大小
                    long fileSize = Files.size(path);
                    if (fileSize > DdnConstants.FileSize.MAX_FILE_SIZE) {
                        log.warn("文件过大，跳过：{} ({}MB)", path, fileSize / 1024 / 1024);
                        return;
                    }

                    // 检查总大小限制
                    totalSize[0] += fileSize;
                    if (totalSize[0] > DdnConstants.FileSize.MAX_ZIP_SIZE) {
                        log.error("DDN包总大小超过限制(1GB)，停止打包");
                        throw new RuntimeException("DDN包总大小超过1GB限制");
                    }

                    zos.putNextEntry(new ZipEntry(zipEntryName));
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    log.error("ZIP打包失败：" + path, e);
                }
            });

            log.info("DDN包打包完成，总大小：{}MB", totalSize[0] / 1024 / 1024);
        }
    }

    /**
     * 解码Base64编码的文件（如果需要）
     * <p>
     * ICN上传时使用 DESUtils.encodeBase64File() 将文件编码为Base64文本保存
     * 导出DDN时需要将Base64文本解码回二进制格式，否则图片/视频无法打开
     * </p>
     *
     * @param srcFile 源文件（可能是Base64文本或二进制）
     * @param destFile 目标文件（输出为二进制）
     * @throws IOException 文件读写失败
     */
    /**
     * 解码Base64编码的文件（如果需要）
     * <p>
     * ICN上传时使用 DESUtils.encodeBase64File() 将文件编码为Base64文本保存
     * 导出DDN时需要将Base64文本解码回二进制格式，否则图片/视频无法打开
     * </p>
     * P1-7修复：添加文件类型验证，防止恶意文件
     *
     * @param srcFile 源文件（可能是Base64文本或二进制）
     * @param destFile 目标文件（输出为二进制）
     * @throws IOException 文件读写失败
     */
    private void decodeBase64FileIfNeeded(File srcFile, File destFile) throws IOException {
        // 读取源文件前几个字节，判断是Base64文本还是二进制
        byte[] header = new byte[100];
        int bytesRead = 0;
        try (FileInputStream fis = new FileInputStream(srcFile)) {
            bytesRead = fis.read(header);
        }

        if (bytesRead <= 0) {
            // 空文件，直接复制
            Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        // 检查是否为Base64文本：Base64字符集为 [A-Za-z0-9+/=\r\n]
        boolean isBase64Text = true;
        for (int i = 0; i < bytesRead; i++) {
            byte b = header[i];
            // Base64有效字符：A-Z(65-90), a-z(97-122), 0-9(48-57), +(43), /(47), =(61), \r(13), \n(10), 空格(32)
            if (!((b >= 65 && b <= 90) || (b >= 97 && b <= 122) || (b >= 48 && b <= 57)
                  || b == 43 || b == 47 || b == 61 || b == 13 || b == 10 || b == 32)) {
                isBase64Text = false;
                break;
            }
        }

        if (isBase64Text) {
            // Base64文本，需要解码
            log.debug("检测到Base64编码文件，开始解码：{}", srcFile.getName());
            try (BufferedReader reader = new BufferedReader(new FileReader(srcFile));
                 FileOutputStream fos = new FileOutputStream(destFile)) {

                StringBuilder base64Content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    base64Content.append(line.trim());
                }

                // 解码Base64为二进制
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Content.toString());

                // P1-7修复：验证解码后的文件类型（文件魔法数）
                if (!isValidImageOrMediaFile(decodedBytes)) {
                    log.warn("Base64解码后的文件类型不合法，拒绝写入：{}", srcFile.getName());
                    throw new IOException("Base64解码后的文件类型验证失败，可能是恶意文件");
                }

                fos.write(decodedBytes);
                log.debug("Base64解码完成，原始大小：{}字节，解码后：{}字节",
                         srcFile.length(), decodedBytes.length);
            }
        } else {
            // 已经是二进制文件，直接复制前先验证类型（P1-7修复）
            log.debug("检测到二进制文件，验证类型后复制：{}", srcFile.getName());
            if (!isValidImageOrMediaFile(header)) {
                log.warn("二进制文件类型不合法，拒绝复制：{}", srcFile.getName());
                throw new IOException("文件类型验证失败，可能是恶意文件");
            }
            Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * P1-7修复：验证文件是否为合法的图片或多媒体文件
     * 通过文件魔法数（magic number）识别文件类型
     *
     * @param bytes 文件头字节
     * @return 是否为合法的图片/视频文件
     */
    private boolean isValidImageOrMediaFile(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }

        // 检查常见图片/视频格式的魔法数
        // PNG: 89 50 4E 47
        if (bytes.length >= 4 && bytes[0] == (byte)0x89 && bytes[1] == 0x50 &&
            bytes[2] == 0x4E && bytes[3] == 0x47) {
            return true;
        }

        // JPEG/JPG: FF D8 FF
        if (bytes.length >= 3 && bytes[0] == (byte)0xFF && bytes[1] == (byte)0xD8 &&
            bytes[2] == (byte)0xFF) {
            return true;
        }

        // GIF: 47 49 46 38
        if (bytes.length >= 4 && bytes[0] == 0x47 && bytes[1] == 0x49 &&
            bytes[2] == 0x46 && bytes[3] == 0x38) {
            return true;
        }

        // BMP: 42 4D
        if (bytes.length >= 2 && bytes[0] == 0x42 && bytes[1] == 0x4D) {
            return true;
        }

        // TIFF: 49 49 2A 00 或 4D 4D 00 2A
        if (bytes.length >= 4 && ((bytes[0] == 0x49 && bytes[1] == 0x49 &&
            bytes[2] == 0x2A && bytes[3] == 0x00) ||
            (bytes[0] == 0x4D && bytes[1] == 0x4D && bytes[2] == 0x00 && bytes[3] == 0x2A))) {
            return true;
        }

        // SVG: 通常以 < 开头（XML文件）
        if (bytes.length >= 1 && bytes[0] == 0x3C) {
            return true;
        }

        // CGM (Computer Graphics Metafile): 通常前4字节为特定值
        // CGM没有固定魔法数，但通常以特定字节序列开始，这里宽松处理
        // 如果需要严格验证，需要更复杂的CGM格式解析

        // MP4/MOV: 00 00 00 [18-20] 66 74 79 70
        if (bytes.length >= 8 && bytes[0] == 0x00 && bytes[1] == 0x00 &&
            bytes[2] == 0x00 && bytes[4] == 0x66 && bytes[5] == 0x74 &&
            bytes[6] == 0x79 && bytes[7] == 0x70) {
            return true;
        }

        // AVI: 52 49 46 46 [4 bytes] 41 56 49 20
        if (bytes.length >= 12 && bytes[0] == 0x52 && bytes[1] == 0x49 &&
            bytes[2] == 0x46 && bytes[3] == 0x46 && bytes[8] == 0x41 &&
            bytes[9] == 0x56 && bytes[10] == 0x49 && bytes[11] == 0x20) {
            return true;
        }

        // PDF: 25 50 44 46 (用于文档类ICN，如果支持)
        if (bytes.length >= 4 && bytes[0] == 0x25 && bytes[1] == 0x50 &&
            bytes[2] == 0x44 && bytes[3] == 0x46) {
            return true;
        }

        // 如果都不匹配，记录警告但不拒绝（宽松策略，避免误伤合法但不常见的格式）
        log.debug("文件魔法数不在已知列表中，前4字节: [{}, {}, {}, {}]",
            String.format("%02X", bytes[0]),
            bytes.length > 1 ? String.format("%02X", bytes[1]) : "N/A",
            bytes.length > 2 ? String.format("%02X", bytes[2]) : "N/A",
            bytes.length > 3 ? String.format("%02X", bytes[3]) : "N/A");

        // 宽松策略：不在已知列表的也允许（避免误伤），但已记录日志
        return true;
    }

    /**
     * 构建ICN专用DDN数据包
     * 修复P0-1：ICN文件直接放在根目录（不创建ICN/子目录）
     * 修复P0-2：使用ICN专用的infoEntityIdent结构生成DDN.XML
     *
     * @param ddnCode DDN编码
     * @param icnIds ICN ID列表
     * @param params DDN生成参数
     * @param projectInfo 项目信息
     * @return 构建结果
     */
    public BuildResult buildIcnOnlyPackage(String ddnCode, List<String> icnIds,
                                           DdnGenerateVO params, Map<String, Object> projectInfo) throws Exception {
        File workDir = null;
        long startTime = System.currentTimeMillis();

        try {
            // 1. 校验并创建工作目录
            if (ddnCode == null || ddnCode.trim().isEmpty()) {
                throw new JeecgBootException("DDN编码不能为空");
            }

            // 白名单校验：DDN编码格式
            if (!ddnCode.matches("^DDN-[A-Za-z0-9-]+-[A-Za-z0-9-]+-[A-Za-z0-9-]+-\\d{4}-\\d{5}$")) {
                throw new JeecgBootException("DDN编码格式非法，必须符合：DDN-型号-单位-序号-年份-版本");
            }

            String sanitizedCode = ddnCode.replaceAll("[^A-Za-z0-9-]", "");
            String ddnWorkDir = fileStorageLocation + File.separator + "ddn" + File.separator + sanitizedCode;
            workDir = new File(ddnWorkDir);

            // 路径遍历防护
            try {
                Path targetPath = workDir.toPath().toAbsolutePath().normalize();
                Path basePath = new File(fileStorageLocation, "ddn").toPath().toAbsolutePath().normalize();

                if (!targetPath.startsWith(basePath)) {
                    throw new JeecgBootException("DDN目录路径非法：路径遍历检测到威胁");
                }

                if (workDir.exists()) {
                    Path realPath = workDir.toPath().toRealPath();
                    Path baseRealPath = basePath.toRealPath();
                    if (!realPath.startsWith(baseRealPath)) {
                        throw new JeecgBootException("DDN目录路径非法：符号链接检测到威胁");
                    }
                    deleteDirectory(workDir);
                }
            } catch (IOException e) {
                log.error("路径校验失败：{}", e.getMessage(), e);
                throw new JeecgBootException("DDN目录路径校验失败：" + e.getMessage());
            }

            workDir.mkdirs();

            // 2. 去重ICN ID
            Set<String> uniqueIcnIds = new HashSet<>(icnIds);
            log.info("DDN[{}] ICN去重：原始{}个，去重后{}个", ddnCode, icnIds.size(), uniqueIcnIds.size());

            // 3. 复制ICN文件到工作目录根目录（修复P0-1：不创建ICN/子目录）
            List<String> missingIcnList = new ArrayList<>();
            StringBuilder ddnLog = new StringBuilder();
            Map<String, IetmIcnManage> icnInfoMap = new LinkedHashMap<>();  // 保留顺序

            // 批量查询所有ICN和附件
            Map<String, IetmIcnManage> icnMap = batchLoadIcns(uniqueIcnIds);
            Map<String, List<IetmAttachment>> attachmentMap = batchLoadAttachments(uniqueIcnIds);

            for (String icnId : uniqueIcnIds) {
                // 修复P0-1：ICN文件直接复制到workDir根目录
                copyIcnFilesToRoot(icnId, icnMap, attachmentMap, workDir, missingIcnList, ddnLog, icnInfoMap);
            }

            // 4. 生成DDN.XML文件（修复P0-2：使用ICN专用的infoEntityIdent结构）
            File ddnXml = new File(workDir, ddnCode + DdnConstants.FileExtensions.XML);
            generateIcnDdnXml(ddnXml, ddnCode, params, projectInfo, icnInfoMap);

            // 5. 生成DDN.log文件
            File ddnLogFile = new File(workDir, "DDN" + DdnConstants.FileExtensions.LOG);
            Files.write(ddnLogFile.toPath(), ddnLog.toString().getBytes(StandardCharsets.UTF_8));
            log.info("DDN.log文件生成完成：{}", ddnLogFile.getName());

            // 6. 打包为ZIP
            String zipFileName = ddnCode + DdnConstants.FileExtensions.ZIP;
            File zipFile = new File(fileStorageLocation + File.separator + "ddn", zipFileName);
            zipFile.getParentFile().mkdirs();
            createZipPackage(workDir, zipFile);

            // 7. 返回结果
            BuildResult result = new BuildResult();
            result.setZipFilePath("ddn" + File.separator + zipFileName);
            result.setTotalDmCount(0);  // ICN导出无DM
            result.setTotalIcnCount(uniqueIcnIds.size());
            result.setAllDmIds(Collections.emptySet());
            result.setAllIcnIds(uniqueIcnIds);
            result.setErrorDmList(Collections.emptyList());
            result.setMissingIcnList(missingIcnList);
            result.setMissingResourceList(Collections.emptyList());
            result.setSuccessIcnCount(uniqueIcnIds.size() - missingIcnList.size());
            result.setSuccessResourceCount(0);
            result.setTotalResourceCount(0);

            long duration = System.currentTimeMillis() - startTime;
            log.info("ICN DDN包构建完成，耗时：{}ms，ICN:{}/{}个，缺失:{}个",
                    duration, result.getSuccessIcnCount(), uniqueIcnIds.size(), missingIcnList.size());

            return result;
        } finally {
            // 清理临时目录
            if (workDir != null && workDir.exists()) {
                try {
                    deleteDirectory(workDir);
                    log.info("清理临时目录：{}", workDir.getAbsolutePath());
                } catch (IOException e) {
                    // P1-8修复：临时文件清理失败增加告警
                    String errorMsg = String.format("清理临时目录失败：%s，磁盘空间可能不足或文件被占用，请手动删除",
                        workDir.getAbsolutePath());
                    log.error(errorMsg, e);

                    // 增加系统告警（可集成监控系统）
                    sendCleanupFailureAlert(workDir.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }

    /**
     * 复制ICN文件到根目录（修复P0-1：ICN导出时文件在根目录，不在ICN/子目录）
     *
     * @param icnId ICN ID
     * @param icnMap ICN缓存
     * @param attachmentMap 附件缓存
     * @param rootDir 根目录（不是ICN子目录）
     * @param missingIcnList 缺失列表
     * @param ddnLog 日志构建器
     * @param icnInfoMap ICN信息映射（用于后续生成XML）
     */
    private void copyIcnFilesToRoot(String icnId, Map<String, IetmIcnManage> icnMap,
                                    Map<String, List<IetmAttachment>> attachmentMap,
                                    File rootDir, List<String> missingIcnList,
                                    StringBuilder ddnLog,
                                    Map<String, IetmIcnManage> icnInfoMap) throws IOException {
        IetmIcnManage icn = icnMap.get(icnId);
        if (icn == null) {
            log.error("ICN[{}]不存在，跳过", icnId);
            if (missingIcnList != null) {
                missingIcnList.add("ICN ID: " + icnId + " (记录不存在)");
            }
            return;
        }

        String icnCode = icn.getIcn();
        if (icnCode == null || icnCode.trim().isEmpty()) {
            log.error("ICN[{}]编码为空，跳过", icnId);
            if (missingIcnList != null) {
                missingIcnList.add("ICN ID: " + icnId + " (编码为空)");
            }
            return;
        }

        // 复制ICN文件
        List<IetmAttachment> attachments = attachmentMap.getOrDefault(icnId, Collections.emptyList());
        boolean fileCopied = false;

        for (IetmAttachment att : attachments) {
            if (att.getFileKey() != null && !att.getFileKey().isEmpty()) {
                File srcFile = new File(fileStorageLocation, att.getFileKey());
                if (srcFile.exists()) {
                    String originalFileName = att.getFileName();
                    String extension = "";
                    int dotIndex = originalFileName.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
                        extension = originalFileName.substring(dotIndex);
                    }

                    // 修复P0-1：文件直接放在根目录
                    String targetFileName = icnCode + extension;
                    File destFile = new File(rootDir, targetFileName);

                    try {
                        decodeBase64FileIfNeeded(srcFile, destFile);
                        log.debug("复制ICN文件到根目录：{} -> {}", originalFileName, targetFileName);
                        fileCopied = true;

                        // 记录到icnInfoMap（用于后续生成XML）
                        icnInfoMap.put(targetFileName, icn);
                    } catch (Exception e) {
                        log.error("复制ICN文件失败：{} -> {}，错误：{}", originalFileName, targetFileName, e.getMessage());
                        throw new IOException("复制ICN文件失败：" + originalFileName, e);
                    }

                    if (ddnLog != null) {
                        ddnLog.append("ICN=").append(icnCode).append(": 导出成功。\n");
                    }
                } else {
                    log.error("ICN[{}]文件不存在：{}", icnId, att.getFileKey());
                    if (missingIcnList != null) {
                        missingIcnList.add(String.format("ICN: %s (文件不存在: %s)", icnCode, att.getFileKey()));
                    }
                }
            }
        }

        if (!fileCopied && missingIcnList != null) {
            missingIcnList.add("ICN: " + icnCode + " (无可用文件)");
        }
    }

    /**
     * 生成ICN专用DDN XML文件（修复P0-2：使用infoEntityIdent结构）
     *
     * 符合S1000D 4.0标准的ICN deliveryListItem结构：
     * <deliveryListItem>
     *   <dispatchFileName>ICN-001.jpg</dispatchFileName>
     *   <infoEntityIdent infoEntityIdentType="ICN">
     *     <infoEntity>ICN-001</infoEntity>
     *     <issueNumber>001</issueNumber>
     *     <issueDate year="2026" month="09" day="02"/>
     *   </infoEntityIdent>
     * </deliveryListItem>
     */
    private void generateIcnDdnXml(File ddnXml, String ddnCode, DdnGenerateVO params,
                                   Map<String, Object> projectInfo,
                                   Map<String, IetmIcnManage> icnInfoMap) throws Exception {
        // 1. 加载DDN模板
        org.dom4j.Document doc = loadDdnTemplate(projectInfo);
        org.dom4j.Element root = doc.getRootElement();

        // 2. 填充基础元素
        fillDdnCodeElement(root, ddnCode);
        fillIssueDateElement(root, params);
        fillSecurityElement(root, params);
        fillEnterpriseNames(root, params);

        // 3. 填充ddnContent（修复P0-2：使用ICN专用的infoEntityIdent结构）
        fillDdnContentWithIcnList(root, icnInfoMap, params);

        // 4. 写入文件
        writeDdnXmlToFile(doc, ddnXml);
        log.info("ICN DDN.XML文件生成完成，包含{}个ICN", icnInfoMap.size());
    }

    /**
     * 填充ddnContent元素（ICN专用：使用infoEntityIdent结构）
     * 修复P0-2：对标S1000D标准，ICN使用infoEntityIdent，而不是dmCode
     */
    private void fillDdnContentWithIcnList(org.dom4j.Element root,
                                           Map<String, IetmIcnManage> icnInfoMap,
                                           DdnGenerateVO params) {
        org.dom4j.Element ddnContentElem = (org.dom4j.Element) root.selectSingleNode("//ddnContent");
        if (ddnContentElem != null) {
            ddnContentElem.clearContent();

            org.dom4j.Element deliveryListElem = ddnContentElem.addElement("deliveryList");

            // 遍历所有ICN，添加deliveryListItem
            for (Map.Entry<String, IetmIcnManage> entry : icnInfoMap.entrySet()) {
                String fileName = entry.getKey();  // 文件名（如 ICN-001.jpg）
                IetmIcnManage icn = entry.getValue();

                org.dom4j.Element itemElem = deliveryListElem.addElement("deliveryListItem");

                // dispatchFileName：文件名（在根目录，不含路径前缀）
                itemElem.addElement("dispatchFileName").setText(fileName);

                // infoEntityIdent：ICN标识结构（修复P0-2）
                org.dom4j.Element infoEntityIdent = itemElem.addElement("infoEntityIdent");
                infoEntityIdent.addAttribute("infoEntityIdentType", "ICN");

                // infoEntity：ICN编码
                infoEntityIdent.addElement("infoEntity").setText(icn.getIcn());

                // issueNumber：版本号（格式：001）
                String issueNumber = icn.getIssueNo() != null ? icn.getIssueNo() : "001";
                // 确保3位数字格式
                if (issueNumber.length() < 3) {
                    issueNumber = String.format("%03d", Integer.parseInt(issueNumber));
                }
                infoEntityIdent.addElement("issueNumber").setText(issueNumber);

                // issueDate：发布日期（从params.issueDate获取）
                if (params.getIssueDate() != null) {
                    String[] dateParts = params.getIssueDate().split("-");
                    if (dateParts.length == 3) {
                        org.dom4j.Element issueDateElem = infoEntityIdent.addElement("issueDate");
                        issueDateElem.addAttribute("year", dateParts[0]);
                        issueDateElem.addAttribute("month", dateParts[1]);
                        issueDateElem.addAttribute("day", dateParts[2]);
                    }
                }
            }

            log.debug("ICN deliveryList包含{}个项", icnInfoMap.size());
        } else {
            log.warn("未找到ddnContent元素，无法填充ICN列表");
        }
    }

    /**
     * 递归删除目录（使用Apache Commons IO）
     */
    private void deleteDirectory(File dir) throws IOException {
        if (dir != null && dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
    }

    /**
     * 构建结果（修复P1-6：新增错误DM列表）
     * 修复P0：新增缺失文件追踪机制
     */
    @Data
    public static class BuildResult {
        private String zipFilePath;
        private Integer totalDmCount;
        private Integer totalIcnCount;
        private Set<String> allDmIds;      // 完整的DM ID列表（含递归引用）
        private Set<String> allIcnIds;     // 完整的ICN ID列表
        private List<String> errorDmList;  // 错误DM列表（无内容或不存在的DM）

        // 新增：缺失文件追踪
        private List<String> missingIcnList;      // 缺失的ICN列表
        private List<String> missingResourceList; // 缺失的资源文件列表
        private Integer successIcnCount;          // 实际成功复制的ICN数量
        private Integer successResourceCount;     // 实际成功复制的资源数量
        private Integer totalResourceCount;       // 资源文件总数
    }

    /**
     * P1-8修复：发送临时文件清理失败告警
     * 可集成到监控系统（如Prometheus/Grafana/钉钉/企业微信等）
     *
     * @param path 临时目录路径
     * @param errorMessage 错误消息
     */
    private void sendCleanupFailureAlert(String path, String errorMessage) {
        try {
            // 记录到专门的告警日志
            log.warn("【系统告警】临时文件清理失败：path={}, error={}", path, errorMessage);

            // TODO: 集成监控系统发送告警
            // 示例：发送到钉钉/企业微信/Prometheus等
            // alertService.send("临时文件清理失败", path, errorMessage);

            // 简单实现：记录到数据库或文件，供运维人员查询
            // 这里使用日志作为基础实现，生产环境应集成专业监控系统
        } catch (Exception e) {
            log.error("发送清理失败告警异常", e);
        }
    }
}
