package org.jeecg.modules.ietm.test.service.impl;

import org.jeecg.modules.ietm.test.entity.TestAgencyFile;
import org.jeecg.modules.ietm.test.mapper.TestAgencyFileMapper;
import org.jeecg.modules.ietm.test.service.ITestAgencyFileService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

@Service
public class TestAgencyFileServiceImpl extends ServiceImpl<TestAgencyFileMapper, TestAgencyFile> implements ITestAgencyFileService {
}