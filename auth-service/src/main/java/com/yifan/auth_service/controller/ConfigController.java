package com.yifan.auth_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.yifan.auth_service.service.ConfigService;

@RestController
@RequestMapping("/config")          
public class ConfigController {

    final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/current")
    public String getCurrentConfig() {
        return configService.getCurrentValue();
    }

    @GetMapping("/config")
    public DeferredResult<String> getConfig(@RequestParam String currentValue) {
        DeferredResult<String> deferredResult = new DeferredResult<>(60000L); // 超时 60 秒

        String latestValue = configService.getCurrentValue();
        if (!latestValue.equals(currentValue)) {
            // 如果已变化，立即返回
            deferredResult.setResult(latestValue);
        } else {
            // 无变化，挂起请求
            configService.addPendingRequest(deferredResult);
        }

        // 超时返回 "NO_CHANGE"，客户端可重试
        deferredResult.onTimeout(() -> deferredResult.setResult("NO_CHANGE"));

        return deferredResult;
    }

    @PostMapping("/update")
    public String updateConfig(@RequestParam String newValue) {
        configService.updateValue(newValue);
        return "Config updated to: " + newValue;
    }

    @GetMapping("/long-polling")
    public DeferredResult<String> getLongPollingConfig() {
        DeferredResult<String> deferredResult = new DeferredResult<>(5000L, "Timeout: No new config value"); // 5秒超时
        configService.addPendingRequest(deferredResult);
        return deferredResult;
    }


}
