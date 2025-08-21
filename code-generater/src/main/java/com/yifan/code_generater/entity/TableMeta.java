package com.yifan.code_generater.entity;

import java.util.List;

import lombok.Data;

@Data
public class TableMeta {
    private String tableName;
    private String entityName;
    private List<ColumnMeta> columns;
    private boolean useBaseEntity;
}
