package com.yifan.app_common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yifan.app_common.handler.BaseMetaObjectHandler;
import com.yifan.app_common.interceptor.SqlDebugInterceptor;

/**
 * MyBatis Plus
 */
@Configuration
public class MybatisPlusAutoConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.mybatis-plus.auto-fill", name = "enabled", havingValue = "false", matchIfMissing = false)
    public BaseMetaObjectHandler baseMetaObjectHandler() {
        return new BaseMetaObjectHandler();
    }

    @Bean
    @ConditionalOnProperty(name = "app.mybatis-plus.sql-debug", havingValue = "true", matchIfMissing = false)
    public SqlDebugInterceptor sqlDebugInterceptor() {
        return new SqlDebugInterceptor();
    }
}
