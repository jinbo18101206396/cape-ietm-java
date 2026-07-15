package org.jeecg.modules.ietm.standardmanagement.service;

import org.jeecg.modules.ietm.standardmanagement.entity.IetmStandardDmtype;
import org.jeecg.modules.ietm.standardmanagement.entity.IetmStandard;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 手册管理-标准管理左侧树
 * @Author: jeecg-boot
 * @Date:   2026-01-08
 * @Version: V1.0
 */
public interface IIetmStandardService extends IService<IetmStandard> {

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
