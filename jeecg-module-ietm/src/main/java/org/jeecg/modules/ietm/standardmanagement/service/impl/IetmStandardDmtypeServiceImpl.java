package org.jeecg.modules.ietm.standardmanagement.service.impl;

import org.jeecg.modules.ietm.standardmanagement.entity.IetmStandardDmtype;
import org.jeecg.modules.ietm.standardmanagement.mapper.IetmStandardDmtypeMapper;
import org.jeecg.modules.ietm.standardmanagement.service.IIetmStandardDmtypeService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description: 手册管理-标准管理列表（标准数据模块）
 * @Author: jeecg-boot
 * @Date:   2026-01-08
 * @Version: V1.0
 */
@Service
@Primary
public class IetmStandardDmtypeServiceImpl extends ServiceImpl<IetmStandardDmtypeMapper, IetmStandardDmtype> implements IIetmStandardDmtypeService {
	
	@Autowired
	private IetmStandardDmtypeMapper ietmStandardDmtypeMapper;
	
	@Override
	public List<IetmStandardDmtype> selectByMainId(String mainId) {
		return ietmStandardDmtypeMapper.selectByMainId(mainId);
	}
}
