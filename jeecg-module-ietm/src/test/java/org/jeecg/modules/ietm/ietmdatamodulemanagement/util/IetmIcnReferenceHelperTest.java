package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnReference;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnManageMapper;
import org.jeecg.modules.ietm.icnmanage.mapper.IetmIcnReferenceMapper;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.constants.IetmDataModuleConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * IetmIcnReferenceHelper 单元测试
 * <p>
 * 测试ICN引用同步的8个关键场景
 * </p>
 *
 * @author IETM Team
 * @since 2026-08-31
 */
@DisplayName("ICN引用同步工具类测试")
class IetmIcnReferenceHelperTest {

    private IetmIcnManageMapper icnManageMapper;
    private IetmIcnReferenceMapper icnReferenceMapper;

    @BeforeEach
    void setUp() {
        icnManageMapper = mock(IetmIcnManageMapper.class);
        icnReferenceMapper = mock(IetmIcnReferenceMapper.class);
    }

    @Test
    @DisplayName("TC-01: XML为空 - 应直接返回，不执行任何操作")
    void testSyncIcnReferences_EmptyXml() throws Exception {
        // Given
        String dmId = "1234567890123456789";
        String xmlContent = "";
        String username = "testuser";
        String remark = "测试";

        // When
        IetmIcnReferenceHelper.syncIcnReferences(
                dmId, xmlContent, username, remark,
                icnManageMapper, icnReferenceMapper
        );

        // Then
        verifyNoInteractions(icnManageMapper);
        verifyNoInteractions(icnReferenceMapper);
    }

    @Test
    @DisplayName("TC-02: XML中无ICN标签 - 应直接返回，不查询数据库")
    void testSyncIcnReferences_NoIcnTags() throws Exception {
        // Given
        String dmId = "1234567890123456789";
        String xmlContent = "<dmodule><content><description><para>纯文本内容，无ICN引用</para></description></content></dmodule>";
        String username = "testuser";
        String remark = "测试";

        // When
        IetmIcnReferenceHelper.syncIcnReferences(
                dmId, xmlContent, username, remark,
                icnManageMapper, icnReferenceMapper
        );

        // Then
        verifyNoInteractions(icnManageMapper);
        verifyNoInteractions(icnReferenceMapper);
    }

    @Test
    @DisplayName("TC-03: XML中有重复ICN代码 - 应自动去重，仅创建一条记录")
    void testSyncIcnReferences_DuplicateIcnCodes() throws Exception {
        // Given
        String dmId = "1234567890123456789";
        String xmlContent =
            "<dmodule>" +
            "  <content>" +
            "    <graphic infoEntityIdent='ICN-001'/>" +
            "    <symbol infoEntityIdent='ICN-001'/>" +  // 重复
            "    <multimedia infoEntityIdent='ICN-001'/>" +  // 重复
            "  </content>" +
            "</dmodule>";
        String username = "testuser";
        String remark = "测试";

        IetmIcnManage icn = new IetmIcnManage();
        icn.setId("ICN_ID_001");
        icn.setIcn("ICN-001");

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(icn));
        when(icnReferenceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(icnReferenceMapper.insert(any(IetmIcnReference.class)))
                .thenReturn(1);

        // When
        IetmIcnReferenceHelper.syncIcnReferences(
                dmId, xmlContent, username, remark,
                icnManageMapper, icnReferenceMapper
        );

        // Then
        verify(icnReferenceMapper, times(1)).insert(any(IetmIcnReference.class));  // 仅插入1次
    }

    @Test
    @DisplayName("TC-04: ICN不存在于ietm_icn_manage表 - 应记录警告日志，不抛异常")
    void testSyncIcnReferences_IcnNotExist() throws Exception {
        // Given
        String dmId = "1234567890123456789";
        String xmlContent = "<dmodule><content><graphic infoEntityIdent='ICN-NONEXIST'/></content></dmodule>";
        String username = "testuser";
        String remark = "测试";

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());  // ICN不存在

        // When & Then
        assertDoesNotThrow(() -> {
            IetmIcnReferenceHelper.syncIcnReferences(
                    dmId, xmlContent, username, remark,
                    icnManageMapper, icnReferenceMapper
            );
        });

