package org.jeecg.modules.ietm.ietmddn;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.ietm.ietmddn.service.IIetmDdnService;
import org.jeecg.modules.ietm.ietmddn.vo.DdnGenerateVO;
import org.jeecg.modules.ietm.ietmddn.vo.DdnGenerateResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DDN Service单元测试
 */
@SpringBootTest
@Transactional
public class IetmDdnServiceTest {

    @Autowired
    private IIetmDdnService ietmDdnService;

    /**
     * 测试：年度首次生成序列号
     */
    @Test
    public void testGenerateNextSeqNumber_FirstOfYear() {
        String year = "2099"; // 使用未来年份避免冲突
        String seqNumber = ietmDdnService.generateNextSeqNumber(year);
        assertEquals("00001", seqNumber, "年度首次序列号应为00001");
    }

    /**
     * 测试：序列号递增
     */
    @Test
    public void testGenerateNextSeqNumber_Increment() {
        String year = "2099";
        String seq1 = ietmDdnService.generateNextSeqNumber(year);
        String seq2 = ietmDdnService.generateNextSeqNumber(year);

        int num1 = Integer.parseInt(seq1);
        int num2 = Integer.parseInt(seq2);

        assertEquals(num1 + 1, num2, "序列号应递增1");
    }

    /**
     * 测试：非法日期格式
     */
    @Test
    public void testGenerateDdn_InvalidDate() {
        DdnGenerateVO params = new DdnGenerateVO();
        params.setDmIds(Arrays.asList("test-dm-id"));
        params.setModelic("TEST");
        params.setSecurity("01");
        params.setSender("00000");
        params.setReceiver("00000");
        params.setIssueDate("2026-13-32"); // 非法日期

        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("projectId", "test-project");

        Exception exception = assertThrows(Exception.class, () -> {
            ietmDdnService.generateDdn(params, projectInfo);
        });

        assertTrue(exception.getMessage().contains("日期格式错误"),
                "应抛出日期格式错误异常");
    }

    /**
     * 测试：空DM列表
     */
    @Test
    public void testGenerateDdn_EmptyDmList() {
        DdnGenerateVO params = new DdnGenerateVO();
        params.setDmIds(new ArrayList<>());  // 空列表

        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("projectId", "test-project");

        Exception exception = assertThrows(JeecgBootException.class, () -> {
            ietmDdnService.generateDdn(params, projectInfo);
        });

        assertTrue(exception.getMessage().contains("DM列表为空"),
                "应抛出DM列表为空异常");
    }

    /**
     * 测试：未打开项目
     */
    @Test
    public void testGenerateDdn_NoProject() {
        DdnGenerateVO params = new DdnGenerateVO();
        params.setDmIds(Arrays.asList("test-dm-id"));

        Exception exception = assertThrows(JeecgBootException.class, () -> {
            ietmDdnService.generateDdn(params, null);
        });

        assertTrue(exception.getMessage().contains("未打开项目"),
                "应抛出未打开项目异常");
    }

    /**
     * 测试：序列号格式（5位补零）
     */
    @Test
    public void testGenerateNextSeqNumber_Format() {
        String year = "2098";
        String seqNumber = ietmDdnService.generateNextSeqNumber(year);

        assertEquals(5, seqNumber.length(), "序列号应为5位");
        assertTrue(seqNumber.matches("\\d{5}"), "序列号应全为数字");
    }
}
