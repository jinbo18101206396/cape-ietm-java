package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 版本号计算器
 * @author jeecg-boot
 * @date 2026-07-21
 */
public class VersionCalculator {

    /** inwork 最大值 */
    private static final int MAX_INWORK = 99;
    /** issueno 最大值 */
    private static final int MAX_ISSUENO = 999;

    /**
     * 升级 inwork 版本号
     * 规则：inwork 从 00 升级到 99，达到 99 时升级 issueno 并重置 inwork 为 00
     *
     * @param currentInwork  当前 inwork（00-99），不能为 null 或非数字
     * @param currentIssueno 当前 issueno（001-999），不能为 null 或非数字
     * @return Map 包含 newInwork 和 newIssueno
     * @throws IllegalArgumentException 参数为 null 或非数字时
     * @throws IllegalStateException    版本号已达最大值时
     */
    public static Map<String, String> upgradeInwork(String currentInwork, String currentIssueno) {
        int inwork = parseVersion("inwork", currentInwork, 0, MAX_INWORK);
        int issueno = parseVersion("issueno", currentIssueno, 1, MAX_ISSUENO);

        if (inwork >= MAX_INWORK) {
            // inwork 达到 99，升级 issueno
            if (issueno >= MAX_ISSUENO) {
                throw new IllegalStateException(
                    String.format("版本号已达最大值（issueno=%d, inwork=%d），无法继续升级", MAX_ISSUENO, MAX_INWORK));
            }
            issueno++;
            inwork = 0;
        } else {
            inwork++;
        }

        Map<String, String> result = new HashMap<>();
        result.put("newInwork", String.format("%02d", inwork));
        result.put("newIssueno", String.format("%03d", issueno));
        return result;
    }

    /**
     * 升级 issueno 版本号，重置 inwork 为 00
     *
     * @param currentIssueno 当前 issueno（001-999），不能为 null 或非数字
     * @return Map 包含 newInwork 和 newIssueno
     * @throws IllegalArgumentException 参数为 null 或非数字时
     * @throws IllegalStateException    版本号已达最大值时
     */
    public static Map<String, String> upgradeIssueno(String currentIssueno) {
        int issueno = parseVersion("issueno", currentIssueno, 1, MAX_ISSUENO);

        if (issueno >= MAX_ISSUENO) {
            throw new IllegalStateException(
                String.format("版本号已达最大值（issueno=%d），无法继续升级", MAX_ISSUENO));
        }
        issueno++;

        Map<String, String> result = new HashMap<>();
        result.put("newInwork", "00");
        result.put("newIssueno", String.format("%03d", issueno));
        return result;
    }

    /**
     * 版本比较
     *
     * @return 正数：版本1 > 版本2；0：相等；负数：版本1 < 版本2
     */
    public static int compare(String inwork1, String issueno1, String inwork2, String issueno2) {
        int issue1 = parseVersion("issueno1", issueno1, 0, MAX_ISSUENO);
        int issue2 = parseVersion("issueno2", issueno2, 0, MAX_ISSUENO);

        if (issue1 != issue2) {
            return Integer.compare(issue1, issue2);
        }

        int work1 = parseVersion("inwork1", inwork1, 0, MAX_INWORK);
        int work2 = parseVersion("inwork2", inwork2, 0, MAX_INWORK);
        return Integer.compare(work1, work2);
    }

    /**
     * 格式化版本号显示（格式：issueno-inwork，如 "001-00"）
     *
     * @param inwork  inwork 编号
     * @param issueno issueno 编号
     * @return 格式化字符串
     */
    public static String format(String inwork, String issueno) {
        return String.format("%s-%s", issueno, inwork);
    }

    /**
     * 解析版本号字符串为整数，并校验范围
     *
     * @param fieldName  字段名（用于异常信息）
     * @param value      字符串值
     * @param minVal     最小允许值（含）
     * @param maxVal     最大允许值（含）
     * @return 解析后的整数
     * @throws IllegalArgumentException 值为 null、空或非数字时
     */
    private static int parseVersion(String fieldName, String value, int minVal, int maxVal) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 必须为数字格式，当前值: " + value);
        }
        if (parsed < minVal || parsed > maxVal) {
            throw new IllegalArgumentException(
                String.format("%s 值 %d 超出范围 [%d, %d]", fieldName, parsed, minVal, maxVal));
        }
        return parsed;
    }
}
