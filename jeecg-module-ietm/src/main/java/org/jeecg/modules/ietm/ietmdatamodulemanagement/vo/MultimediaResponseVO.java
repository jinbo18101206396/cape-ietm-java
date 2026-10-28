package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import lombok.Data;
import java.util.List;

/**
 * Multimedia响应VO
 * 用于：后端 → 前端（getMultimediaContent接口返回）
 */
@Data
public class MultimediaResponseVO {

    /**
     * 标题（必填）
     */
    private String title;

    /**
     * ID属性（可选）
     */
    private String id;

    /**
     * 多媒体对象列表
     */
    private List<MultimediaObjectItemVO> multimediaObjects;
}
