package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 图形项VO
 *
 * 表示figure中的单个graphic元素
 */
@Data
public class GraphicItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ICN编号（必填）
     */
    private String icn;

    /**
     * 文件名（延迟加载）
     */
    private String filename;

    /**
     * 热点列表
     */
    private List<HotspotItemVO> hotspots;
}
