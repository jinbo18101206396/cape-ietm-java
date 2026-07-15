package org.jeecg.modules.ietm.projectconfigurationmanagement.service;

import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.common.system.vo.TreeModel;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.exception.JeecgBootException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectGxTreeVo;

import java.util.List;

/**
 * @Description: 项目管理-项目构型管理
 * @Author: jeecg-boot
 * @Date:   2026-02-10
 * @Version: V1.0
 */
public interface IIetmProjectConfigurationManagementService extends IService<IetmProjectConfigurationManagement> {

	/**根节点父ID的值*/
	public static final String ROOT_PID_VALUE = "0";

	/**树节点有子节点状态值*/
	public static final String HASCHILD = "1";

	/**树节点无子节点状态值*/
	public static final String NOCHILD = "0";

	/**
	 * 新增节点
	 *
	 * @param ietmProjectConfigurationManagement
	 */
	void addIetmProjectConfigurationManagement(IetmProjectConfigurationManagement ietmProjectConfigurationManagement);

	/**
   * 修改节点
   *
   * @param ietmProjectConfigurationManagement
   * @throws JeecgBootException
   */
	void updateIetmProjectConfigurationManagement(IetmProjectConfigurationManagement ietmProjectConfigurationManagement) throws JeecgBootException;

	/**
	 * 删除节点
	 *
	 * @param id
   * @throws JeecgBootException
	 */
	void deleteIetmProjectConfigurationManagement(String id) throws JeecgBootException;

	  /**
	   * 查询所有数据，无分页
	   *
	   * @param queryWrapper
	   * @return List<IetmProjectConfigurationManagement>
	   */
    List<IetmProjectConfigurationManagement> queryTreeListNoPage(QueryWrapper<IetmProjectConfigurationManagement> queryWrapper);

	/**
	 * 【vue3专用】根据父级编码加载分类字典的数据
	 *
	 * @param parentCode
	 * @return
	 */
	List<SelectTreeModel> queryListByCode(String parentCode);

	/**
	 * 【vue3专用】根据pid查询子节点集合
	 *
	 * @param pid
	 * @return
	 */
	List<SelectTreeModel> queryListByPid(String pid);

	/**
	 * 查询所有数据，无分页
	 *
	 * @return List<IetmProjectConfigurationManagement>
	 */
	List<TreeModel> queryTreeList();

    List<IetmProjectGxTreeVo> buildTree(List<IetmProjectConfigurationManagement> list, String projectId);

}