package org.jeecg.common.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author: zxk
 * @description: TODO
 * @date: 2026/2/9 15:28
 * @param:
 * @return:
 * @version: 1.0
 */
@Data
public class TreeModel implements Serializable {
    /**
     * key
     */
    private String key;
    /**
     * title
     */
    private String pid;
    /**
     * title
     */
    private String title;
    /**
     * 子节点
     */
    private List<TreeModel> children;

}

