package org.jeecg.modules.ietm.ietmdatamodulemanagement.vo;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * DM预览配置VO
 *
 * @author IETM Team
 * @date 2026-09-18
 */
@Data
@ApiModel(value = "DM预览配置", description = "用户自定义预览外观和行为配置")
public class PreviewConfigVO {

    // ========== 视觉配置 ==========

    @ApiModelProperty(value = "字体大小", example = "medium", allowableValues = "small,medium,large,xlarge")
    private String fontSize = "medium";

    @ApiModelProperty(value = "颜色主题", example = "light", allowableValues = "light,dark,high-contrast")
    private String theme = "light";

    @ApiModelProperty(value = "行间距（百分比）", example = "150")
    private Integer lineHeight = 150;

    @ApiModelProperty(value = "页面宽度", example = "auto", allowableValues = "auto,fixed-800,fixed-1000,fixed-1200")
    private String pageWidth = "auto";

    // ========== 显示配置 ==========

    @ApiModelProperty(value = "显示DMC编码", example = "true")
    private Boolean showDmCode = true;

    @ApiModelProperty(value = "显示修订标记", example = "true")
    private Boolean showRevision = true;

    @ApiModelProperty(value = "显示安全警告图标", example = "true")
    private Boolean showSafetyIcons = true;

    @ApiModelProperty(value = "图形缩放比例（百分比）", example = "100")
    private Integer graphicScale = 100;

    // ========== 打印配置 ==========

    @ApiModelProperty(value = "打印模式", example = "false")
    private Boolean printMode = false;

    @ApiModelProperty(value = "显示页眉", example = "true")
    private Boolean showHeader = true;

    @ApiModelProperty(value = "显示页脚", example = "true")
    private Boolean showFooter = true;

    @ApiModelProperty(value = "分页符处理", example = "auto", allowableValues = "auto,avoid,always")
    private String pageBreak = "auto";

    /**
     * 创建默认配置
     */
    public static PreviewConfigVO createDefault() {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setFontSize("medium");
        config.setTheme("light");
        config.setLineHeight(150);
        config.setPageWidth("auto");
        config.setShowDmCode(true);
        config.setShowRevision(true);
        config.setShowSafetyIcons(true);
        config.setGraphicScale(100);
        config.setPrintMode(false);
        config.setShowHeader(true);
        config.setShowFooter(true);
        config.setPageBreak("auto");
        return config;
    }

    /**
     * 验证配置有效性
     */
    public void validate() {
        if (lineHeight != null && (lineHeight < 100 || lineHeight > 300)) {
            throw new IllegalArgumentException("行间距必须在100-300之间");
        }
        if (graphicScale != null && (graphicScale < 50 || graphicScale > 200)) {
            throw new IllegalArgumentException("图形缩放比例必须在50-200之间");
        }
    }
}
