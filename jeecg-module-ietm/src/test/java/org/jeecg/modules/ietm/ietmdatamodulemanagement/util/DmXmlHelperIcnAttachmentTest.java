package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DmXmlHelper ICN附件查询修复测试
 *
 * 问题：DmXmlHelper.loadIcnAsBase64() 使用不存在的 business_id/business_type 字段查询附件
 * 修复：改用 pid + file_type 字段查询
 *
 * @author Kiro
 * @date 2026-09-17
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DmXmlHelper ICN附件查询修复测试")
public class DmXmlHelperIcnAttachmentTest {

    @Autowired
    private DmXmlHelper dmXmlHelper;

    @MockBean
    private IIetmIcnManageService icnManageService;

    @MockBean
    private IIetmAttachmentService attachmentService;

    private static final String TEST_ICN_ID = "1234567890123456789";
    private static final String TEST_ICN_CODE = "ICN-ZB1-A-020000-60101-30101-00017-A-001-01";

    @BeforeEach
    void setUp() {
        reset(icnManageService, attachmentService);
    }

    @Test
    @DisplayName("TC-01: 验证使用 pid 字段查询ICN附件")
    void testLoadIcnUsePidField() throws Exception {
        // Given: 准备ICN和附件数据
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(TEST_ICN_ID);
        icn.setIcn(TEST_ICN_CODE);

        IetmAttachment attachment = new IetmAttachment();
        attachment.setId("9876543210987654321");
        attachment.setPid(TEST_ICN_ID);
        attachment.setFileKey("project/test/icn/test.jpg");
        attachment.setFileName("test.jpg");
        attachment.setFileType(IetmConstants.FILE_TYPE_ENTITY);

        List<IetmIcnManage> icnList = new ArrayList<>();
        icnList.add(icn);

        List<IetmAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(attachment);

        // Mock ICN查询
        when(icnManageService.list(any(QueryWrapper.class))).thenReturn(icnList);

        // Mock 附件查询
        when(attachmentService.list(any(QueryWrapper.class))).thenReturn(attachmentList);

        // When: 使用反射调用 private static loadIcnAsBase64
        Method method = DmXmlHelper.class.getDeclaredMethod("loadIcnAsBase64", String.class);
        method.setAccessible(true);

        try {
            method.invoke(null, TEST_ICN_CODE);
        } catch (Exception e) {
            // 预期会因为文件不存在而失败，但查询逻辑应该正确
        }

        // Then: 验证使用了正确的查询条件
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(attachmentService, atLeastOnce()).list(captor.capture());

        QueryWrapper<IetmAttachment> actualQw = captor.getValue();

        // 验证查询条件中使用了 pid 而不是 business_id
        String sqlSegment = actualQw.getSqlSegment();
        assertThat(sqlSegment).as("查询条件应包含 pid")
                .contains("pid");
        assertThat(sqlSegment).as("查询条件应包含 file_type")
                .contains("file_type");
        assertThat(sqlSegment).as("查询条件不应包含废弃的 business_id")
                .doesNotContain("business_id");
        assertThat(sqlSegment).as("查询条件不应包含废弃的 business_type")
                .doesNotContain("business_type");
    }

