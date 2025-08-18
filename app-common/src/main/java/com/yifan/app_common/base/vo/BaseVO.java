package com.yifan.app_common.base.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BaseVO {

    private Long id;

    private Long createdBy;

    private String createdByName;

    private LocalDateTime createTime;

    private Long updatedBy;

    private String updatedByName;

    private LocalDateTime updatedTime;

    private Integer isDeleted;
}
