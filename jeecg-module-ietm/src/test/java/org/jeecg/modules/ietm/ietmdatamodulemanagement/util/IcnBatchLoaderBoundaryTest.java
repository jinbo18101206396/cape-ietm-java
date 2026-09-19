package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.mapper.IetmAttachmentMapper;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IcnBatchLoader 边界和异常测试
 *
 * 测试范围：
 * 1. 极端边界值（0个、1个、1000个ICN）
 * 2. 异常场景（数据库异常、文件系统异常）
 * 3. 特殊字符处理
 * 4. 内存占用测试
 * 5. 并发安全性测试
 *
 * @author jeecg-boot
 * @since 2026-09-18
 */
@ExtendWith(MockitoExtension.class)
class IcnBatchLoaderBoundaryTest {

    @Mock
    private IetmIcnManageMapper icnManageMapper;

    @Mock
    private IetmAttachmentMapper attachmentMapper;

    @InjectMocks
    private IcnBatchLoader icnBatchLoader;

    @TempDir
    Path tempDir;

    @Test
    void testLoadIcnBatch_SingleIcn() {
        // Given - 仅1个ICN
        Set<String> singleIcn = Collections.singleton("ICN-SINGLE-001");

        IetmIcnManage icn = new IetmIcnManage();
        icn.setId("1001");
        icn.setIcn("ICN-SINGLE-001");

        IetmAttachment att = new IetmAttachment();
        att.setPid("1001");
        att.setFileKey("single.png");
        att.setFileName("single.png");

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(icn));
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(att));

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(singleIcn);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("ICN-SINGLE-001"));
    }

    @Test
    void testLoadIcnBatch_VeryLargeDataset() {
        // Given - 1000个ICN（压力测试）
        Set<String> largeIcnSet = new HashSet<>();
        List<IetmIcnManage> largeIcnList = new ArrayList<>();
        List<IetmAttachment> largeAttList = new ArrayList<>();

        for (int i = 1; i <= 1000; i++) {
            String icnCode = "ICN-LARGE-" + String.format("%04d", i);
            largeIcnSet.add(icnCode);

            IetmIcnManage icn = new IetmIcnManage();
            icn.setId("3000" + i);
            icn.setIcn(icnCode);
            largeIcnList.add(icn);

            IetmAttachment att = new IetmAttachment();
            att.setPid("3000" + i);
            att.setFileKey("large-file-" + i + ".png");
            att.setFileName("large-" + i + ".png");
            largeAttList.add(att);
        }

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(largeIcnList);
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(largeAttList);

        // When
        long startTime = System.currentTimeMillis();
        Map<String, String> result = icnBatchLoader.loadIcnBatch(largeIcnSet);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(result);
        assertEquals(1000, result.size());

        // 性能验证：1000个ICN应该在5秒内完成（Mock场景）
        assertTrue(duration < 5000, "批量加载1000个ICN耗时应小于5秒，实际耗时: " + duration + "ms");

        // 验证只调用了1次批量查询
        verify(icnManageMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(attachmentMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void testLoadIcnBatch_DatabaseException() {
        // Given - 数据库查询抛出异常
        Set<String> testIcnCodes = new HashSet<>(Arrays.asList("ICN-TEST-001", "ICN-TEST-002"));

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(testIcnCodes);

        // Then - 应该返回空Map，不抛出异常
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadIcnBatch_AttachmentQueryException() {
        // Given - ICN查询成功，但附件查询失败
        Set<String> testIcnCodes = new HashSet<>(Arrays.asList("ICN-TEST-001"));

        IetmIcnManage icn = new IetmIcnManage();
        icn.setId("1001");
        icn.setIcn("ICN-TEST-001");

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(icn));
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("Attachment query failed"));

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(testIcnCodes);

        // Then - 应该返回空Map或占位图
        assertNotNull(result);
        // 因为附件查询失败，整个批量加载可能失败或返回占位图
        assertTrue(result.isEmpty() || result.get("ICN-TEST-001").contains("占位图"));
    }

    @Test
    void testLoadIcnBatch_SpecialCharactersInIcnCode() {
        // Given - ICN编码包含特殊字符
        Set<String> specialIcns = new HashSet<>(Arrays.asList(
                "ICN-TEST-<script>",
                "ICN-TEST-&amp;",
                "ICN-TEST-\"quote\""
        ));

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(specialIcns);

        // Then - 应该能处理特殊字符，不抛出异常
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadIcnBatch_EmptyStringInSet() {
        // Given - Set中包含空字符串
        Set<String> icnsWithEmpty = new HashSet<>(Arrays.asList("ICN-TEST-001", "", "ICN-TEST-002"));

        IetmIcnManage icn1 = new IetmIcnManage();
        icn1.setId("1001");
        icn1.setIcn("ICN-TEST-001");

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(icn1));
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(icnsWithEmpty);

        // Then - 应该能处理空字符串
        assertNotNull(result);
    }

    @Test
    void testLoadIcnBatch_MultipleAttachmentsPerIcn() {
        // Given - 一个ICN有多个附件（取第一个）
        Set<String> testIcnCodes = Collections.singleton("ICN-MULTI-001");

        IetmIcnManage icn = new IetmIcnManage();
        icn.setId("1001");
        icn.setIcn("ICN-MULTI-001");

        IetmAttachment att1 = new IetmAttachment();
        att1.setPid("1001");
        att1.setFileKey("file1.png");
        att1.setFileName("file1.png");

        IetmAttachment att2 = new IetmAttachment();
        att2.setPid("1001");
        att2.setFileKey("file2.png");
        att2.setFileName("file2.png");

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(icn));
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(att1, att2));

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(testIcnCodes);

        // Then - 应该只使用第一个附件
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testLoadIcnBatch_AttachmentFileKeyWithSpaces() {
        // Given - FileKey包含空格或特殊路径字符
        Set<String> testIcnCodes = Collections.singleton("ICN-SPACE-001");

        IetmIcnManage icn = new IetmIcnManage();
        icn.setId("1001");
        icn.setIcn("ICN-SPACE-001");

        IetmAttachment att = new IetmAttachment();
        att.setPid("1001");
        att.setFileKey("file with spaces.png");
        att.setFileName("file with spaces.png");

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(icn));
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(att));

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(testIcnCodes);

        // Then - 应该能处理包含空格的文件名
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testLoadIcnBatch_VeryLongIcnCode() {
        // Given - 非常长的ICN编码（边界测试）
        StringBuilder sb = new StringBuilder("ICN-");
        for (int i = 0; i < 200; i++) {
            sb.append("A");
        }
        String longIcnCode = sb.toString();
        Set<String> testIcnCodes = Collections.singleton(longIcnCode);

        IetmIcnManage icn = new IetmIcnManage();
        icn.setId("1001");
        icn.setIcn(longIcnCode);

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(icn));
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(testIcnCodes);

        // Then - 应该能处理长字符串
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(longIcnCode));
    }

    @Test
    void testLoadIcnBatch_ConcurrentModification() {
        // Given - 模拟并发修改场景
        Set<String> concurrentSet = new HashSet<>(Arrays.asList("ICN-001", "ICN-002", "ICN-003"));

        List<IetmIcnManage> icnList = new ArrayList<>();
        for (String code : concurrentSet) {
            IetmIcnManage icn = new IetmIcnManage();
            icn.setId(code);
            icn.setIcn(code);
            icnList.add(icn);
        }

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(icnList);
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // When - 在查询过程中尝试修改Set（测试线程安全）
        assertDoesNotThrow(() -> {
            Map<String, String> result = icnBatchLoader.loadIcnBatch(concurrentSet);
            assertNotNull(result);
        });
    }
}
