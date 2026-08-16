package org.jeecg.modules.ietm.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.workflow.entity.WfTemplate;

import java.util.List;

/**
 * @Description: 工作流模板Mapper接口
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
public interface WfTemplateMapper extends BaseMapper<WfTemplate> {

    /**
     * 查询已发布的模板列表
     * @param tmpltype 模板类型（可选）
     * @return 模板列表
     */
    List<WfTemplate> selectPublishedTemplates(@Param("tmpltype") String tmpltype);
}
