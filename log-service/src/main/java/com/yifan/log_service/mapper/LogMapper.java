package com.yifan.log_service.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yifan.log_service.entity.LogEntry;

@Mapper
public interface LogMapper extends BaseMapper<LogEntry> {

}