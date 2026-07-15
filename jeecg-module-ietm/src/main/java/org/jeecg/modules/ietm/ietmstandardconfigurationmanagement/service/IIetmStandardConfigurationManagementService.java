package org.jeecg.modules.ietm.ietmstandardconfigurationmanagement.service;

import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.ietm.ietmstandardconfigurationmanagement.entity.IetmStandardConfigurationManagement;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.exception.JeecgBootException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;

/**
 * @Description: 预制模板-构型管理
 * @Author: jeecg-boot
 * @Date:   2026-01-07
 * @Version: V1.0
 */
public interface IIetmStandardConfigurationManagementService extends IService<IetmStandardConfigurationManagement> {

	/**根节点父ID的值*/
	public static final String ROOT_PID_VALUE = "0";

	/**树节点有子节点状态值*/
	public static final String HASCHILD = "1";

	/**树节点无子节点状态值*/
	public static final String NOCHILD = "0";

	/**
	 * 新增节点
	 *
	 * @param ietmStandardConfigurationManagement
	 */
	void addIetmStandardConfigurationManagement(IetmStandardConfigurationManagement ietmStandardConfigurationManagement);

	/**
   * 修改节点
   *
   * @param ietmStandardConfigurationManagement
   * @throws JeecgBootException
   */
	void updateIetmStandardConfigurationManagement(IetmStandardConfigurationManagement ietmStandardConfigurationManagement) throws JeecgBootException;

	/**
	 * 删除节点
	 *
	 * @param id
   * @throws JeecgBootException
	 */
	void deleteIetmStandardConfigurationManagement(String id) throws JeecgBootException;

	  /**
	   * 查询所有数据，无分页
	   *
	   * @param queryWrapper
	   * @return List<IetmStandardConfigurationManagement>
	   */
    List<IetmStandardConfigurationManagement> queryTreeListNoPage(QueryWrapper<IetmStandardConfigurationManagement> queryWrapper);

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
	 * 显示路径
	 * @param list
	 */
    void setWay(List<IetmStandardConfigurationManagement> list);

	Integer getCodeTemp(String pid);
}
