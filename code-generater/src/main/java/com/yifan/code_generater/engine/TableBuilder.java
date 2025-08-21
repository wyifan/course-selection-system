package com.yifan.code_generater.engine;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.yifan.code_generater.entity.ColumnMeta;
import com.yifan.code_generater.entity.TableMeta;
import com.yifan.code_generater.config.GenerateConfig;

public class TableBuilder {
    private final GenerateConfig config;

    public TableBuilder(GenerateConfig config) {
        this.config = config;
    }

    /**
     * 模式一：通过字段定义构建
     */
    public TableMeta buildFromDefinition(String tableName, String entityName, List<ColumnMeta> columns) {
        TableMeta table = new TableMeta();
        table.setTableName(tableName);
        table.setEntityName(entityName);
        table.setColumns(columns);
        table.setUseBaseEntity(true);
        return table;
    }

    /**
     * 模式二：从数据库读取表信息
     */
    public TableMeta buildFromDatabase(String tableName) throws Exception {
        TableMeta table = new TableMeta();
        table.setTableName(tableName);
        table.setEntityName(toCamelCase(tableName, true));
        table.setUseBaseEntity(false);

        List<ColumnMeta> columns = new ArrayList<>();

        Class.forName(config.getJdbcDriver());
        try (Connection conn = DriverManager.getConnection(config.getJdbcUrl(), config.getJdbcUsername(),
                config.getJdbcPassword())) {
            String sql = """
                    SELECT COLUMN_NAME, DATA_TYPE, COLUMN_KEY, COLUMN_COMMENT
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tableName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ColumnMeta col = new ColumnMeta();
                        col.setColumnName(rs.getString("COLUMN_NAME"));
                        col.setJavaName(toCamelCase(rs.getString("COLUMN_NAME"), false));
                        col.setSqlType(rs.getString("DATA_TYPE"));
                        col.setJavaType(sqlTypeToJava(rs.getString("DATA_TYPE")));
                        col.setPrimaryKey("PRI".equals(rs.getString("COLUMN_KEY")));
                        col.setComment(rs.getString("COLUMN_COMMENT"));
                        columns.add(col);
                    }
                }
            }
        }

        table.setColumns(columns);
        return table;
    }

    private String sqlTypeToJava(String sqlType) {
        return switch (sqlType.toLowerCase()) {
            case "varchar", "text", "char" -> "String";
            case "int", "tinyint" -> "Integer";
            case "bigint" -> "Long";
            case "datetime", "timestamp" -> "LocalDateTime";
            case "bit" -> "Boolean";
            default -> "String";
        };
    }

    private String toCamelCase(String str, boolean capitalizeFirst) {
        StringBuilder sb = new StringBuilder();
        boolean upper = capitalizeFirst;
        for (char c : str.toCharArray()) {
            if (c == '_' || c == '-') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
