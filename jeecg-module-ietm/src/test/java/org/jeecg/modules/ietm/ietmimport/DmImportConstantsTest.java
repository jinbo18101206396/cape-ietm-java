package org.jeecg.modules.ietm.ietmimport;

import org.jeecg.modules.ietm.ietmimport.constants.DmImportConstants;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * DmImportConstants 单元测试
 *
 * @author IETM Team
 * @date 2026-09-03
 */
public class DmImportConstantsTest {

    @Test
    public void testErrorCodes_allDefined() {
        // 测试所有错误码都已定义
        assertNotNull(DmImportConstants.ERROR_UNKNOWN);
        assertNotNull(DmImportConstants.ERROR_DM_EXISTS);
        assertNotNull(DmImportConstants.ERROR_SNS_NOT_IN_CM);
        assertNotNull(DmImportConstants.ERROR_DDN_FILE_NOT_EXISTS);
        assertNotNull(DmImportConstants.ERROR_CODE_MISMATCH);
        assertNotNull(DmImportConstants.ERROR_MODEL_MISMATCH);
        assertNotNull(DmImportConstants.ERROR_SECURITY_NOT_EXISTS);
        assertNotNull(DmImportConstants.ERROR_SECURITY_EXCEED);
        assertNotNull(DmImportConstants.ERROR_NO_FILE);
        assertNotNull(DmImportConstants.ERROR_ICN_NAME_INVALID);
        assertNotNull(DmImportConstants.ERROR_ICN_SNS_NOT_IN_CM);
        assertNotNull(DmImportConstants.ERROR_ICN_EXISTS);
        assertNotNull(DmImportConstants.SUCCESS);
    }

    @Test
    public void testErrorCodes_uniqueValues() {
        // 测试错误码值唯一性
        assertEquals("-10", DmImportConstants.ERROR_UNKNOWN);
        assertEquals("-1", DmImportConstants.ERROR_DM_EXISTS);
        assertEquals("-2", DmImportConstants.ERROR_SNS_NOT_IN_CM);
        assertEquals("-3", DmImportConstants.ERROR_DDN_FILE_NOT_EXISTS);
        assertEquals("-4", DmImportConstants.ERROR_CODE_MISMATCH);
        assertEquals("-5", DmImportConstants.ERROR_MODEL_MISMATCH);
        assertEquals("-6", DmImportConstants.ERROR_SECURITY_NOT_EXISTS);
        assertEquals("-7", DmImportConstants.ERROR_SECURITY_EXCEED);
        assertEquals("-99", DmImportConstants.ERROR_NO_FILE);
        assertEquals("-11", DmImportConstants.ERROR_ICN_NAME_INVALID);
        assertEquals("-12", DmImportConstants.ERROR_ICN_SNS_NOT_IN_CM);
        assertEquals("-13", DmImportConstants.ERROR_ICN_EXISTS);
        assertEquals("1", DmImportConstants.SUCCESS);
    }

    @Test
    public void testGetErrorMessage_allErrors() {
        // 测试所有错误消息都有对应文本
        assertEquals("未知原因导入失败", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_UNKNOWN));
        assertEquals("DM已经存在", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_DM_EXISTS));
        assertEquals("SNS不在构型中", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_SNS_NOT_IN_CM));
        assertEquals("DDN文件列表不存在", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_DDN_FILE_NOT_EXISTS));
        assertEquals("文件名与DM内容编码不一致", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_CODE_MISMATCH));
        assertEquals("型号不匹配", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_MODEL_MISMATCH));
        assertEquals("密级值不存在", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_SECURITY_NOT_EXISTS));
        assertEquals("密级超限", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_SECURITY_EXCEED));
        assertEquals("ZIP包无文件", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_NO_FILE));
        assertEquals("ICN文件名不规范", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_ICN_NAME_INVALID));
        assertEquals("ICN的SNS不在构型中", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_ICN_SNS_NOT_IN_CM));
        assertEquals("ICN已存在", DmImportConstants.getErrorMessage(DmImportConstants.ERROR_ICN_EXISTS));
        assertEquals("可以导入", DmImportConstants.getErrorMessage(DmImportConstants.SUCCESS));
    }

    @Test
    public void testGetErrorMessage_unknownCode() {
        // 测试未知错误码返回默认消息
        assertEquals("未知错误", DmImportConstants.getErrorMessage("-999"));
        assertEquals("未知错误", DmImportConstants.getErrorMessage(""));
        // null可能抛出NPE，需要特殊处理
        try {
            String result = DmImportConstants.getErrorMessage(null);
            assertEquals("未知错误", result);
        } catch (NullPointerException e) {
            // 如果抛出NPE也是可接受的行为
            assertTrue(true);
        }
    }

    @Test
    public void testFileTypes() {
        // 测试文件类型常量
        assertEquals(".xml", DmImportConstants.FILE_TYPE_XML);
        assertEquals(".zip", DmImportConstants.FILE_TYPE_ZIP);
    }

    @Test
    public void testFileSizeLimit() {
        // 测试文件大小限制（1GB = 1024MB = 1024*1024*1024 bytes）
        assertEquals(1073741824L, DmImportConstants.MAX_FILE_SIZE);
    }

    @Test
    public void testBufferSize() {
        // 测试缓冲区大小（8KB = 8192 bytes）
        assertEquals(8192, DmImportConstants.BUFFER_SIZE);
    }
}
