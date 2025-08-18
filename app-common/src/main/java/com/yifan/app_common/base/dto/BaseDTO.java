package com.yifan.app_common.base.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Created by: yifan
 * Created on: 2025-08-18 23:20:52
 * Description: Base DTO class 
 *  
 */ 
@Data
public class BaseDTO {
    //JsonSerialize(using = ToStringSerializer.class) // 只针对此字段
    private Long id;

    private Long createdBy;

    private String createdByName;

    private LocalDateTime createTime;

    private Long updatedBy;

    private String updatedByName;

    private LocalDateTime updatedTime;

    private Integer isDeleted;
}

