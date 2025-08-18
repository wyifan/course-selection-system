package com.yifan.auth_service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("config")
public class ConfigEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
}
