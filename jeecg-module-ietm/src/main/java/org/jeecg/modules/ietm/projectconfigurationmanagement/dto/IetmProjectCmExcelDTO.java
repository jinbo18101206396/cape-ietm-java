package org.jeecg.modules.ietm.projectconfigurationmanagement.dto;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;
import java.io.Serializable;

/**
 * Excel导入数据传输对象
 * Excel模板：14列，二级表头
 * 第1行：区分码、系统码、子系统、子子系统、部件码、拆分码、拆分码变量
 * 第2行：编码、名称（交替出现）
 * 第3行开始：数据行
 */
@Data
public class IetmProjectCmExcelDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Excel行号（用于错误提示，从3开始） */
    private Integer rowNum;

    /** 一级编码（区分码-编码） */
    @Excel(name = "编码", orderNum = "0")
    private String code1;

    /** 一级技术名称（区分码-名称） */
    @Excel(name = "名称", orderNum = "1")
    private String title1;

    /** 二级编码（系统码-编码） */
    @Excel(name = "编码", orderNum = "2")
    private String code2;

    /** 二级技术名称（系统码-名称） */
    @Excel(name = "名称", orderNum = "3")
    private String title2;

    /** 三级编码（子系统-编码） */
    @Excel(name = "编码", orderNum = "4")
    private String code3;

    /** 三级技术名称（子系统-名称） */
    @Excel(name = "名称", orderNum = "5")
    private String title3;

    /** 四级编码（子子系统-编码） */
    @Excel(name = "编码", orderNum = "6")
    private String code4;

    /** 四级技术名称（子子系统-名称） */
    @Excel(name = "名称", orderNum = "7")
    private String title4;

    /** 五级编码（部件码-编码） */
    @Excel(name = "编码", orderNum = "8")
    private String code5;

    /** 五级技术名称（部件码-名称） */
    @Excel(name = "名称", orderNum = "9")
    private String title5;

    /** 六级编码（拆分码-编码） */
    @Excel(name = "编码", orderNum = "10")
    private String code6;

    /** 六级技术名称（拆分码-名称） */
    @Excel(name = "名称", orderNum = "11")
    private String title6;

    /** 七级编码（拆分码变量-编码） */
    @Excel(name = "编码", orderNum = "12")
    private String code7;

    /** 七级技术名称（拆分码变量-名称） */
    @Excel(name = "名称", orderNum = "13")
    private String title7;

    /** 校验错误信息 */
    private String errorMsg;

    /** 是否校验通过 */
    private boolean valid = true;

    /**
     * 计算当前节点的层级（1-7）
     * @return 层级
     */
    public Integer calculateTreeLevel() {
        if (isNotBlank(code7)) return 7;
        if (isNotBlank(code6)) return 6;
        if (isNotBlank(code5)) return 5;
        if (isNotBlank(code4)) return 4;
        if (isNotBlank(code3)) return 3;
        if (isNotBlank(code2)) return 2;
        if (isNotBlank(code1)) return 1;
        return 0; // 无有效编码
    }

    /**
     * 获取当前层级的编码
     * @param level 层级（1-7）
     * @return 编码
     */
    public String getCodeByLevel(int level) {
        switch (level) {
            case 1: return code1;
            case 2: return code2;
            case 3: return code3;
            case 4: return code4;
            case 5: return code5;
            case 6: return code6;
            case 7: return code7;
            default: return null;
        }
    }

    /**
     * 获取当前层级的技术名称
     * @param level 层级（1-7）
     * @return 技术名称
     */
    public String getTitleByLevel(int level) {
        switch (level) {
            case 1: return title1;
            case 2: return title2;
            case 3: return title3;
            case 4: return title4;
            case 5: return title5;
            case 6: return title6;
            case 7: return title7;
            default: return null;
        }
    }

    /**
     * 添加错误信息
     * @param error 错误信息
     */
    public void addError(String error) {
        this.valid = false;
        if (this.errorMsg == null || this.errorMsg.isEmpty()) {
            this.errorMsg = error;
        } else {
            this.errorMsg += "; " + error;
        }
    }

    /**
     * 校验编码连续性（不能跳级）
     * @return 错误信息，无错误返回null
     */
    public String checkCodeContinuity() {
        boolean hasCode1 = isNotBlank(code1);
        boolean hasCode2 = isNotBlank(code2);
        boolean hasCode3 = isNotBlank(code3);
        boolean hasCode4 = isNotBlank(code4);
        boolean hasCode5 = isNotBlank(code5);
        boolean hasCode6 = isNotBlank(code6);
        boolean hasCode7 = isNotBlank(code7);

        if (hasCode2 && !hasCode1) return "有二级编码但缺少一级编码";
        if (hasCode3 && !hasCode2) return "有三级编码但缺少二级编码";
        if (hasCode4 && !hasCode3) return "有四级编码但缺少三级编码";
        if (hasCode5 && !hasCode4) return "有五级编码但缺少四级编码";
        if (hasCode6 && !hasCode5) return "有六级编码但缺少五级编码";
        if (hasCode7 && !hasCode6) return "有七级编码但缺少六级编码";

        return null;
    }

    /**
     * 校验编码与技术名称匹配
     * @return 错误信息，无错误返回null
     */
    public String checkCodeTitleMatch() {
        if (isNotBlank(code1) && isBlank(title1)) return "一级编码存在但技术名称为空";
        if (isNotBlank(code2) && isBlank(title2)) return "二级编码存在但技术名称为空";
        if (isNotBlank(code3) && isBlank(title3)) return "三级编码存在但技术名称为空";
        if (isNotBlank(code4) && isBlank(title4)) return "四级编码存在但技术名称为空";
        if (isNotBlank(code5) && isBlank(title5)) return "五级编码存在但技术名称为空";
        if (isNotBlank(code6) && isBlank(title6)) return "六级编码存在但技术名称为空";
        if (isNotBlank(code7) && isBlank(title7)) return "七级编码存在但技术名称为空";
        return null;
    }

    /**
     * 判断字符串是否非空
     */
    private boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 判断字符串是否为空
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
