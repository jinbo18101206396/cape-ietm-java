package org.jeecg.modules.ietm.standardmanagement.service;

import org.jeecg.modules.ietm.standardmanagement.entity.IetmStandardDmtype;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * @Description: 手册管理-标准管理列表（标准数据模块）
 * @Author: jeecg-boot
 * @Date:   2026-01-08
 * @Version: V1.0
 */
public interface IIetmStandardDmtypeService extends IService<IetmStandardDmtype> {

  /**
   * 通过主表id查询子表数据
   *
   * @param mainId
   * @return List<IetmStandardDmtype>
   */
	public List<IetmStandardDmtype> selectByMainId(String mainId);
}
