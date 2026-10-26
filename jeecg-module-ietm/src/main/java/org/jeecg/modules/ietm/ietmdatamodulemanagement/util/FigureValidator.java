package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.*;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import java.util.*;

/**
 * Figure数据验证工具
 *
 * 功能:
 * - validateFigure: 验证完整Figure对象
 * - validateHotspot: 验证单个热点
 * - validateLegend: 验证单个图例
 * - validateIcnExists: 验证ICN是否真实存在（需注入IetmIcnManageService）
 */
public class FigureValidator {

    private static IIetmIcnManageService icnManageService;

    /**
     * 注入ICN管理服务（由Spring使用）
     */
    public void setIcnManageService(IIetmIcnManageService service) {
        FigureValidator.icnManageService = service;
    }

    /**
     * 验证完整figure对象
     */
    public static ValidationResult validateFigure(FigureResponseVO figure) {
        ValidationResult result = new ValidationResult();

        // 标题验证(必填)
        if (figure.getTitle() == null || figure.getTitle().trim().isEmpty()) {
            result.addError("Figure标题必填");
            return result;
        }
        if (figure.getTitle().length() > 200) {
            result.addError("Figure标题长度不能超过200个字符");
        }

        // 图形验证(至少1个,每个icn必填)
        if (figure.getGraphics() == null || figure.getGraphics().isEmpty()) {
            result.addError("至少需要添加一个图形");
            return result;
        }

        for (int i = 0; i < figure.getGraphics().size(); i++) {
            GraphicItemVO graphic = figure.getGraphics().get(i);

            // icn必填
            if (graphic.getIcn() == null || graphic.getIcn().trim().isEmpty()) {
                result.addError("第" + (i + 1) + "个图形的ICN编码不能为空");
            }
            if (graphic.getIcn() != null && graphic.getIcn().length() > 50) {
                result.addError("第" + (i + 1) + "个图形的ICN编码长度不能超过50个字符");
            }

            // ICN存在性验证
            if (graphic.getIcn() != null && !graphic.getIcn().trim().isEmpty() && icnManageService != null) {
                boolean icnExists = isIcnExists(graphic.getIcn());
                if (!icnExists) {
                    result.addError("第" + (i + 1) + "个图形的ICN编码不存在: " + graphic.getIcn());
                }
            }

            // 热点验证
            if (graphic.getHotspots() != null && !graphic.getHotspots().isEmpty()) {
                ValidationResult hsResult = validateHotspots(graphic.getHotspots(), i);
                result.merge(hsResult);
            }
        }

        // 图例验证(可选,但如果存在则需完整)
        if (figure.getLegends() != null && !figure.getLegends().isEmpty()) {
            ValidationResult legendResult = validateLegends(figure.getLegends());
            result.merge(legendResult);
        }

        return result;
    }

    /**
     * 验证单个热点
     */
    public static ValidationResult validateHotspot(HotspotItemVO hotspot, int index) {
        ValidationResult result = new ValidationResult();

        // apsid必填
        if (hotspot.getApsid() == null || hotspot.getApsid().trim().isEmpty()) {
            result.addError("热点" + index + "的APS ID不能为空");
        }
        if (hotspot.getApsid() != null && hotspot.getApsid().length() > 100) {
            result.addError("热点" + index + "的APS ID长度不能超过100个字符");
        }

        // name必填
        if (hotspot.getName() == null || hotspot.getName().trim().isEmpty()) {
            result.addError("热点" + index + "的名称不能为空");
        }
        if (hotspot.getName() != null && hotspot.getName().length() > 200) {
            result.addError("热点" + index + "的名称长度不能超过200个字符");
        }

        // id可选
        if (hotspot.getId() != null && hotspot.getId().length() > 50) {
            result.addError("热点" + index + "的ID长度不能超过50个字符");
        }

        return result;
    }

    /**
     * 批量验证热点数组
     */
    private static ValidationResult validateHotspots(List<HotspotItemVO> hotspots, int graphicIndex) {
        ValidationResult result = new ValidationResult();

        for (int i = 0; i < hotspots.size(); i++) {
            ValidationResult hsResult = validateHotspot(hotspots.get(i), i);
            if (!hsResult.isValid()) {
                for (String error : hsResult.getErrors()) {
                    result.addError("第" + (graphicIndex + 1) + "个图形 - " + error);
                }
            }
        }

        // 检查重复的apsid
        Set<String> apsidSet = new HashSet<>();
        for (HotspotItemVO hotspot : hotspots) {
            if (!apsidSet.add(hotspot.getApsid())) {
                result.addError("第" + (graphicIndex + 1) + "个图形存在重复的APS ID: " + hotspot.getApsid());
            }
        }

        return result;
    }

    /**
     * 验证单个图例
     */
    public static ValidationResult validateLegend(LegendItemVO legend, int index) {
        ValidationResult result = new ValidationResult();

        // no必填
        if (legend.getNo() == null || legend.getNo().trim().isEmpty()) {
            result.addError("图例" + index + "的标号不能为空");
        }
        if (legend.getNo() != null && legend.getNo().length() > 50) {
            result.addError("图例" + index + "的标号长度不能超过50个字符");
        }

        // name必填
        if (legend.getName() == null || legend.getName().trim().isEmpty()) {
            result.addError("图例" + index + "的名称不能为空");
        }
        if (legend.getName() != null && legend.getName().length() > 200) {
            result.addError("图例" + index + "的名称长度不能超过200个字符");
        }

        return result;
    }

    /**
     * 批量验证图例数组
     */
    private static ValidationResult validateLegends(List<LegendItemVO> legends) {
        ValidationResult result = new ValidationResult();

        for (int i = 0; i < legends.size(); i++) {
            ValidationResult legendResult = validateLegend(legends.get(i), i);
            result.merge(legendResult);
        }

        // 检查重复的no
        Set<String> noSet = new HashSet<>();
        for (LegendItemVO legend : legends) {
            if (!noSet.add(legend.getNo())) {
                result.addError("存在重复的图例标号: " + legend.getNo());
            }
        }

        return result;
    }

    /**
     * 检查ICN是否存在
     */
    private static boolean isIcnExists(String icnCode) {
        try {
            if (icnManageService == null) {
                return true; // 服务未注入时跳过检查
            }
            List<IetmIcnManage> list = icnManageService.lambdaQuery()
                .eq(IetmIcnManage::getUniqueId, icnCode)
                .list();
            return !list.isEmpty();
        } catch (Exception e) {
            // 查询出错时，允许继续（避免数据库故障导致保存失败）
            return true;
        }
    }

    /**
     * 验证结果对象
     */
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();

        public void addError(String error) {
            this.errors.add(error);
        }

        public boolean isValid() {
            return this.errors.isEmpty();
        }

        public void merge(ValidationResult other) {
            this.errors.addAll(other.getErrors());
        }

        public String getErrorMessage() {
            return String.join("\n", this.errors);
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
