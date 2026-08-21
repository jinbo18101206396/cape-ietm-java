package org.jeecg;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"org.jeecg"})
public class TestApplication {
    // 测试专用配置类，用于扫描所有测试需要的Bean
}
