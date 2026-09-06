package org.jeecg.modules.ietm.ietmddn;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ietm.ietmddn.util.DdnPackageBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DDN ZIP打包测试 - 验证空目录是否被正确打包
 *
 * 测试场景：
 * 1. 空ICN目录应该被打包进ZIP
 * 2. 有文件的ICN目录应该被正确打包
 * 3. 多层目录结构应该被完整保留
 * 4. 目录条目名称应以/结尾
 */
@Slf4j
public class DdnZipPackageTest {

    private Path tempDir;
    private File workDir;
    private File zipFile;

    @BeforeEach
    public void setup() throws IOException {
        // 创建临时测试目录
        tempDir = Files.createTempDirectory("ddn-zip-test");
        workDir = tempDir.toFile();
        zipFile = new File(tempDir.toFile(), "test.zip");

        log.info("测试环境准备完成：workDir={}", workDir.getAbsolutePath());
    }

    @AfterEach
    public void cleanup() throws IOException {
        // 清理测试文件
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            log.info("测试环境清理完成");
        }
    }

    @Test
    @DisplayName("测试1：空ICN目录应该被打包进ZIP")
    public void testEmptyIcnDirectory_ShouldBeIncludedInZip() throws Exception {
        // 1. 准备测试数据
        // 创建DM目录和文件
        File dmDir = new File(workDir, "DM");
        dmDir.mkdirs();
        Files.write(new File(dmDir, "DMC-TEST-001.xml").toPath(),
                "<dmodule><content>Test DM</content></dmodule>".getBytes());

        // 创建空ICN目录（模拟没有ICN引用的场景）
        File icnDir = new File(workDir, "ICN");
        icnDir.mkdirs();

        // 创建DDN.xml
        Files.write(new File(workDir, "DDN-TEST-001.xml").toPath(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ddn></ddn>".getBytes());

        log.info("测试数据准备完成：DM目录有1个文件，ICN目录为空");

        // 2. 执行ZIP打包（使用反射调用私有方法）
        DdnPackageBuilder builder = new DdnPackageBuilder();
        Method method = DdnPackageBuilder.class.getDeclaredMethod("createZipPackage", File.class, File.class);
        method.setAccessible(true);
        method.invoke(builder, workDir, zipFile);

        log.info("ZIP打包完成：{}", zipFile.getAbsolutePath());

        // 3. 验证ZIP包内容
        assertTrue(zipFile.exists(), "ZIP文件应该存在");
        assertTrue(zipFile.length() > 0, "ZIP文件不应为空");

        try (ZipFile zip = new ZipFile(zipFile)) {
            log.info("开始验证ZIP包内容，总条目数：{}", zip.size());

            // 验证1：应包含ICN/目录条目
            ZipEntry icnEntry = zip.getEntry("ICN/");
            assertNotNull(icnEntry, "❌ ZIP包应包含ICN/目录条目");
            assertTrue(icnEntry.isDirectory(), "ICN/应为目录条目");
            assertEquals("ICN/", icnEntry.getName(), "目录条目名称应以/结尾");
            log.info("✅ 验证通过：ICN/目录条目存在");

            // 验证2：应包含DM/目录条目
            ZipEntry dmDirEntry = zip.getEntry("DM/");
            assertNotNull(dmDirEntry, "ZIP包应包含DM/目录条目");
            assertTrue(dmDirEntry.isDirectory(), "DM/应为目录条目");
            log.info("✅ 验证通过：DM/目录条目存在");

            // 验证3：应包含DM文件
            ZipEntry dmFileEntry = zip.getEntry("DM/DMC-TEST-001.xml");
            assertNotNull(dmFileEntry, "ZIP包应包含DM/DMC-TEST-001.xml文件");
            assertFalse(dmFileEntry.isDirectory(), "DM文件应为文件条目，不是目录");
            log.info("✅ 验证通过：DM文件存在");

            // 验证4：应包含DDN.xml文件
            ZipEntry ddnEntry = zip.getEntry("DDN-TEST-001.xml");
            assertNotNull(ddnEntry, "ZIP包应包含DDN-TEST-001.xml文件");
            assertFalse(ddnEntry.isDirectory(), "DDN文件应为文件条目");
            log.info("✅ 验证通过：DDN.xml文件存在");

            // 验证5：ICN目录下不应有文件（因为是空目录）
            boolean hasIcnFiles = false;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith("ICN/") && !entry.isDirectory()) {
                    hasIcnFiles = true;
                    break;
                }
            }
            assertFalse(hasIcnFiles, "空ICN目录下不应有文件");
            log.info("✅ 验证通过：ICN目录为空（符合预期）");

            log.info("========================================");
            log.info("测试1完成：所有验证通过 ✅");
            log.info("========================================");
        }
    }

    @Test
    @DisplayName("测试2：有文件的ICN目录应该被正确打包")
    public void testIcnDirectoryWithFiles_ShouldBeIncludedInZip() throws Exception {
        // 1. 准备测试数据
        File dmDir = new File(workDir, "DM");
        dmDir.mkdirs();
        Files.write(new File(dmDir, "DMC-TEST-002.xml").toPath(),
                "<dmodule><content>Test DM with ICN</content></dmodule>".getBytes());

        // 创建ICN目录并添加文件（模拟有ICN引用的场景）
        File icnDir = new File(workDir, "ICN");
        icnDir.mkdirs();
        Files.write(new File(icnDir, "ICN-TEST-A-A11000-C-30101-00001-A-001-01.png").toPath(),
                "fake-image-data".getBytes());
        Files.write(new File(icnDir, "ICN-TEST-A-A12000-C-30101-00002-A-001-01.jpg").toPath(),
                "fake-image-data-2".getBytes());

        Files.write(new File(workDir, "DDN-TEST-002.xml").toPath(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ddn></ddn>".getBytes());

        log.info("测试数据准备完成：ICN目录有2个文件");

        // 2. 执行ZIP打包
        DdnPackageBuilder builder = new DdnPackageBuilder();
        Method method = DdnPackageBuilder.class.getDeclaredMethod("createZipPackage", File.class, File.class);
        method.setAccessible(true);
        method.invoke(builder, workDir, zipFile);

        log.info("ZIP打包完成");

        // 3. 验证ZIP包内容
        try (ZipFile zip = new ZipFile(zipFile)) {
            log.info("开始验证ZIP包内容，总条目数：{}", zip.size());

            // 验证1：ICN目录条目应存在
            ZipEntry icnDirEntry = zip.getEntry("ICN/");
            assertNotNull(icnDirEntry, "ZIP包应包含ICN/目录条目");
            assertTrue(icnDirEntry.isDirectory(), "ICN/应为目录条目");
            log.info("✅ 验证通过：ICN/目录条目存在");

            // 验证2：ICN文件1应存在
            ZipEntry icnFile1 = zip.getEntry("ICN/ICN-TEST-A-A11000-C-30101-00001-A-001-01.png");
            assertNotNull(icnFile1, "ZIP包应包含ICN文件1");
            assertFalse(icnFile1.isDirectory(), "ICN文件应为文件条目");
            log.info("✅ 验证通过：ICN文件1存在");

            // 验证3：ICN文件2应存在
            ZipEntry icnFile2 = zip.getEntry("ICN/ICN-TEST-A-A12000-C-30101-00002-A-001-01.jpg");
            assertNotNull(icnFile2, "ZIP包应包含ICN文件2");
            assertFalse(icnFile2.isDirectory(), "ICN文件应为文件条目");
            log.info("✅ 验证通过：ICN文件2存在");

            // 验证4：统计ICN目录下的文件数
            int icnFileCount = 0;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith("ICN/") && !entry.isDirectory()) {
                    icnFileCount++;
                }
            }
            assertEquals(2, icnFileCount, "ICN目录应包含2个文件");
            log.info("✅ 验证通过：ICN目录包含2个文件");

            log.info("========================================");
            log.info("测试2完成：所有验证通过 ✅");
            log.info("========================================");
        }
    }

    @Test
    @DisplayName("测试3：多层目录结构应该被完整保留")
    public void testNestedDirectories_ShouldBePreserved() throws Exception {
        // 1. 准备测试数据 - 创建多层嵌套目录
        File dmDir = new File(workDir, "DM");
        dmDir.mkdirs();
        Files.write(new File(dmDir, "test.xml").toPath(), "<dm/>".getBytes());

        // 创建嵌套的空目录（模拟复杂场景）
        File icnDir = new File(workDir, "ICN");
        icnDir.mkdirs();

        File comDir = new File(workDir, "COM");
        comDir.mkdirs();

        File subDir = new File(comDir, "subdirectory");
        subDir.mkdirs();

        log.info("测试数据准备完成：创建了4个目录（DM、ICN、COM、COM/subdirectory）");

        // 2. 执行ZIP打包
        DdnPackageBuilder builder = new DdnPackageBuilder();
        Method method = DdnPackageBuilder.class.getDeclaredMethod("createZipPackage", File.class, File.class);
        method.setAccessible(true);
        method.invoke(builder, workDir, zipFile);

        // 3. 验证所有目录都被打包
        try (ZipFile zip = new ZipFile(zipFile)) {
            log.info("开始验证多层目录结构，总条目数：{}", zip.size());

            assertNotNull(zip.getEntry("DM/"), "应包含DM/目录");
            assertNotNull(zip.getEntry("ICN/"), "应包含ICN/目录");
            assertNotNull(zip.getEntry("COM/"), "应包含COM/目录");
            assertNotNull(zip.getEntry("COM/subdirectory/"), "应包含COM/subdirectory/嵌套目录");

            log.info("✅ 验证通过：所有目录（包括嵌套目录）都被正确打包");

            log.info("========================================");
            log.info("测试3完成：所有验证通过 ✅");
            log.info("========================================");
        }
    }

    @Test
    @DisplayName("测试4：验证目录条目名称格式")
    public void testDirectoryEntryNames_ShouldEndWithSlash() throws Exception {
        // 1. 准备测试数据
        File dmDir = new File(workDir, "DM");
        dmDir.mkdirs();
        File icnDir = new File(workDir, "ICN");
        icnDir.mkdirs();

        Files.write(new File(dmDir, "test.xml").toPath(), "<dm/>".getBytes());

        log.info("测试数据准备完成");

        // 2. 执行ZIP打包
        DdnPackageBuilder builder = new DdnPackageBuilder();
        Method method = DdnPackageBuilder.class.getDeclaredMethod("createZipPackage", File.class, File.class);
        method.setAccessible(true);
        method.invoke(builder, workDir, zipFile);

        // 3. 验证目录条目名称格式
        try (ZipFile zip = new ZipFile(zipFile)) {
            log.info("开始验证目录条目名称格式");

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    // 所有目录条目名称必须以/结尾
                    assertTrue(entry.getName().endsWith("/"),
                            "目录条目名称应以/结尾：" + entry.getName());
                    log.info("✅ 目录条目格式正确：{}", entry.getName());
                }
            }

            log.info("========================================");
            log.info("测试4完成：所有目录条目名称格式正确 ✅");
            log.info("========================================");
        }
    }

    @Test
    @DisplayName("测试5：对比修复前后的行为（模拟）")
    public void testFixComparison_EmptyVsNonEmptyDirectory() throws Exception {
        // 这个测试用于说明修复的效果

        log.info("========================================");
        log.info("修复效果对比测试");
        log.info("========================================");

        // 场景1：空ICN目录
        File dmDir1 = new File(workDir, "DM");
        dmDir1.mkdirs();
        Files.write(new File(dmDir1, "test.xml").toPath(), "<dm/>".getBytes());

        File icnDir1 = new File(workDir, "ICN");
        icnDir1.mkdirs();

        DdnPackageBuilder builder = new DdnPackageBuilder();
        Method method = DdnPackageBuilder.class.getDeclaredMethod("createZipPackage", File.class, File.class);
        method.setAccessible(true);
        method.invoke(builder, workDir, zipFile);

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry icnEntry = zip.getEntry("ICN/");

            log.info("========================================");
            log.info("修复前：空ICN目录 → 不被打包（用户看不到）❌");
            log.info("修复后：空ICN目录 → 被打包进ZIP（用户能看到）✅");
            log.info("========================================");
            log.info("实际验证结果：ICN/目录条目 {} 存在",
                    icnEntry != null ? "✅" : "❌");
            log.info("========================================");

            assertNotNull(icnEntry, "修复后：空ICN目录应该被打包");
        }

        log.info("测试5完成：修复效果验证通过 ✅");
    }
}
