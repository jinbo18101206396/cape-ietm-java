package org.jeecg.modules.ietm.icnmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.modules.ietm.common.IetmConstants;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.service.IIetmAttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * IetmIcnManageServiceImpl.loadAttachment() 修复测试
 *
 * 问题：loadAttachment() 使用不存在的 business_id/business_type 字段查询附件
 * 修复：改用 pid + file_type 字段查询
 *
 * @author Kiro
 * @date 2026-09-17
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("IetmIcnManageServiceImpl.loadAttachment() 修复测试")
public class IetmIcnManageServiceImplLoadAttachmentTest {

    @Autowired
    private IIetmIcnManageService icnManageService;

    @MockBean
    private IIetmAttachmentService attachmentService;

    private static final String TEST_ICN_ID = "1234567890123456789";

    @BeforeEach
    void setUp() {
        reset(attachmentService);
    }

    @Test
    @DisplayName("TC-01: 验证使用 pid 字段查询附件")
    void testLoadAttachmentUsePidField() {
        // Given: 准备ICN对象
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(TEST_ICN_ID);
        icn.setIcn("ICN-TEST-001");

        IetmAttachment attachment = new IetmAttachment();
        attachment.setId("9876543210987654321");
        attachment.setPid(TEST_ICN_ID);
        attachment.setFileKey("project/test/icn/test.jpg");
        attachment.setFileName("test.jpg");
        attachment.setFileType(IetmConstants.FILE_TYPE_ENTITY);

        List<IetmAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(attachment);

        when(attachmentService.list(any(QueryWrapper.class))).thenReturn(attachmentList);

        // When: 调用 loadAttachment
        icnManageService.loadAttachment(icn);

        // Then: 验证使用了正确的查询条件
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(attachmentService).list(captor.capture());

        QueryWrapper<IetmAttachment> actualQw = captor.getValue();
        String sqlSegment = actualQw.getSqlSegment();

        // 验证查询条件
        assertThat(sqlSegment).as("查询条件应包含 pid")
                .contains("pid");
        assertThat(sqlSegment).as("查询条件应包含 file_type")
                .contains("file_type");
        assertThat(sqlSegment).as("查询条件不应包含废弃的 business_id")
                .doesNotContain("business_id");
        assertThat(sqlSegment).as("查询条件不应包含废弃的 business_type")
                .doesNotContain("business_type");

        // 验证附件已正确加载
        assertThat(icn.getIetmAttachment()).as("附件应已加载到ICN对象").isNotNull();
        assertThat(icn.getIetmAttachment().getId()).isEqualTo(attachment.getId());
    }

    @Test
    @DisplayName("TC-02: 验证查询实体类型附件")
    void testLoadAttachmentEntityFileType() {
        // Given: 准备ICN和实体类型附件
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(TEST_ICN_ID);

        IetmAttachment entityAttachment = new IetmAttachment();
        entityAttachment.setId("1111111111111111111");
        entityAttachment.setPid(TEST_ICN_ID);
        entityAttachment.setFileKey("entity.jpg");
        entityAttachment.setFileType(IetmConstants.FILE_TYPE_ENTITY);

        List<IetmAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(entityAttachment);

        when(attachmentService.list(any(QueryWrapper.class))).thenReturn(attachmentList);

        // When: 调用方法
        icnManageService.loadAttachment(icn);

        // Then: 验证查询了实体类型附件
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(attachmentService).list(captor.capture());

        QueryWrapper<IetmAttachment> actualQw = captor.getValue();
        String sqlSegment = actualQw.getSqlSegment();

        assertThat(sqlSegment).as("应查询实体类型附件")
                .contains("file_type");
        assertThat(icn.getIetmAttachment()).isNotNull();
        assertThat(icn.getIetmAttachment().getFileType()).isEqualTo(IetmConstants.FILE_TYPE_ENTITY);
    }

    @Test
    @DisplayName("TC-03: 验证ICN为null时不查询")
    void testLoadAttachmentNullIcn() {
        // When: 传入null
        icnManageService.loadAttachment(null);

        // Then: 不应调用附件服务
        verify(attachmentService, never()).list(any());
    }

