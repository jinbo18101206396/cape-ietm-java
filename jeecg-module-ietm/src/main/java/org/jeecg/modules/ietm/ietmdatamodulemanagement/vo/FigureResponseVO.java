package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * Figure响应VO
 *
 * 用于返回Figure数据结构给前端
 */
@Data
public class FigureResponseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Figure标题（必填）
     */
    private String title;

    /**
     * Figure ID（可选）
     */
    private String id;

    /**
     * 图形列表
     */
    private List<GraphicItemVO> graphics;

    /**
     * 图例列表
     */
    private List<LegendItemVO> legends;
}
