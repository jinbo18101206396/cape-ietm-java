package org.jeecg.modules.ietm.projectmanagement.service;

import org.jeecg.modules.ietm.projectmanagement.entity.IetmProjectParams;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProject;
import com.baomidou.mybatisplus.extension.service.IService;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 手册管理-手册项目管理列表
 * @Author: jeecg-boot
 * @Date:   2026-01-09
 * @Version: V1.0
 */
public interface IIetmProjectService extends IService<IetmProject> {

	/**
	 * 添加一对多
	 *
	 * @param ietmProject
	 * @param ietmProjectParam
	 */
	public void saveMain(IetmProject ietmProject, IetmProjectParams ietmProjectParam) ;

	/**
	 * 修改一对多
	 *
   * @param ietmProject
   * @param ietmProjectParam
	 */
	public void updateMain(IetmProject ietmProject);

	/**
	 * 删除一对多
	 *
	 * @param id
	 */
	public void delMain (String id);

	/**
	 * 批量删除一对多
	 *
	 * @param idList
	 */
	public void delBatchMain (Collection<? extends Serializable> idList);

}