    @Test
    @DisplayName("TC-04: 验证ICN ID为null时不查询")
    void testLoadAttachmentNullIcnId() {
        // Given: ICN对象但ID为null
        IetmIcnManage icn = new IetmIcnManage();
        icn.setIcn("ICN-TEST-001");
        // id 未设置

        // When: 调用方法
        icnManageService.loadAttachment(icn);

        // Then: 不应调用附件服务
        verify(attachmentService, never()).list(any());
        assertThat(icn.getIetmAttachment()).as("ICN ID为null时不应加载附件").isNull();
    }

    @Test
    @DisplayName("TC-05: 验证无附件时的处理")
    void testLoadAttachmentNoAttachment() {
        // Given: ICN存在但无附件
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(TEST_ICN_ID);

        when(attachmentService.list(any(QueryWrapper.class))).thenReturn(new ArrayList<>());

        // When: 调用方法
        icnManageService.loadAttachment(icn);

        // Then: 附件应为null
        assertThat(icn.getIetmAttachment()).as("无附件时应保持null").isNull();
        verify(attachmentService).list(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("TC-06: 验证多个附件时取第一个")
    void testLoadAttachmentMultipleAttachments() {
        // Given: 一个ICN有多个实体附件（按create_time升序）
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(TEST_ICN_ID);

        IetmAttachment firstAttachment = new IetmAttachment();
        firstAttachment.setId("1111111111111111111");
        firstAttachment.setPid(TEST_ICN_ID);
        firstAttachment.setFileKey("first.jpg");
        firstAttachment.setFileName("first.jpg");

        IetmAttachment secondAttachment = new IetmAttachment();
        secondAttachment.setId("2222222222222222222");
        secondAttachment.setPid(TEST_ICN_ID);
        secondAttachment.setFileKey("second.jpg");
        secondAttachment.setFileName("second.jpg");

        List<IetmAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(firstAttachment);
        attachmentList.add(secondAttachment);

        when(attachmentService.list(any(QueryWrapper.class))).thenReturn(attachmentList);

        // When: 调用方法
        icnManageService.loadAttachment(icn);

        // Then: 应使用第一个附件
        assertThat(icn.getIetmAttachment()).as("应加载第一个附件").isNotNull();
        assertThat(icn.getIetmAttachment().getId()).isEqualTo(firstAttachment.getId());
        assertThat(icn.getIetmAttachment().getFileName()).isEqualTo("first.jpg");
    }

    @Test
    @DisplayName("TC-07: 验证按创建时间升序排序")
    void testLoadAttachmentOrderByCreateTime() {
        // Given: 准备ICN对象
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(TEST_ICN_ID);

        IetmAttachment attachment = new IetmAttachment();
        attachment.setId("9999999999999999999");
        attachment.setPid(TEST_ICN_ID);

        List<IetmAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(attachment);

        when(attachmentService.list(any(QueryWrapper.class))).thenReturn(attachmentList);

        // When: 调用方法
        icnManageService.loadAttachment(icn);

        // Then: 验证排序条件
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(attachmentService).list(captor.capture());

        QueryWrapper<IetmAttachment> actualQw = captor.getValue();
        String sqlSegment = actualQw.getSqlSegment();

        assertThat(sqlSegment).as("应按create_time升序排序")
                .contains("create_time")
                .contains("ASC");
    }

    @Test
    @DisplayName("TC-08: 验证过滤相关文件类型附件")
    void testLoadAttachmentFilterRelatedFileType() {
        // Given: ICN有实体和相关文件两种类型附件
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(TEST_ICN_ID);

        IetmAttachment entityAttachment = new IetmAttachment();
        entityAttachment.setId("1111111111111111111");
        entityAttachment.setPid(TEST_ICN_ID);
        entityAttachment.setFileType(IetmConstants.FILE_TYPE_ENTITY);

        // 只返回实体类型（file_type过滤在查询中完成）
        List<IetmAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(entityAttachment);

        when(attachmentService.list(any(QueryWrapper.class))).thenReturn(attachmentList);

        // When: 调用方法
        icnManageService.loadAttachment(icn);

        // Then: 验证查询条件包含file_type过滤
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(attachmentService).list(captor.capture());

        QueryWrapper<IetmAttachment> actualQw = captor.getValue();
        String sqlSegment = actualQw.getSqlSegment();

        assertThat(sqlSegment).as("应过滤file_type")
                .contains("file_type");
        assertThat(icn.getIetmAttachment()).isNotNull();
        assertThat(icn.getIetmAttachment().getFileType())
                .as("应只加载实体类型附件")
                .isEqualTo(IetmConstants.FILE_TYPE_ENTITY);
    }
}
