package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import lombok.Data;
import java.util.List;

/**
 * Multimedia对象项VO
 * 对应XML中的<multimediaObject>元素
 */
@Data
public class MultimediaObjectItemVO {

    /**
     * ICN编码（对应infoEntityIdent属性）
     */
    private String icn;

    /**
     * 文件名（从ICN查询获得，不在XML中）
     */
    private String filename;

    /**
     * 多媒体类型（video | 3D）
     */
    private String multimediaType;

    /**
     * 热点列表
     */
    private List<HotspotItemVO> hotspots;
}
