package org.jeecg.modules.ietm.test.service.impl;

import org.jeecg.modules.ietm.test.entity.TestAgency;
import org.jeecg.modules.ietm.test.mapper.TestAgencyMapper;
import org.jeecg.modules.ietm.test.service.ITestAgencyService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

@Service
public class TestAgencyServiceImpl extends ServiceImpl<TestAgencyMapper, TestAgency> implements ITestAgencyService {
}