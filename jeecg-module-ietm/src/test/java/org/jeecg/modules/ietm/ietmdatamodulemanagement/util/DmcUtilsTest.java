package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * DmcUtils 单元测试
 * 验证 SNS 拆解/重建往返幂等性 + modelIdentCode 提取规则
 */
public class DmcUtilsTest {

    // ==================== resolveModelIdentCode（选项2：从项目配置取）====================

    @Test
    public void resolveModelIdentCode_优先用schema() {
        assertEquals("TEST", DmcUtils.resolveModelIdentCode("TEST", "ATA52"));
        assertEquals("ATA52", DmcUtils.resolveModelIdentCode("ATA52", "IGNORE"));
    }

    @Test
    public void resolveModelIdentCode_schema空则用equipmentCode() {
        assertEquals("ATA52", DmcUtils.resolveModelIdentCode(null, "ATA52"));
        assertEquals("TEST", DmcUtils.resolveModelIdentCode("", "TEST"));
    }

    @Test
    public void resolveModelIdentCode_1位补齐() {
        assertEquals("JA", DmcUtils.resolveModelIdentCode("J", null));
        assertEquals("XA", DmcUtils.resolveModelIdentCode(null, "X"));
    }

    @Test
    public void resolveModelIdentCode_超14位截断() {
        assertEquals("ABCDEFGHIJKLMN", DmcUtils.resolveModelIdentCode("ABCDEFGHIJKLMNOPQ", null));
        assertEquals("ABCDEFGHIJKLMN", DmcUtils.resolveModelIdentCode(null, "ABCDEFGHIJKLMNOPQ"));
    }

    @Test
    public void resolveModelIdentCode_全空返回AA() {
        assertEquals("AA", DmcUtils.resolveModelIdentCode(null, null));
        assertEquals("AA", DmcUtils.resolveModelIdentCode("", ""));
        assertEquals("AA", DmcUtils.resolveModelIdentCode("abc", "123-xyz"));  // 含非法字符
    }

    // ==================== decomposeSns ====================

    @Test
    public void decomposeSns_标准6段SNS_含equipname() {
        // 对标老系统 getDmrefByDmc 注释样例 "TEST-A-29-10-01-00A"（含 equipname 首段）
        Map<String, String> r = DmcUtils.decomposeSns("TEST-A-29-10-01-00A");
        assertEquals("TEST", r.get("modelIdentCode"));  // 首段=equipname
        assertEquals("A", r.get("systemDiffCode"));
        assertEquals("29", r.get("systemCode"));
        assertEquals("1", r.get("subSystemCode"));
        assertEquals("0", r.get("subSubSystemCode"));
        assertEquals("01", r.get("assyCode"));
        assertEquals("00", r.get("disassyCode"));
        assertEquals("A", r.get("disassyCodeVariant"));
    }

    @Test
    public void decomposeSns_短SNS_3段() {
        // "TEST-A-29" → modelIdentCode=TEST, systemDiffCode=A, systemCode=29, 其余默认
        Map<String, String> r = DmcUtils.decomposeSns("TEST-A-29");
        assertEquals("TEST", r.get("modelIdentCode"));
        assertEquals("A", r.get("systemDiffCode"));
        assertEquals("29", r.get("systemCode"));
        assertEquals("0", r.get("subSystemCode"));       // 缺失段用默认值
        assertEquals("", r.get("subSubSystemCode"));
        assertEquals("00", r.get("assyCode"));
        assertEquals("00", r.get("disassyCode"));
        assertEquals("", r.get("disassyCodeVariant"));
    }

    @Test
    public void decomposeSns_空SNS_全默认值() {
        Map<String, String> r = DmcUtils.decomposeSns(null);
        assertEquals("", r.get("modelIdentCode"));        // 空，由调用方 resolveModelIdentCode 兜底
        assertEquals("A", r.get("systemDiffCode"));       // coderule 模板首段默认 "A"
        assertEquals("00", r.get("systemCode"));
        assertEquals("0", r.get("subSystemCode"));
        assertEquals("", r.get("subSubSystemCode"));      // 空（对标老系统 substr 越界）
        assertEquals("00", r.get("assyCode"));
        assertEquals("00", r.get("disassyCode"));
        assertEquals("", r.get("disassyCodeVariant"));    // 空
    }

