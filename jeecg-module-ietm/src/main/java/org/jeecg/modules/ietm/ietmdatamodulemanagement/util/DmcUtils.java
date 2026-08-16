package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * DMC / SNS 拆解工具类
 * 对标老系统 IetmEditorUtils-src.js:1372-1417 getDmrefByDmc 的按位拆解规则
 *
 * @Author: jeecg-boot
 * @Date: 2026-08-04
 */
public class DmcUtils {

    /**
     * 从项目配置获取 modelIdentCode（选项 2：对标老系统"读进来是啥就用啥"+ 项目级默认值）
     * 老系统没有 extractModelIdentCode 方法，modelIdentCode 永远从 XML 读取，不生成
     * Vue 系统需要默认值时，从项目装备代码取
     *
     * @param schema           DM 已有的 schema 字段（导入/复制时可能已有值）
     * @param equipmentCode    项目的装备代码（如 "ATA52" / "TEST"）
     * @return modelIdentCode（2-14位大写字母数字），兜底返回 "AA"
     */
    public static String resolveModelIdentCode(String schema, String equipmentCode) {
        // 1. 优先用 DM 已有的 schema
        if (StringUtils.isNotBlank(schema)) {
            String cleaned = schema.trim();
            // 必须原始就是大写字母数字，不自动转大写（防止误将非法值变合法）
            if (cleaned.matches("[A-Z0-9]{2,14}")) {
                return cleaned;
            }
            // 不足2位补齐，超14位截断（仅当原始就合法时）
            if (cleaned.matches("[A-Z0-9]+")) {
                if (cleaned.length() < 2) {
                    return (cleaned + "A").substring(0, 2);
                } else if (cleaned.length() > 14) {
                    return cleaned.substring(0, 14);
                } else {
                    return cleaned;
                }
            }
        }

        // 2. schema 为空或非法，取项目装备代码
        if (StringUtils.isNotBlank(equipmentCode)) {
            String cleaned = equipmentCode.trim();
            if (cleaned.matches("[A-Z0-9]{2,14}")) {
                return cleaned;
            }
            if (cleaned.matches("[A-Z0-9]+")) {
                if (cleaned.length() < 2) {
                    return (cleaned + "A").substring(0, 2);
                } else if (cleaned.length() > 14) {
                    return cleaned.substring(0, 14);
                } else {
                    return cleaned;
                }
            }
        }

        // 3. 兜底默认值
        return "AA";
    }

    /**
     * 拆解 SNS 为 dmCode 8 属性（按 - 拆分 + 组合段内切）
     * 对标老系统 IetmEditorUtils-src.js getDmrefByDmc:1373-1416（逐行核实）
     *
     * 【方案A】SNS 含 equipname 作首段（= modelIdentCode），与老系统一致：
     *   SNS = {modelIdentCode}-{systemDiffCode}-{systemCode}-{subSystem+subSubSystem}-{assyCode}-{disassyCode+disassyCodeVariant}
     *   样例（老系统注释）："TEST-A-29-10-01-00A"
     *     → modelIdentCode="TEST", systemDiffCode="A", systemCode="29",
     *       subSystemCode="1", subSubSystemCode="0", assyCode="01",
     *       disassyCode="00", disassyCodeVariant="A"
     *   注：infoCode/itemLocationCode 属 DMC 的 dmc[7]/dmc[8]，不在 SNS 内，此处不拆。
     *
     * @param sns SNS 字符串（如 "TEST-A-29-10-01-00A"）
     * @return Map 包含 8 个键：modelIdentCode/systemDiffCode/systemCode/subSystemCode/
     *         subSubSystemCode/assyCode/disassyCode/disassyCodeVariant，缺失段返回默认值
     */
    public static Map<String, String> decomposeSns(String sns) {
        Map<String, String> result = new HashMap<>();

        // 默认值（对标老系统 getDmrefByDmc 的 substr 越界处理 + coderule 模板 "A-00-0-0-00-00-A"）
        result.put("modelIdentCode", "");     // 首段=equipname，缺失时由调用方 resolveModelIdentCode 兜底
        result.put("systemDiffCode", "A");
        result.put("systemCode", "00");
        result.put("subSystemCode", "0");
        result.put("subSubSystemCode", "");   // 空字符串（老系统 substr(1) 越界返回空）
        result.put("assyCode", "00");
        result.put("disassyCode", "00");
        result.put("disassyCodeVariant", "");  // 空字符串（老系统 substr(2) 越界返回空）

        if (StringUtils.isBlank(sns)) {
            return result;
        }

        // 按 - 拆分
        String[] parts = sns.split("-");

        // [0] modelIdentCode（=equipname，SNS[0]=DMC[1]）
        if (parts.length > 0 && StringUtils.isNotBlank(parts[0])) {
            result.put("modelIdentCode", parts[0].trim());
        }

        // [1] systemDiffCode（1-4位，SNS[1]=DMC[2]）
        if (parts.length > 1 && StringUtils.isNotBlank(parts[1])) {
            result.put("systemDiffCode", parts[1].trim());
        }

        // [2] systemCode（2-3位，SNS[2]=DMC[3]）
        if (parts.length > 2 && StringUtils.isNotBlank(parts[2])) {
            result.put("systemCode", parts[2].trim());
        }

        // [3] subSystemCode(1位) + subSubSystemCode(余位)（SNS[3]=DMC[4]，老系统 substr(0,1)/substr(1)）
        if (parts.length > 3 && StringUtils.isNotBlank(parts[3])) {
            String segment = parts[3].trim();
            result.put("subSystemCode", segment.substring(0, Math.min(1, segment.length())));
            if (segment.length() > 1) {
                result.put("subSubSystemCode", segment.substring(1));  // 留空对标老系统 substr 越界
            }
        }

        // [4] assyCode（2位，SNS[4]=DMC[5]）
        if (parts.length > 4 && StringUtils.isNotBlank(parts[4])) {
            result.put("assyCode", parts[4].trim());
        }

        // [5] disassyCode(前2位) + disassyCodeVariant(余位)（SNS[5]=DMC[6]，老系统 substr(0,2)/substr(2)）
        if (parts.length > 5 && StringUtils.isNotBlank(parts[5])) {
            String segment = parts[5].trim();
            result.put("disassyCode", segment.substring(0, Math.min(2, segment.length())));
            if (segment.length() > 2) {
                result.put("disassyCodeVariant", segment.substring(2));  // 留空对标老系统
            }
        }

        return result;
    }

