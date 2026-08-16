package org.jeecg.modules.ietm.icnmanage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: ICN引用关系信息VO
 * @Author: jeecg-boot
 * @Date: 2026-07-21
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "ReferenceInfoVO", description = "ICN引用关系信息VO")
public class ReferenceInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "当前ICN ID")
    private String icnId;

    @ApiModelProperty(value = "当前ICN编码")
    private String icn;

    @ApiModelProperty(value = "当前ICN文件名")
    private String fileName;

    @ApiModelProperty(value = "引用的其他ICN列表（正向引用）")
    private List<IcnReferenceItem> referencedIcnList;

    @ApiModelProperty(value = "被其他ICN引用列表（反向引用）")
    private List<IcnReferenceItem> referencingIcnList;

    @ApiModelProperty(value = "被DM模块引用列表")
    private List<DmReferenceItem> dmReferenceList;

    @ApiModelProperty(value = "引用统计信息")
    private ReferenceStatistics statistics;

    /**
     * ICN引用项
     */
    @Data
    @Accessors(chain = true)
    @ApiModel(value = "IcnReferenceItem", description = "ICN引用项")
    public static class IcnReferenceItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "ICN ID")
        private String id;

        @ApiModelProperty(value = "ICN编码")
        private String icn;

        @ApiModelProperty(value = "文件名称")
        private String fileName;

        @ApiModelProperty(value = "文件类型")
        private String fileType;

        @ApiModelProperty(value = "版本号")
        private String issueNo;

        @ApiModelProperty(value = "引用类型")
        private String referenceType;

        @ApiModelProperty(value = "创建时间")
        private String createTime;

        @ApiModelProperty(value = "备注")
        private String remark;
    }

    /**
     * DM引用项
     */
    @Data
    @Accessors(chain = true)
    @ApiModel(value = "DmReferenceItem", description = "DM引用项")
    public static class DmReferenceItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "DM ID")
        private String id;

        @ApiModelProperty(value = "DM编码")
        private String dmCode;

        @ApiModelProperty(value = "DM标题")
        private String dmTitle;

        @ApiModelProperty(value = "DM类型")
        private String dmType;

        @ApiModelProperty(value = "版本号")
        private String issueNo;

        @ApiModelProperty(value = "引用位置")
        private String referencePosition;

        @ApiModelProperty(value = "创建时间")
        private String createTime;

        @ApiModelProperty(value = "备注")
        private String remark;
    }

    /**
     * 引用统计信息
     */
    @Data
    @Accessors(chain = true)
    @ApiModel(value = "ReferenceStatistics", description = "引用统计信息")
    public static class ReferenceStatistics implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "引用的ICN数量")
        private Integer referencedIcnCount;

        @ApiModelProperty(value = "被引用的ICN数量")
        private Integer referencingIcnCount;

        @ApiModelProperty(value = "被DM引用数量")
        private Integer dmReferenceCount;

        @ApiModelProperty(value = "总引用关系数量")
        private Integer totalReferenceCount;
    }
}
