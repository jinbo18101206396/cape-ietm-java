package org.jeecg.modules.ietm.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.ietm.workflow.entity.WfInstance;
import org.jeecg.modules.ietm.workflow.vo.TodoItemVO;

import java.util.List;
import java.util.Map;

/**
 * @Description: 工作流实例Mapper接口
 * @Author: jeecg-boot
 * @Date: 2026-07-25
 * @Version: V1.0
 */
public interface WfInstanceMapper extends BaseMapper<WfInstance> {

    /**
     * 根据formid查询流转中的工作流实例
     * @param formid 业务表单ID（ietm_data_module.id）
     * @return 流转中的实例（status_='1'）
     */
    WfInstance selectActiveByFormid(@Param("formid") String formid);

    /**
     * 根据批次ID查询工作流实例列表
     * @param batchId 批次ID
     * @return 实例列表
     */
    List<WfInstance> selectByBatchId(@Param("batchId") String batchId);

    /**
     * 批量终止工作流实例（将status_改为9）
     * @param instanceIds 实例ID列表
     * @param updateBy 更新人
     * @return 影响行数
     */
    int batchTerminate(@Param("instanceIds") List<String> instanceIds, @Param("updateBy") String updateBy);

    /**
     * 批量插入工作流实例（一次SQL插入多条，替代循环insert）
     * <p>调用方需提前生成id（IdWorker），本方法不依赖MyBatis-Plus的主键回填与自动填充。</p>
     * @param list 实例列表（id/createBy/createTime 需已赋值）
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<WfInstance> list);

    /**
     * 签出时迁移工作流实例的formid关联
     * 将活动的工作流实例从旧版本DM关联到新版本DM
     *
     * @param oldFormid 旧版本DM的id
     * @param newFormid 新版本DM的id
     * @param updateBy 更新人
     * @return 影响行数
     */
    int migrateFormid(@Param("oldFormid") String oldFormid,
                      @Param("newFormid") String newFormid,
                      @Param("updateBy") String updateBy);

    /**
     * 查询我的待办列表（v1.3权限匹配：前缀编码模式）
     *
     * @param params 查询参数
     *               - userId: 当前用户username
     *               - projectId: 项目ID（参数化查询）
     *               - searchField: 搜索字段（可选）
     *               - searchValue: 搜索值（可选）
     * @return 待办列表
     */
    List<TodoItemVO> getMyTodoList(@Param("params") Map<String, Object> params);
}
