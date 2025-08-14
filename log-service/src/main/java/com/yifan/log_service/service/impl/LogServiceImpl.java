package com.yifan.log_service.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yifan.log_service.entity.LogEntry;
import com.yifan.log_service.mapper.LogMapper;
import com.yifan.log_service.service.LogService;


@Service
public class LogServiceImpl implements LogService {
    final LogMapper logMapper;

    public LogServiceImpl(LogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Override
    public void saveLog(LogEntry logEntry) {
        logMapper.insert(logEntry);
    }

    @Override
    public List<LogEntry> findLogs(String tenantId, String serviceName, String level, String userId) {
        QueryWrapper<LogEntry> wrapper = new QueryWrapper<>();
        if (tenantId != null)
            wrapper.eq("tenant_id", tenantId);
        if (serviceName != null)
            wrapper.eq("service_name", serviceName);
        if (level != null)
            wrapper.eq("level", level);
        if (userId != null)
            wrapper.eq("user_id", userId);
        return logMapper.selectList(wrapper);
    }

}