    /**
     * 从 dmCode 8 属性重建 SNS（导入路径 B1/B2 用）
     * 【方案A】SNS 含 modelIdentCode(=equipname) 作首段，与老系统一致。
     *
     * @param modelIdentCode   模式识别码（=equipname，SNS 首段）
     * @param systemDiffCode   系统差异码
     * @param systemCode       系统码
     * @param subSystemCode    子系统码
     * @param subSubSystemCode 子子系统码
     * @param assyCode         组件码
     * @param disassyCode      拆分件码
     * @param disassyCodeVariant 拆分件变体
     * @return SNS 字符串（如 "TEST-A-29-10-01-00A"）
     */
    public static String composeSns(String modelIdentCode, String systemDiffCode, String systemCode,
                                     String subSystemCode, String subSubSystemCode,
                                     String assyCode, String disassyCode,
                                     String disassyCodeVariant) {
        StringBuilder sns = new StringBuilder();

        // [0] modelIdentCode（=equipname）
        if (StringUtils.isNotBlank(modelIdentCode)) {
            sns.append(modelIdentCode);
        }

        // [1] systemDiffCode
        if (sns.length() > 0) sns.append("-");
        if (StringUtils.isNotBlank(systemDiffCode)) {
            sns.append(systemDiffCode);
        } else {
            sns.append("A");  // 默认值（coderule 模板首段）
        }

        // [2] systemCode
        sns.append("-");
        if (StringUtils.isNotBlank(systemCode)) {
            sns.append(systemCode);
        } else {
            sns.append("00");  // 默认值
        }

        // [3] subSystemCode + subSubSystemCode（不带分隔符）
        sns.append("-");
        if (StringUtils.isNotBlank(subSystemCode)) {
            sns.append(subSystemCode);
        } else {
            sns.append("0");
        }
        if (StringUtils.isNotBlank(subSubSystemCode)) {
            sns.append(subSubSystemCode);
        }
        // 注意：subSubSystemCode 为空时不补默认值（对标老系统 substr 越界行为）

        // [4] assyCode
        sns.append("-");
        if (StringUtils.isNotBlank(assyCode)) {
            sns.append(assyCode);
        } else {
            sns.append("00");
        }

        // [5] disassyCode + disassyCodeVariant（不带分隔符）
        sns.append("-");
        if (StringUtils.isNotBlank(disassyCode)) {
            sns.append(disassyCode);
        } else {
            sns.append("00");
        }
        if (StringUtils.isNotBlank(disassyCodeVariant)) {
            sns.append(disassyCodeVariant);
        }
        // 注意：disassyCodeVariant 为空时不补默认值（对标老系统）

        return sns.toString();
    }

    /**
     * 辅助方法：非空取值，空则返回默认值
     */
    private static String nvl(String value, String defaultValue) {
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }
}
