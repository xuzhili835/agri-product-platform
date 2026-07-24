package com.agri.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.agri.platform.config.TurnstileProperties;
import com.agri.platform.config.AlipayProperties;

/**
 * 农产品融销一体平台 - 主启动类
 */
@SpringBootApplication
@MapperScan("com.agri.platform.mapper")
@EnableConfigurationProperties({TurnstileProperties.class, AlipayProperties.class})
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("农产品融销一体平台 - 后端启动成功！");
        System.out.println("访问地址: http://localhost:8080/api");
        System.out.println("========================================\n");
    }
}
