package org.jeecg.modules.ietm.ietmddn.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmddn.entity.IetmDdn;
import org.jeecg.modules.ietm.ietmddn.mapper.IetmDdnMapper;
import org.jeecg.modules.ietm.ietmddn.service.IIetmDdnExchangeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @Description: 数据交换记录服务实现
 * @Author: jeecg-boot
 * @Date: 2026-09-15
 * @Version: V1.0
 */
@Service
@Slf4j
public class IetmDdnExchangeServiceImpl extends ServiceImpl<IetmDdnMapper, IetmDdn> implements IIetmDdnExchangeService {

    @Value("${jeecg.path.upload}")
    private String uploadPath;

    @Override
    public Map<String, Object> getDdnFileContent(String id) throws Exception {
        IetmDdn ietmDdn = this.getById(id);
        if (ietmDdn == null) {
            throw new Exception("DDN记录不存在");
        }

        String ddnFilePath = ietmDdn.getDdnFilePath();
        if (ddnFilePath == null || ddnFilePath.trim().isEmpty()) {
            throw new Exception("DDN文件路径为空");
        }

        // 构建完整文件路径
        String fullPath = uploadPath + File.separator + ddnFilePath;
        File file = new File(fullPath);

        if (!file.exists()) {
            throw new Exception("DDN文件不存在：" + ddnFilePath);
        }

        if (!file.isFile()) {
            throw new Exception("DDN路径不是文件：" + ddnFilePath);
        }

        // 从ZIP包中读取XML内容
        String xmlContent = readXmlFromZip(file, ietmDdn.getDdnCode());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("ddnCode", ietmDdn.getDdnCode());
        result.put("fileName", ietmDdn.getDdnCode() + ".xml");
        result.put("fileSize", xmlContent.getBytes(StandardCharsets.UTF_8).length);
        result.put("content", xmlContent);
        result.put("impexp", ietmDdn.getImpexp());
        result.put("ddnType", ietmDdn.getDdnType());
        result.put("dmCount", ietmDdn.getDmCount());
        result.put("icnCount", ietmDdn.getIcnCount());

        log.info("成功读取DDN文件内容，ID: {}, 文件: {}", id, ddnFilePath);
        return result;
    }

    /**
     * 从ZIP包中读取XML文件内容
     */
    private String readXmlFromZip(File zipFile, String ddnCode) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // 查找XML文件（不区分大小写）
                if (entryName.toLowerCase().endsWith(".xml")) {
                    log.info("找到XML文件：{}", entryName);

                    // 读取XML内容
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }

                    String content = baos.toString(StandardCharsets.UTF_8.name());
                    zis.closeEntry();
                    return content;
                }
                zis.closeEntry();
            }
        }

        throw new Exception("ZIP包中未找到XML文件，DDN编码：" + ddnCode);
    }
}
