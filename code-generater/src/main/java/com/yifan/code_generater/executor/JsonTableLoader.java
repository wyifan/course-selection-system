package com.yifan.code_generater.executor;

import com.yifan.code_generater.config.ConfigFromYml;
import com.yifan.code_generater.entity.ColumnMeta;
import com.yifan.code_generater.entity.TableMeta;

import java.util.stream.Collectors;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.InputStream;

public class JsonTableLoader {
    public static List<TableMeta> loadFromJson(String jsonFilePath, ConfigFromYml config) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonWrapper wrapper;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream(jsonFilePath)) {
            if (is == null) {
                throw new RuntimeException("找不到配置文件 table-definitions.json");
            }
            wrapper = mapper.readValue(is, JsonWrapper.class);
        }
        // JsonWrapper wrapper = mapper.readValue(new File(jsonFilePath),
        // JsonWrapper.class);

        return wrapper.getTables().stream().map(t -> {
            TableMeta table = new TableMeta();
            table.setTableName(t.getTableName());
            table.setEntityName(t.getEntityName());
            table.setUseBaseEntity(true);

            // 转换字段
            List<ColumnMeta> cols = t.getColumns().stream().map(c -> {
                ColumnMeta col = new ColumnMeta();
                col.setJavaName(c.getJavaName());
                col.setColumnName(toSnakeCase(c.getJavaName()));
                col.setJavaType(c.getJavaType());
                col.setSqlType(config.mapJavaToSql(c.getJavaType())); // 根据全局配置映射
                col.setComment(c.getComment());
                return col;
            }).collect(Collectors.toList());

            table.setColumns(cols);
            return table;
        }).collect(Collectors.toList());
    }

    private static String toSnakeCase(String javaName) {
        return javaName.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    // JSON 配置对应的 DTO
    public static class JsonWrapper {
        private List<JsonTable> tables;

        public List<JsonTable> getTables() {
            return tables;
        }

        public void setTables(List<JsonTable> tables) {
            this.tables = tables;
        }
    }

    public static class JsonTable {
        private String tableName;
        private String entityName;
        private List<JsonColumn> columns;

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getEntityName() {
            return entityName;
        }

        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        public List<JsonColumn> getColumns() {
            return columns;
        }

        public void setColumns(List<JsonColumn> columns) {
            this.columns = columns;
        }
    }

    public static class JsonColumn {
        private String javaName;
        private String javaType;
        private String comment;

        public String getJavaName() {
            return javaName;
        }

        public void setJavaName(String javaName) {
            this.javaName = javaName;
        }

        public String getJavaType() {
            return javaType;
        }

        public void setJavaType(String javaType) {
            this.javaType = javaType;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
