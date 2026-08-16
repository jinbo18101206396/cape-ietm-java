package org.jeecg.modules.ietm.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.workflow.entity.WfTemplateDtl;

import java.util.List;

/**
 * @Description: 工作流模板节点Mapper接口
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
public interface WfTemplateDtlMapper extends BaseMapper<WfTemplateDtl> {

    /**
     * 根据模板ID查询所有节点，按seqno排序
     * @param templateId 模板ID
     * @return 节点列表
     */
    List<WfTemplateDtl> selectByTemplateIdOrderBySeqno(@Param("templateId") String templateId);
}
