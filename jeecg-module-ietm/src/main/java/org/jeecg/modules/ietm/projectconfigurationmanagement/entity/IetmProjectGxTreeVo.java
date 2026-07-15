package org.jeecg.modules.ietm.projectconfigurationmanagement.entity;

import lombok.Data;

import java.util.List;

/**
 * 项目构型树数据-授权页面用
 */
@Data
public class IetmProjectGxTreeVo {
    private String id;
    private String key;
    private String value;
    private String title;
    private String parentId;
    private List<IetmProjectGxTreeVo> children;

    public IetmProjectGxTreeVo() {
    }

    public IetmProjectGxTreeVo(String id, String key, String value, String title, List<IetmProjectGxTreeVo> children) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.title = title;
        this.children = children;
    }
}
