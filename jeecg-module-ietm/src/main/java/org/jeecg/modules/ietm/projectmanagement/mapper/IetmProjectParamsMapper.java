package org.jeecg.modules.ietm.projectmanagement.mapper;

import java.util.List;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProjectParams;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: 项目管理-项目参数
 * @Author: jeecg-boot
 * @Date:   2026-01-09
 * @Version: V1.0
 */
public interface IetmProjectParamsMapper extends BaseMapper<IetmProjectParams> {

	/**
	 * 通过主表id删除子表数据
	 *
	 * @param mainId 主表id
	 * @return boolean
	 */
	public boolean deleteByMainId(@Param("mainId") String mainId);

  /**
   * 通过主表id查询子表数据
   *
   * @param mainId 主表id
   * @return List<IetmProjectParams>
   */
	public List<IetmProjectParams> selectByMainId(@Param("mainId") String mainId);
}
