package org.jeecg.modules.ietm.ietmdatamodulemanagement.util;

import org.jeecg.modules.ietm.ietmdatamodulemanagement.vo.*;
import java.util.*;

/**
 * Multimedia数据验证工具
 *
 * 功能:
 * - validateMultimedia: 验证MultimediaResponseVO数据完整性
 * - validateHotspots: 验证热点数据
 */
public class MultimediaValidator {

    /**
     * 验证Multimedia数据
     *
     * @param multimedia MultimediaResponseVO对象
     * @return 验证结果
     */
    public static ValidationResult validateMultimedia(MultimediaResponseVO multimedia) {
        List<String> errors = new ArrayList<>();

        if (multimedia == null) {
            errors.add("multimedia对象不能为空");
            return new ValidationResult(false, errors);
        }

        // 标题验证（必填）
        if (multimedia.getTitle() == null || multimedia.getTitle().trim().isEmpty()) {
            errors.add("Multimedia标题必填");
        }
        if (multimedia.getTitle() != null && multimedia.getTitle().length() > 200) {
            errors.add("Multimedia标题长度不能超过200个字符");
        }

        // 多媒体对象验证（至少1个，每个icn必填）
        if (multimedia.getMultimediaObjects() == null ||
            multimedia.getMultimediaObjects().isEmpty()) {
            errors.add("至少需要添加一个多媒体对象");
        } else {
            for (int i = 0; i < multimedia.getMultimediaObjects().size(); i++) {
                MultimediaObjectItemVO obj = multimedia.getMultimediaObjects().get(i);

                // icn必填
                if (obj.getIcn() == null || obj.getIcn().trim().isEmpty()) {
                    errors.add("第" + (i + 1) + "个多媒体对象的ICN编码不能为空");
                }

                // 热点验证
                if (obj.getHotspots() != null && !obj.getHotspots().isEmpty()) {
                    ValidationResult hsValidation = validateHotspots(obj.getHotspots(), i);
                    if (!hsValidation.isValid()) {
                        errors.addAll(hsValidation.getErrors());
                    }
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证热点数据
     *
     * @param hotspots 热点列表
     * @param objectIndex 多媒体对象索引
     * @return 验证结果
     */
    public static ValidationResult validateHotspots(List<HotspotItemVO> hotspots, int objectIndex) {
        List<String> errors = new ArrayList<>();

        for (int j = 0; j < hotspots.size(); j++) {
            HotspotItemVO hs = hotspots.get(j);

            if (hs.getApsid() == null || hs.getApsid().trim().isEmpty()) {
                errors.add("第" + (objectIndex + 1) + "个对象的第" + (j + 1) + "个热点的应用结构标识(apsid)不能为空");
            }

            if (hs.getName() == null || hs.getName().trim().isEmpty()) {
                errors.add("第" + (objectIndex + 1) + "个对象的第" + (j + 1) + "个热点的名称不能为空");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
