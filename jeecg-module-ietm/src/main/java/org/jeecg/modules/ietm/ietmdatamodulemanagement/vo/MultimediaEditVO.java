package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import lombok.Data;

/**
 * Multimedia编辑请求VO
 * 用于：前端 → 后端（saveMultimediaContent接口入参）
 */
@Data
public class MultimediaEditVO {

    /**
     * DM文件ID
     */
    private String dmId;

    /**
     * multimedia所在起始行号
     */
    private Integer lineno;

    /**
     * multimedia所在结束行号
     */
    private Integer endline;

    /**
     * 语言环境（en | cn）
     */
    private String locale;

    /**
     * Multimedia数据对象
     */
    private MultimediaResponseVO multimediaData;
}
