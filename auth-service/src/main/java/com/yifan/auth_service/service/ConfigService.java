package com.yifan.auth_service.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import com.yifan.auth_service.entity.ConfigEntity;
import com.yifan.auth_service.mapper.ConfigMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConfigService {

    final ConfigMapper configMapper;

    public ConfigService(ConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    private final List<DeferredResult<String>> pendingRequests = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        // 初始化数据库记录，如果不存在
        Optional.ofNullable(configMapper.selectById(1L))
                .orElseGet(() -> {
                    ConfigEntity entity = new ConfigEntity();
                    entity.setId(1L);
                    entity.setConfigKey("apollo.config.value");
                    entity.setConfigValue("default");
                    entity.setDescription("Configuration value for demonstration");
                    configMapper.insert(entity);
                    log.info("Initialized config record in database.");

                    return entity;
                });
    }

    public String getCurrentValue() {
        return Optional.ofNullable(configMapper.selectById(1L))
                .map(ConfigEntity::getConfigValue)
                .orElse("default");
    }

    public void addPendingRequest(DeferredResult<String> deferredResult) {
        pendingRequests.add(deferredResult);
        // 当完成或超时时，从列表移除
        deferredResult.onCompletion(() -> pendingRequests.remove(deferredResult));
        deferredResult.onTimeout(() -> pendingRequests.remove(deferredResult));
    }

    public void updateValue(String newValue) {
        ConfigEntity entity = configMapper.selectById(1L);
        entity.setConfigValue(newValue);
        configMapper.updateById(entity);

        log.info("Configuration value updated to: {}", newValue);

        String currentValue = getCurrentValue();
        // 通知所有挂起的请求
        for (DeferredResult<String> dr : pendingRequests) {
            dr.setResult(currentValue);
        }
        pendingRequests.clear();
    }
}
