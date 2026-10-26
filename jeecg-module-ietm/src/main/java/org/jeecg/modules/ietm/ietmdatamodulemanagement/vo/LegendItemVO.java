package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 图例项VO
 *
 * 表示figure中的单个图例项
 */
@Data
public class LegendItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图例编号（必填）
     */
    private String no;

    /**
     * 图例名称（必填）
     */
    private String name;
}
