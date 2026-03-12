package com.qingqingketang.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
        "com.qingqingketang.student.mapper"
})
public class MybatisPlusConfig {
    // Additional MyBatis-Plus configuration can be added here as needed.
}
