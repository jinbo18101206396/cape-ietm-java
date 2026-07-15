package org.jeecg.modules.ietm.test.service.impl;

import org.jeecg.modules.ietm.test.entity.TestProject;
import org.jeecg.modules.ietm.test.mapper.TestProjectMapper;
import org.jeecg.modules.ietm.test.service.ITestProjectService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

@Service
public class TestProjectServiceImpl extends ServiceImpl<TestProjectMapper, TestProject> implements ITestProjectService {
}