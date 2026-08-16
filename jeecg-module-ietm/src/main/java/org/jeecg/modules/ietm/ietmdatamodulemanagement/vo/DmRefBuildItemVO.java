package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import lombok.Data;

/** 批量生成 dmRef 的单条入参（§14.5.4） */
@Data
public class DmRefBuildItemVO {
    /** 目标DM的主键ID */
    private String dmId;
    /** 是否包含版本信息（true=生成 issueInfo+issueDate；默认false=不生成） */
    private Boolean includeVersion;
    /** 内部引用片段id（整体引用时为null/空；内部引用时取#refcombo 选中值） */
    private String referredFragment;
}