    @Test
    public void decomposeSns_单字符段_空尾段() {
        // parts[3]="1" 只1位 → subSubSystemCode 空
        Map<String, String> r1 = DmcUtils.decomposeSns("TEST-A-29-1-01-00A");
        assertEquals("1", r1.get("subSystemCode"));
        assertEquals("", r1.get("subSubSystemCode"));  // 空，非 "0"

        // parts[5]="00" 只2位 → disassyCodeVariant 空
        Map<String, String> r2 = DmcUtils.decomposeSns("TEST-A-29-10-01-00");
        assertEquals("00", r2.get("disassyCode"));
        assertEquals("", r2.get("disassyCodeVariant"));  // 空，非 "A"
    }

    @Test
    public void decomposeSns_长段名() {
        // subSubSystemCode 可能多位、disassyCodeVariant 可能多位
        Map<String, String> r = DmcUtils.decomposeSns("MODEL-A-000-123-00-00ABCD");
        assertEquals("MODEL", r.get("modelIdentCode"));
        assertEquals("A", r.get("systemDiffCode"));
        assertEquals("000", r.get("systemCode"));
        assertEquals("1", r.get("subSystemCode"));
        assertEquals("23", r.get("subSubSystemCode"));  // 余位
        assertEquals("00", r.get("assyCode"));
        assertEquals("00", r.get("disassyCode"));
        assertEquals("ABCD", r.get("disassyCodeVariant"));  // 余位
    }

    // ==================== composeSns ====================

    @Test
    public void composeSns_标准() {
        String sns = DmcUtils.composeSns("TEST", "A", "29", "1", "0", "01", "00", "A");
        assertEquals("TEST-A-29-10-01-00A", sns);
    }

    @Test
    public void composeSns_空值走默认() {
        // systemDiffCode 空→"A", systemCode 空→"00", subSystemCode="5"+subSubSystemCode空→"5", variant 空→不补
        String sns = DmcUtils.composeSns("MODEL", null, null, "5", null, null, "01", null);
        assertEquals("MODEL-A-00-5-00-01", sns);
    }

    // ==================== 往返幂等 ====================

    @Test
    public void 往返幂等_标准SNS() {
        String original = "TEST-A-29-10-01-00A";
        Map<String, String> decomposed = DmcUtils.decomposeSns(original);
        String recomposed = DmcUtils.composeSns(
            decomposed.get("modelIdentCode"),
            decomposed.get("systemDiffCode"),
            decomposed.get("systemCode"),
            decomposed.get("subSystemCode"),
            decomposed.get("subSubSystemCode"),
            decomposed.get("assyCode"),
            decomposed.get("disassyCode"),
            decomposed.get("disassyCodeVariant")
        );
        assertEquals("往返必须恒等", original, recomposed);
    }

    @Test
    public void 往返幂等_多样例() {
        // 【方案A】全部含 equipname 首段
        String[] samples = {
            "TEST-A-00-00-00-00A",
            "ATA52-B-100-25-12-34BC",
            "X9-C-99-98-97-96YZ",
            "ZBBM01-A-29-1-01-00A",   // 单字符 subSystemCode
            "B2-D-00-00-00-00",        // 无 disassyCodeVariant
            "C3-E-11-2-99-88"          // 单字符 + 无 variant
        };
        for (String sns : samples) {
            Map<String, String> d = DmcUtils.decomposeSns(sns);
            String r = DmcUtils.composeSns(d.get("modelIdentCode"), d.get("systemDiffCode"),
                d.get("systemCode"), d.get("subSystemCode"), d.get("subSubSystemCode"),
                d.get("assyCode"), d.get("disassyCode"), d.get("disassyCodeVariant"));
            assertEquals("SNS=" + sns + " 往返失败", sns, r);
        }
    }
}
