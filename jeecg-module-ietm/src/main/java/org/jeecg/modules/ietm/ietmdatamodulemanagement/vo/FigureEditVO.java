package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * Figure编辑请求VO
 *
 * 前端提交的编辑数据，包含审计追踪字段
 */
@Data
public class FigureEditVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * DM文件ID（必填）
     */
    private String dmId;

    /**
     * figure所在行号（必填）
     */
    private Integer lineno;

    /**
     * figure结束行号（必填）
     */
    private Integer endline;

    /**
     * 语言（cn/en）
     */
    private String locale;

    /**
     * Figure数据
     */
    private FigureResponseVO figureData;

    /**
     * Figure ID（用于更新时定位figure）
     */
    private String figureId;

    /**
     * 操作者（审计追踪）
     */
    private String operator;
}
