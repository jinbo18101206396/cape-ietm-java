package org.jeecg.modules.ietm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试启动类
 *
 * 用于 @SpringBootTest 集成测试的最小化 Spring Boot 应用
 *
 * @author Claude
 * @since 2026-08-31
 */
@SpringBootApplication(scanBasePackages = {"org.jeecg"})
@MapperScan(basePackages = {"org.jeecg.**.mapper"})
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
