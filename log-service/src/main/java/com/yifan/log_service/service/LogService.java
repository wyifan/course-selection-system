package com.yifan.log_service.service;

import java.util.List;

import com.yifan.log_service.entity.LogEntry;

public interface LogService {
 void saveLog(LogEntry logEntry);

 List<LogEntry> findLogs(String tenantId, String serviceName, String level, String userId);
}
