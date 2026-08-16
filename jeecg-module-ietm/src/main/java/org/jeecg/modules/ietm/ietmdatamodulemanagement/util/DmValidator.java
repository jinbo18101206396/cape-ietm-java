package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.ietm.ietmdatamodulemanagement.entity.IetmDataModule;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * DM数据校验器
 * @author jeecg-boot
 * @date 2026-07-21
 */
@Slf4j
public class DmValidator {

    // 预编译正则，避免每次调用时重复编译
    private static final Pattern ISSUENO_PATTERN = Pattern.compile("^(00[1-9]|0[1-9][0-9]|[1-9][0-9]{2})$");
    private static final Pattern INWORK_PATTERN = Pattern.compile("^\\d{2}$");
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("^[a-z]{2,3}$");  // ISO 639: 2-3位小写字母
    private static final Pattern COUNTRY_PATTERN = Pattern.compile("^[A-Z]{2,3}$");   // ISO 3166: 2-3位大写字母
    private static final Pattern DMC_FORMAT_PATTERN = Pattern.compile("^DMC-[A-Z]+-[A-Z0-9]+-[A-Z0-9]+-.*");

    /**
     * 校验DM必填字段
     *
     * @param dm 数据模块对象
     * @return 校验错误列表
     * @throws IllegalArgumentException dm 为 null 时
     */
    public static List<String> validateRequired(IetmDataModule dm) {
        if (dm == null) {
            throw new IllegalArgumentException("数据模块对象不能为null");
        }

        List<String> errors = new ArrayList<>();
        
        if (oConvertUtils.isEmpty(dm.getProjectId())) {
            errors.add("项目ID不能为空");
        }
        if (oConvertUtils.isEmpty(dm.getSns())) {
            errors.add("SNS编号不能为空");
        }
        if (oConvertUtils.isEmpty(dm.getInfoCode())) {
            errors.add("信息代码不能为空");
        }
        if (oConvertUtils.isEmpty(dm.getOriginator())) {
            errors.add("发行方代码不能为空");
        }
        if (oConvertUtils.isEmpty(dm.getCmNodeId())) {
            errors.add("构型节点不能为空");
        }
        
        return errors;
    }

    /**
     * 校验DMC编码格式
     *
     * @param dmcCode DMC编码字符串
     * @return true-格式正确，false-格式错误
     */
    public static boolean validateDmcFormat(String dmcCode) {
        if (oConvertUtils.isEmpty(dmcCode)) {
            return false;
        }
        return DMC_FORMAT_PATTERN.matcher(dmcCode).matches();
    }

    /**
     * 校验版本号格式（拒绝 "000"）
     *
     * @param issueno 发行编号（001-999，不允许 000）
     * @param inwork  在编编号（00-99）
     * @return 校验错误列表
     */
    public static List<String> validateVersion(String issueno, String inwork) {
        List<String> errors = new ArrayList<>();

        if (oConvertUtils.isEmpty(issueno)) {
            errors.add("发行编号不能为空");
        } else if (!ISSUENO_PATTERN.matcher(issueno).matches()) {
            errors.add("发行编号必须为001-999的3位数字（不允许000）");
        }

        if (oConvertUtils.isEmpty(inwork)) {
            errors.add("在编编号不能为空");
        } else if (!INWORK_PATTERN.matcher(inwork).matches()) {
            errors.add("在编编号必须为2位数字（00-99）");
        }

        return errors;
    }

    /**
     * 校验语言和国家代码
     *
     * @param languageIsoCode 语言ISO代码
     * @param countryIsoCode  国家ISO代码
     * @return 校验错误列表
     */
    public static List<String> validateLocale(String languageIsoCode, String countryIsoCode) {
        List<String> errors = new ArrayList<>();

        if (oConvertUtils.isEmpty(languageIsoCode)) {
            errors.add("语言ISO代码不能为空");
        } else if (!LANGUAGE_PATTERN.matcher(languageIsoCode).matches()) {
            errors.add("语言ISO代码必须为2-3位小写字母（符合ISO 639标准）");
        }

        if (oConvertUtils.isEmpty(countryIsoCode)) {
            errors.add("国家ISO代码不能为空");
        } else if (!COUNTRY_PATTERN.matcher(countryIsoCode).matches()) {
            errors.add("国家ISO代码必须为2-3位大写字母（符合ISO 3166标准）");
        }

        return errors;
    }

    /**
     * 校验XML内容格式（使用真实XML解析）
     *
     * @param xmlContent XML内容
     * @return true-格式正确，false-格式错误
     */
    public static boolean validateXmlContent(String xmlContent) {
        if (oConvertUtils.isEmpty(xmlContent)) {
            return true; // 允许为空
        }

        org.dom4j.io.SAXReader reader = new org.dom4j.io.SAXReader();
        // 禁用外部实体（XXE防护）。旧版解析器不识别该特性时静默降级，
        // 避免把「设置特性失败」误判为「XML格式错误」（与 createSafeSaxReader 一致）。
        setReaderFeatureQuietly(reader, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setReaderFeatureQuietly(reader, "http://xml.org/sax/features/external-general-entities", false);
        setReaderFeatureQuietly(reader, "http://xml.org/sax/features/external-parameter-entities", false);

        // 修复资源泄漏：使用 try-with-resources
        try (java.io.StringReader sr = new java.io.StringReader(xmlContent)) {
            reader.read(sr);
            return true;
        } catch (Exception e) {
            // 解析失败说明 XML 格式不正确
            return false;
        }
    }

    /**
     * 为 dom4j SAXReader 设置 XXE 防护特性；旧版解析器不识别时静默降级（仅记 warn）。
     * 与 setFeature 抛出的 SAXException 分离，避免污染 XML 格式判定。
     */
    private static void setReaderFeatureQuietly(org.dom4j.io.SAXReader reader, String feature, boolean value) {
        try {
            reader.setFeature(feature, value);
        } catch (org.xml.sax.SAXException e) {
            log.warn("设置SAXReader安全特性失败，存在XXE风险: {}", feature);
        }
    }

    /**
     * 全面校验DM对象
     *
     * @param dm 数据模块对象
     * @return 校验错误列表
     * @throws IllegalArgumentException dm 为 null 时
     */
    public static List<String> validateAll(IetmDataModule dm) {
        if (dm == null) {
            throw new IllegalArgumentException("数据模块对象不能为null");
        }

        List<String> errors = new ArrayList<>();

        errors.addAll(validateRequired(dm));
        errors.addAll(validateVersion(dm.getIssueNo(), dm.getInWork()));
        errors.addAll(validateLocale(dm.getLanguageIsoCode(), dm.getCountryIsoCode()));

        return errors;
    }
}
