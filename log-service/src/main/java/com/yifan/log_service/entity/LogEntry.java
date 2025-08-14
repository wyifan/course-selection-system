package com.yifan.log_service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("log_entries")
public class LogEntry {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId; // 多租户标识
    private String traceId; // 分布式追踪ID
    private String serviceName; // 来源服务
    private String level; // 日志级别 (INFO, ERROR 等)
    private String message; // 日志内容
    private String timestamp; // 时间戳
    private String userId; // 可选：关联用户
}
