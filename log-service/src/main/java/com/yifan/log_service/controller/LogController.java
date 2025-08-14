package com.yifan.log_service.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yifan.log_service.entity.LogEntry;
import com.yifan.log_service.service.LogService;

@RestController
@RequestMapping("/logs")
public class LogController {
    final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    // Define endpoints for logging operations here
    // 其他服务发送日志
    @PostMapping
    public void receiveLog(@RequestBody LogEntry logEntry) {
        logService.saveLog(logEntry);
    }

    // 查询日志
    @GetMapping
    public List<LogEntry> getLogs(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String userId) {
        return logService.findLogs(tenantId, serviceName, level, userId);
    }
}
