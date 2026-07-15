package org.jeecg.modules.ietm.projectmanagement.service;

import org.jeecg.modules.ietm.projectmanagement.entity.IetmProjectParams;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @Description: 项目管理-项目参数
 * @Author: jeecg-boot
 * @Date:   2026-01-09
 * @Version: V1.0
 */
public interface IIetmProjectParamsService extends IService<IetmProjectParams> {

	/**
	 * 通过主表id查询子表数据
	 *
	 * @param mainId 主表id
	 * @return List<IetmProjectParams>
	 */
	public List<IetmProjectParams> selectByMainId(String mainId);
}
