package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.mapper.IetmAttachmentMapper;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * IcnBatchLoader 单元测试
 *
 * 测试范围：
 * 1. 批量加载成功场景
 * 2. 空输入处理
 * 3. ICN不存在场景
 * 4. 附件缺失场景
 * 5. 文件不存在场景
 * 6. 并发加载性能
 *
 * @author jeecg-boot
 * @since 2026-09-18
 */
@ExtendWith(MockitoExtension.class)
class IcnBatchLoaderTest {

    @Mock
    private IetmIcnManageMapper icnManageMapper;

    @Mock
    private IetmAttachmentMapper attachmentMapper;

    @InjectMocks
    private IcnBatchLoader icnBatchLoader;

    private Set<String> testIcnCodes;
    private List<IetmIcnManage> mockIcns;
    private List<IetmAttachment> mockAttachments;

    @BeforeEach
    void setUp() {
        testIcnCodes = new HashSet<>(Arrays.asList(
                "ICN-TEST-001",
                "ICN-TEST-002"
        ));

        // Mock ICN实体
        mockIcns = new ArrayList<>();
        IetmIcnManage icn1 = new IetmIcnManage();
        icn1.setId("1001");
        icn1.setIcn("ICN-TEST-001");
        mockIcns.add(icn1);

        IetmIcnManage icn2 = new IetmIcnManage();
        icn2.setId("1002");
        icn2.setIcn("ICN-TEST-002");
        mockIcns.add(icn2);

        // Mock附件
        mockAttachments = new ArrayList<>();
        IetmAttachment att1 = new IetmAttachment();
        att1.setPid("1001");
        att1.setFileKey("test-file-001.png");
        att1.setFileName("test-001.png");
        mockAttachments.add(att1);

        IetmAttachment att2 = new IetmAttachment();
        att2.setPid("1002");
        att2.setFileKey("test-file-002.png");
        att2.setFileName("test-002.png");
        mockAttachments.add(att2);
    }

    @Test
    void testLoadIcnBatch_Success() {
        // Given
        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockIcns);
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockAttachments);

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(testIcnCodes);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("ICN-TEST-001"));
        assertTrue(result.containsKey("ICN-TEST-002"));

        // 验证调用了批量查询
        verify(icnManageMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(attachmentMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void testLoadIcnBatch_EmptyInput() {
        // Given
        Set<String> emptySet = new HashSet<>();

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(emptySet);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // 验证没有调用数据库
        verify(icnManageMapper, never()).selectList(any());
        verify(attachmentMapper, never()).selectList(any());
    }

    @Test
    void testLoadIcnBatch_NullInput() {
        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // 验证没有调用数据库
        verify(icnManageMapper, never()).selectList(any());
    }

    @Test
    void testLoadIcnBatch_IcnNotFound() {
        // Given
        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(testIcnCodes);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // 验证调用了ICN查询，但没有调用附件查询
        verify(icnManageMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(attachmentMapper, never()).selectList(any());
    }

    @Test
    void testLoadIcnBatch_AttachmentMissing() {
        // Given
        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockIcns);
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(testIcnCodes);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证返回了占位图（包含"附件缺失"的Base64 SVG）
        String data1 = result.get("ICN-TEST-001");
        assertNotNull(data1);
        assertTrue(data1.startsWith("data:image/svg+xml;base64,"));
    }

    @Test
    void testLoadIcnBatch_DuplicateIcnCode() {
        // Given
        IetmIcnManage duplicateIcn = new IetmIcnManage();
        duplicateIcn.setId("1003");
        duplicateIcn.setIcn("ICN-TEST-001"); // 重复的ICN编码

        List<IetmIcnManage> icnsWithDuplicate = new ArrayList<>(mockIcns);
        icnsWithDuplicate.add(duplicateIcn);

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(icnsWithDuplicate);
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockAttachments);

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(testIcnCodes);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size()); // 重复的被合并，只保留第一个
        assertTrue(result.containsKey("ICN-TEST-001"));
    }

    @Test
    void testLoadIcnBatch_PerformanceWithLargeDataset() {
        // Given - 模拟100个ICN
        Set<String> largeIcnSet = new HashSet<>();
        List<IetmIcnManage> largeIcnList = new ArrayList<>();
        List<IetmAttachment> largeAttList = new ArrayList<>();

        for (int i = 1; i <= 100; i++) {
            String icnCode = "ICN-PERF-" + String.format("%03d", i);
            largeIcnSet.add(icnCode);

            IetmIcnManage icn = new IetmIcnManage();
            icn.setId("200" + i);
            icn.setIcn(icnCode);
            largeIcnList.add(icn);

            IetmAttachment att = new IetmAttachment();
            att.setPid("200" + i);
            att.setFileKey("perf-file-" + i + ".png");
            att.setFileName("perf-" + i + ".png");
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
        assertEquals(100, result.size());

        // 验证性能：100个ICN应该在1秒内完成（Mock场景）
        assertTrue(duration < 1000, "批量加载100个ICN耗时应小于1秒，实际耗时: " + duration + "ms");

        // 验证只调用了1次批量查询（避免N+1问题）
        verify(icnManageMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(attachmentMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void testLoadIcnBatch_AttachmentFileKeyNull() {
        // Given
        IetmAttachment attWithNullKey = new IetmAttachment();
        attWithNullKey.setPid("1001");
        attWithNullKey.setFileKey(null); // 空的fileKey
        attWithNullKey.setFileName("test.png");

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockIcns);
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(attWithNullKey));

        // When
        Map<String, String> result = icnBatchLoader.loadIcnBatch(testIcnCodes);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证返回了占位图
        String data1 = result.get("ICN-TEST-001");
        assertTrue(data1.startsWith("data:image/svg+xml;base64,"));
    }
}
