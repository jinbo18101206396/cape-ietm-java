package org.jeecg.modules.ietm.projectmanagement.service.impl;

import org.jeecg.modules.ietm.projectmanagement.entity.IetmProjectParams;
import org.jeecg.modules.ietm.projectmanagement.mapper.IetmProjectParamsMapper;
import org.jeecg.modules.ietm.projectmanagement.service.IIetmProjectParamsService;
import org.springframework.stereotype.Service;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description: 项目管理-项目参数
 * @Author: jeecg-boot
 * @Date:   2026-01-09
 * @Version: V1.0
 */
@Service
public class IetmProjectParamsServiceImpl extends ServiceImpl<IetmProjectParamsMapper, IetmProjectParams> implements IIetmProjectParamsService {

	@Autowired
	private IetmProjectParamsMapper ietmProjectParamsMapper;

	@Override
	public List<IetmProjectParams> selectByMainId(String mainId) {
		return ietmProjectParamsMapper.selectByMainId(mainId);
	}
}
