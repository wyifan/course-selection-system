package com.yifan.app_common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yifan.app_common.handler.BaseMetaObjectHandler;

/**
 * MyBatis Plus
 */
@Configuration
@ConditionalOnProperty(prefix = "mybatis-plus.auto-fill", name = "enabled", havingValue = "false", matchIfMissing = false)
public class MybatisPlusAutoConfig {

    @Bean
    @ConditionalOnMissingBean(BaseMetaObjectHandler.class)
    public BaseMetaObjectHandler baseMetaObjectHandler() {
        return new BaseMetaObjectHandler();
    }
}