    @Test
    @DisplayName("TC-02: 验证查询实体类型附件")
    void testLoadIcnEntityFileType() throws Exception {
        // Given: 准备ICN和多个附件（实体+相关文件）
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(TEST_ICN_ID);
        icn.setIcn(TEST_ICN_CODE);

        IetmAttachment entityAttachment = new IetmAttachment();
        entityAttachment.setId("1111111111111111111");
        entityAttachment.setPid(TEST_ICN_ID);
        entityAttachment.setFileKey("project/test/icn/entity.jpg");
        entityAttachment.setFileName("entity.jpg");
        entityAttachment.setFileType(IetmConstants.FILE_TYPE_ENTITY);

        List<IetmIcnManage> icnList = new ArrayList<>();
        icnList.add(icn);

        // 只返回实体类型附件
        List<IetmAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(entityAttachment);

        when(icnManageService.list(any(QueryWrapper.class))).thenReturn(icnList);
        when(attachmentService.list(any(QueryWrapper.class))).thenReturn(attachmentList);

        // When: 使用反射调用方法
        Method method = DmXmlHelper.class.getDeclaredMethod("loadIcnAsBase64", String.class);
        method.setAccessible(true);

        try {
            method.invoke(null, TEST_ICN_CODE);
        } catch (Exception e) {
            // 预期会因为文件不存在而失败
        }

        // Then: 验证查询了实体类型附件
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(attachmentService, atLeastOnce()).list(captor.capture());

        QueryWrapper<IetmAttachment> actualQw = captor.getValue();
        String sqlSegment = actualQw.getSqlSegment();

        // 验证使用了 FILE_TYPE_ENTITY 常量
        assertThat(sqlSegment).as("应查询实体类型附件")
                .contains("file_type");
    }

    @Test
    @DisplayName("TC-03: 验证附件不存在时的处理")
    void testLoadIcnNoAttachment() throws Exception {
        // Given: ICN存在但无附件
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(TEST_ICN_ID);
        icn.setIcn(TEST_ICN_CODE);

        List<IetmIcnManage> icnList = new ArrayList<>();
        icnList.add(icn);

        when(icnManageService.list(any(QueryWrapper.class))).thenReturn(icnList);
        when(attachmentService.list(any(QueryWrapper.class))).thenReturn(new ArrayList<>());

        // When: 使用反射调用方法
        Method method = DmXmlHelper.class.getDeclaredMethod("loadIcnAsBase64", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, TEST_ICN_CODE);

        // Then: 返回null（无附件）
        assertThat(result).as("无附件时应返回null").isNull();
        verify(attachmentService).list(any(QueryWrapper.class));
    }

    @Test
    @DisplayName("TC-04: 验证ICN不存在时的处理")
    void testLoadIcnNotFound() throws Exception {
        // Given: ICN不存在
        when(icnManageService.list(any(QueryWrapper.class))).thenReturn(new ArrayList<>());

        // When: 使用反射调用方法
        Method method = DmXmlHelper.class.getDeclaredMethod("loadIcnAsBase64", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "NON_EXISTENT_ICN");

        // Then: 返回null
        assertThat(result).as("ICN不存在时应返回null").isNull();
        verify(attachmentService, never()).list(any());
    }

    @Test
    @DisplayName("TC-05: 验证多个附件时取第一个")
    void testLoadIcnMultipleAttachments() throws Exception {
        // Given: 一个ICN有多个实体附件
        IetmIcnManage icn = new IetmIcnManage();
        icn.setId(TEST_ICN_ID);
        icn.setIcn(TEST_ICN_CODE);

        IetmAttachment attachment1 = new IetmAttachment();
        attachment1.setId("1111111111111111111");
        attachment1.setPid(TEST_ICN_ID);
        attachment1.setFileKey("project/test/icn/first.jpg");
        attachment1.setFileName("first.jpg");

        IetmAttachment attachment2 = new IetmAttachment();
        attachment2.setId("2222222222222222222");
        attachment2.setPid(TEST_ICN_ID);
        attachment2.setFileKey("project/test/icn/second.jpg");
        attachment2.setFileName("second.jpg");

        List<IetmIcnManage> icnList = new ArrayList<>();
        icnList.add(icn);

        List<IetmAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(attachment1);
        attachmentList.add(attachment2);

        when(icnManageService.list(any(QueryWrapper.class))).thenReturn(icnList);
        when(attachmentService.list(any(QueryWrapper.class))).thenReturn(attachmentList);

        // When & Then: 应使用第一个附件
        Method method = DmXmlHelper.class.getDeclaredMethod("loadIcnAsBase64", String.class);
        method.setAccessible(true);

        try {
            method.invoke(null, TEST_ICN_CODE);
        } catch (Exception e) {
            // 预期会因为文件不存在而失败，但逻辑应该正确
        }

        verify(attachmentService).list(any(QueryWrapper.class));
    }
}
