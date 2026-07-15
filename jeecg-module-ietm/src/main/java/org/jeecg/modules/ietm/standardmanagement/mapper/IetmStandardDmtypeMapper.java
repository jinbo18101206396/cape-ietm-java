package org.jeecg.modules.ietm.standardmanagement.mapper;

import java.util.List;
import org.jeecg.modules.ietm.standardmanagement.entity.IetmStandardDmtype;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: 手册管理-标准管理列表（标准数据模块）
 * @Author: jeecg-boot
 * @Date:   2026-01-08
 * @Version: V1.0
 */
public interface IetmStandardDmtypeMapper extends BaseMapper<IetmStandardDmtype> {

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
    * @return List<IetmStandardDmtype>
    */
	public List<IetmStandardDmtype> selectByMainId(@Param("mainId") String mainId);

	/**
	 * 根据标准名称查询数据模块类型
	 *
	 * @param standard 标准名称
	 * @return List<IetmStandardDmtype>
	 */
	public List<IetmStandardDmtype> selectByStandard(@Param("standard") String standard);

}
