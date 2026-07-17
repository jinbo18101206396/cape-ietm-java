package org.jeecg.modules.ietm.projectconfigurationmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;

import java.util.List;
import java.util.Map;

/**
 * @Description: 项目管理-项目构型管理
 * @Author: jeecg-boot
 * @Date:   2026-02-10
 * @Version: V1.0
 */
public interface IetmProjectConfigurationManagementMapper extends BaseMapper<IetmProjectConfigurationManagement> {

	/**
	 * 编辑节点状态
	 * @param id
	 * @param status
	 */
	void updateTreeNodeStatus(@Param("id") String id,@Param("status") String status);

	/**
	 * 【vue3专用】根据父级ID查询树节点数据
	 *
	 * @param pid
	 * @param query
	 * @return
	 */
	List<SelectTreeModel> queryListByPid(@Param("pid") String pid, @Param("query") Map<String, String> query);

	/**
	 * 根据项目ID查询项目信息
	 * @param projectId
	 * @return
	 */
	@Select("SELECT * FROM ietm_project WHERE id = #{projectId}")
	org.jeecg.modules.ietm.projectmanagement.entity.IetmProject selectProjectById(@Param("projectId") String projectId);

	/**
	 * 检查DM引用数量
	 * @param cmNodeId
	 * @return
	 */
	int countDmReference(@Param("cmNodeId") String cmNodeId);

	/**
	 * 检查ICN引用数量
	 * @param cmNodeId
	 * @return
	 */
	int countIcnReference(@Param("cmNodeId") String cmNodeId);

}