        verifyNoInteractions(icnReferenceMapper);  // 不应尝试插入
    }

    @Test
    @DisplayName("TC-05: 引用关系已存在 - 应跳过插入，不产生重复记录")
    void testSyncIcnReferences_AlreadyExists() throws Exception {
        // Given
        String dmId = "1234567890123456789";
        String xmlContent = "<dmodule><content><graphic infoEntityIdent='ICN-001'/></content></dmodule>";
        String username = "testuser";
        String remark = "测试";

        IetmIcnManage icn = new IetmIcnManage();
        icn.setId("ICN_ID_001");
        icn.setIcn("ICN-001");

        IetmIcnReference existingRef = new IetmIcnReference();
        existingRef.setSourceIcnId("ICN_ID_001");
        existingRef.setDmCode(dmId);

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(icn));
        when(icnReferenceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(existingRef));  // 已存在

        // When
        IetmIcnReferenceHelper.syncIcnReferences(
                dmId, xmlContent, username, remark,
                icnManageMapper, icnReferenceMapper
        );

        // Then
        verify(icnReferenceMapper, never()).insert(any(IetmIcnReference.class));  // 不应插入
    }

    @Test
    @DisplayName("TC-06: 并发插入冲突 - 应捕获DuplicateKeyException，不抛异常")
    void testSyncIcnReferences_ConcurrentInsert() throws Exception {
        // Given
        String dmId = "1234567890123456789";
        String xmlContent = "<dmodule><content><graphic infoEntityIdent='ICN-001'/></content></dmodule>";
        String username = "testuser";
        String remark = "测试";

        IetmIcnManage icn = new IetmIcnManage();
        icn.setId("ICN_ID_001");
        icn.setIcn("ICN-001");

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(icn));
        when(icnReferenceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(icnReferenceMapper.insert(any(IetmIcnReference.class)))
                .thenThrow(new DuplicateKeyException("唯一索引冲突"));  // 模拟并发冲突

        // When & Then
        assertDoesNotThrow(() -> {
            IetmIcnReferenceHelper.syncIcnReferences(
                    dmId, xmlContent, username, remark,
                    icnManageMapper, icnReferenceMapper
            );
        });
    }

    @Test
    @DisplayName("TC-07: XML格式错误 - 应抛出JeecgBootException")
    void testSyncIcnReferences_XmlParseError() {
        // Given
        String dmId = "1234567890123456789";
        String xmlContent = "<dmodule><unclosed>";  // 格式错误的XML
        String username = "testuser";
        String remark = "测试";

        // When & Then
        Exception exception = assertThrows(JeecgBootException.class, () -> {
            IetmIcnReferenceHelper.syncIcnReferences(
                    dmId, xmlContent, username, remark,
                    icnManageMapper, icnReferenceMapper
            );
        });

        assertTrue(exception.getMessage().contains("XML解析失败"));
    }

    @Test
    @DisplayName("TC-08: 混合三种ICN标签（graphic/multimedia/symbol）- 应全部提取并创建引用")
    void testSyncIcnReferences_ThreeTypes() throws Exception {
        // Given
        String dmId = "1234567890123456789";
        String xmlContent =
            "<dmodule>" +
            "  <content>" +
            "    <graphic infoEntityIdent='ICN-001'/>" +
            "    <multimedia infoEntityIdent='ICN-002'/>" +
            "    <symbol infoEntityIdent='ICN-003'/>" +
            "  </content>" +
            "</dmodule>";
        String username = "testuser";
        String remark = "DM保存时自动创建";

        IetmIcnManage icn1 = new IetmIcnManage();
        icn1.setId("ICN_ID_001");
        icn1.setIcn("ICN-001");

        IetmIcnManage icn2 = new IetmIcnManage();
        icn2.setId("ICN_ID_002");
        icn2.setIcn("ICN-002");

        IetmIcnManage icn3 = new IetmIcnManage();
        icn3.setId("ICN_ID_003");
        icn3.setIcn("ICN-003");

        List<IetmIcnManage> icnList = new ArrayList<>();
        icnList.add(icn1);
        icnList.add(icn2);
        icnList.add(icn3);

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(icnList);
        when(icnReferenceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(icnReferenceMapper.insert(any(IetmIcnReference.class)))
                .thenReturn(1);

        // When
        IetmIcnReferenceHelper.syncIcnReferences(
                dmId, xmlContent, username, remark,
                icnManageMapper, icnReferenceMapper
        );

        // Then
        ArgumentCaptor<IetmIcnReference> captor = ArgumentCaptor.forClass(IetmIcnReference.class);
        verify(icnReferenceMapper, times(3)).insert(captor.capture());  // 应插入3条

        List<IetmIcnReference> insertedRefs = captor.getAllValues();
        assertEquals(3, insertedRefs.size());

        // 验证备注字段
        for (IetmIcnReference ref : insertedRefs) {
            assertEquals(dmId, ref.getDmCode());
            assertEquals("ICN_TO_DM", ref.getReferenceType());
            assertEquals(remark, ref.getRemark());
            assertEquals(username, ref.getCreateBy());
            assertNotNull(ref.getCreateTime());
        }
    }

    @Test
    @DisplayName("TC-09: 备注字段区分调用来源 - 应正确设置remark")
    void testSyncIcnReferences_RemarkDifferentiation() throws Exception {
        // Given
        String dmId = "1234567890123456789";
        String xmlContent = "<dmodule><content><graphic infoEntityIdent='ICN-001'/></content></dmodule>";
        String username = "testuser";

        IetmIcnManage icn = new IetmIcnManage();
        icn.setId("ICN_ID_001");
        icn.setIcn("ICN-001");

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(icn));
        when(icnReferenceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(icnReferenceMapper.insert(any(IetmIcnReference.class)))
                .thenReturn(1);

        // When - 场景1：DM保存
        IetmIcnReferenceHelper.syncIcnReferences(
                dmId, xmlContent, username, "DM保存时自动创建",
                icnManageMapper, icnReferenceMapper
        );

        // Then
        ArgumentCaptor<IetmIcnReference> captor1 = ArgumentCaptor.forClass(IetmIcnReference.class);
        verify(icnReferenceMapper).insert(captor1.capture());
        assertEquals("DM保存时自动创建", captor1.getValue().getRemark());

        // When - 场景2：计算引用
        reset(icnReferenceMapper);
        when(icnReferenceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(icnReferenceMapper.insert(any(IetmIcnReference.class)))
                .thenReturn(1);

        IetmIcnReferenceHelper.syncIcnReferences(
                dmId, xmlContent, username, "计算引用时自动创建",
                icnManageMapper, icnReferenceMapper
        );

        // Then
        ArgumentCaptor<IetmIcnReference> captor2 = ArgumentCaptor.forClass(IetmIcnReference.class);
        verify(icnReferenceMapper).insert(captor2.capture());
        assertEquals("计算引用时自动创建", captor2.getValue().getRemark());
    }

    @Test
    @DisplayName("TC-10: 部分ICN不存在 - 应仅为存在的ICN创建引用，记录警告日志")
    void testSyncIcnReferences_PartialIcnMissing() throws Exception {
        // Given
        String dmId = "1234567890123456789";
        String xmlContent =
            "<dmodule>" +
            "  <content>" +
            "    <graphic infoEntityIdent='ICN-001'/>" +
            "    <graphic infoEntityIdent='ICN-MISSING'/>" +  // 不存在
            "    <graphic infoEntityIdent='ICN-002'/>" +
            "  </content>" +
            "</dmodule>";
        String username = "testuser";
        String remark = "测试";

        IetmIcnManage icn1 = new IetmIcnManage();
        icn1.setId("ICN_ID_001");
        icn1.setIcn("ICN-001");

        IetmIcnManage icn2 = new IetmIcnManage();
        icn2.setId("ICN_ID_002");
        icn2.setIcn("ICN-002");

        List<IetmIcnManage> icnList = new ArrayList<>();
        icnList.add(icn1);
        icnList.add(icn2);

        when(icnManageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(icnList);  // 只返回2个，ICN-MISSING不存在
        when(icnReferenceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(icnReferenceMapper.insert(any(IetmIcnReference.class)))
                .thenReturn(1);

        // When
        IetmIcnReferenceHelper.syncIcnReferences(
                dmId, xmlContent, username, remark,
                icnManageMapper, icnReferenceMapper
        );

        // Then
        verify(icnReferenceMapper, times(2)).insert(any(IetmIcnReference.class));  // 仅插入存在的2个
    }
}
