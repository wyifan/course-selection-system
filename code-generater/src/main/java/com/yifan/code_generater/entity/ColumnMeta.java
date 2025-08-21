package com.yifan.code_generater.entity;

import lombok.Data;

@Data
public class ColumnMeta {
    private String columnName;
    private String javaName;
    private String javaType;
    private String sqlType;
    private boolean primaryKey;
    private String comment;
}
