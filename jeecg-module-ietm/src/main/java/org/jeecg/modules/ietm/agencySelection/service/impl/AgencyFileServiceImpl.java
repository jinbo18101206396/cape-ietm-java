package org.jeecg.modules.ietm.agencySelection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.ietm.agencySelection.entity.AgencyFile;
import org.jeecg.modules.ietm.agencySelection.mapper.AgencyFileMapper;
import org.jeecg.modules.ietm.agencySelection.service.IAgencyFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @Description: 机构遴选-附件表
 * @Author: jeecg-boot
 * @Date: 2026-04-29
 */
@Service
@Slf4j
public class AgencyFileServiceImpl extends ServiceImpl<AgencyFileMapper, AgencyFile> implements IAgencyFileService {

    @Autowired
    private AgencyFileMapper agencyFileMapper;

    private static final String FILES_BASE;

    static {
        String base = System.getProperty("user.dir");
        if (base == null || base.isEmpty()) {
            base = ".";
        }
        FILES_BASE = base + "/src/main/resources/files/";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgencyFile uploadFile(MultipartFile file, String bizId, String bizType) throws Exception {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new JeecgBootException("文件名不能为空");
        }

        AgencyFile existing = agencyFileMapper.selectByFileName(bizId, bizType, originalName);
        if (existing != null) {
            throw new JeecgBootException("文件 '" + originalName + "' 已存在，不允许重复上传");
        }

        String dirPath = FILES_BASE + bizType + "/" + bizId;
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();

        String filePath = dirPath + "/" + originalName;
        file.transferTo(new File(filePath));

        AgencyFile af = new AgencyFile();
        af.setBizId(bizId);
        af.setBizType(bizType);
        af.setFileName(originalName);
        af.setFilePath(filePath);
        agencyFileMapper.insert(af);
        return af;
    }

    @Override
    public List<AgencyFile> getFiles(String bizId, String bizType) {
        return agencyFileMapper.selectByBizId(bizId, bizType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(String fileId) {
        AgencyFile af = agencyFileMapper.selectById(fileId);
        if (af == null) return;
        File physical = new File(af.getFilePath());
        if (physical.exists()) physical.delete();
        agencyFileMapper.deleteById(fileId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByBizId(String bizId, String bizType) {
        List<AgencyFile> files = agencyFileMapper.selectByBizId(bizId, bizType);
        for (AgencyFile af : files) {
            File physical = new File(af.getFilePath());
            if (physical.exists()) physical.delete();
        }
        agencyFileMapper.deleteByBizId(bizId, bizType);
    }

    @Override
    public void downloadAllFiles(HttpServletResponse response) throws Exception {
        List<AgencyFile> allFiles = agencyFileMapper.selectAll();
        if (allFiles.isEmpty()) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("没有可下载的文件");
            return;
        }

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("机构遴选附件.zip", "UTF-8"));

        Set<String> entryNames = new HashSet<>();
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (AgencyFile af : allFiles) {
                File file = new File(af.getFilePath());
                if (!file.exists()) continue;

                String entryName = af.getBizType() + "/" + af.getFileName();
                if (entryNames.contains(entryName)) {
                    int dot = af.getFileName().lastIndexOf('.');
                    String baseName = dot > 0 ? af.getFileName().substring(0, dot) : af.getFileName();
                    String ext = dot > 0 ? af.getFileName().substring(dot) : "";
                    int counter = 1;
                    String altName;
                    do {
                        altName = af.getBizType() + "/" + baseName + "(" + counter + ")" + ext;
                        counter++;
                    } while (entryNames.contains(altName));
                    entryName = altName;
                }
                entryNames.add(entryName);

                zos.putNextEntry(new ZipEntry(entryName));
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    @Override
    public void downloadFile(String fileId, HttpServletResponse response) throws Exception {
        AgencyFile af = agencyFileMapper.selectById(fileId);
        if (af == null) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("文件记录不存在");
            return;
        }
        File file = new File(af.getFilePath());
        if (!file.exists()) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("文件不存在");
            return;
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(af.getFileName(), "UTF-8"));
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
        }
    }
}
