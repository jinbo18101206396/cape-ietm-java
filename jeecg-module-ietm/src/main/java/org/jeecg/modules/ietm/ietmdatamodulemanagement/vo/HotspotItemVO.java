package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 热点项VO
 *
 * 表示图形中的单个热点
 */
@Data
public class HotspotItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 应用结构标识（必填）
     */
    private String apsid;

    /**
     * 热点名称（必填）
     */
    private String name;

    /**
     * 热点ID（可选）
     */
    private String id;
}
