package org.jeecg.modules.ietm.ietmimport;

import org.jeecg.modules.ietm.ietmimport.constants.DmImportConstants;
import org.jeecg.modules.ietm.ietmimport.vo.ImportFileItemVO;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ImportFileItemVO 单元测试
 *
 * @author IETM Team
 * @date 2026-09-03
 */
public class ImportFileItemVOTest {

    @Test
    public void testCanImport_successCode() {
        // 测试：resultCode = "1" 时可以导入
        ImportFileItemVO item = new ImportFileItemVO();
        item.setResultCode(DmImportConstants.SUCCESS);
        assertTrue(item.canImport());
    }

    @Test
    public void testCanImport_errorCodes() {
        // 测试：所有错误码都不能导入
        String[] errorCodes = {
            DmImportConstants.ERROR_UNKNOWN,
            DmImportConstants.ERROR_DM_EXISTS,
            DmImportConstants.ERROR_SNS_NOT_IN_CM,
            DmImportConstants.ERROR_CODE_MISMATCH,
            DmImportConstants.ERROR_MODEL_MISMATCH,
            DmImportConstants.ERROR_SECURITY_NOT_EXISTS,
            DmImportConstants.ERROR_SECURITY_EXCEED,
            DmImportConstants.ERROR_NO_FILE,
            DmImportConstants.ERROR_ICN_NAME_INVALID,
            DmImportConstants.ERROR_ICN_SNS_NOT_IN_CM,
            DmImportConstants.ERROR_ICN_EXISTS
        };

        for (String errorCode : errorCodes) {
            ImportFileItemVO item = new ImportFileItemVO();
            item.setResultCode(errorCode);
            assertFalse("错误码 " + errorCode + " 应该不能导入", item.canImport());
        }
    }

    @Test
    public void testCanImport_nullOrEmpty() {
        // 测试：null或空字符串不能导入
        ImportFileItemVO item1 = new ImportFileItemVO();
        item1.setResultCode(null);
        assertFalse(item1.canImport());

        ImportFileItemVO item2 = new ImportFileItemVO();
        item2.setResultCode("");
        assertFalse(item2.canImport());
    }

    @Test
    public void testSettersAndGetters() {
        // 测试所有setter和getter
        ImportFileItemVO item = new ImportFileItemVO();

        item.setFileName("test.xml");
        assertEquals("test.xml", item.getFileName());

        item.setFileType("DM");
        assertEquals("DM", item.getFileType());

        item.setResultCode("1");
        assertEquals("1", item.getResultCode());

        item.setResultMessage("可以导入");
        assertEquals("可以导入", item.getResultMessage());

        item.setDmcCode("DMC-001");
        assertEquals("DMC-001", item.getDmcCode());

        item.setTempFilePath("/temp/test.xml");
        assertEquals("/temp/test.xml", item.getTempFilePath());

        item.setXmlContent("<xml>content</xml>");
        assertEquals("<xml>content</xml>", item.getXmlContent());
    }

    @Test
    public void testDmFileItem() {
        // 测试构建完整的DM文件项
        ImportFileItemVO item = new ImportFileItemVO();
        item.setFileName("DMC-MODEL001-A-SNS001.xml");
        item.setFileType("DM");
        item.setResultCode(DmImportConstants.SUCCESS);
        item.setResultMessage("可以导入");
        item.setDmcCode("DMC-MODEL001-A-SNS001");
        item.setTempFilePath("/temp/123_DMC-MODEL001-A-SNS001.xml");
        item.setXmlContent("<?xml version=\"1.0\"?><dmodule/>");

        assertTrue(item.canImport());
        assertEquals("DM", item.getFileType());
        assertNotNull(item.getXmlContent());
        assertNotNull(item.getDmcCode());
    }

    @Test
    public void testIcnFileItem() {
        // 测试构建完整的ICN文件项
        ImportFileItemVO item = new ImportFileItemVO();
        item.setFileName("ICN-MODEL001-SNS001-00001.png");
        item.setFileType("ICN");
        item.setResultCode(DmImportConstants.SUCCESS);
        item.setResultMessage("可以导入");
        item.setTempFilePath("/temp/123_ICN-MODEL001-SNS001-00001.png");

        assertTrue(item.canImport());
        assertEquals("ICN", item.getFileType());
        assertNull(item.getXmlContent()); // ICN没有XML内容
        assertNull(item.getDmcCode()); // ICN没有DMC编码
    }

    @Test
    public void testFailedFileItem() {
        // 测试构建失败的文件项
        ImportFileItemVO item = new ImportFileItemVO();
        item.setFileName("DMC-INVALID.xml");
        item.setFileType("DM");
        item.setResultCode(DmImportConstants.ERROR_DM_EXISTS);
        item.setResultMessage("DM已经存在");

        assertFalse(item.canImport());
        assertEquals(DmImportConstants.ERROR_DM_EXISTS, item.getResultCode());
    }
}
