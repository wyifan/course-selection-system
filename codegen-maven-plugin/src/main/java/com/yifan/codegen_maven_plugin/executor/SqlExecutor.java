package com.yifan.codegen_maven_plugin.executor;

import com.yifan.codegen_maven_plugin.entity.ColumnMeta;
import com.yifan.codegen_maven_plugin.entity.TableMeta;
import com.yifan.codegen_maven_plugin.config.ConfigFromYml;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.nio.file.Paths;

@Slf4j
public class SqlExecutor {
    private final ConfigFromYml config;

    public SqlExecutor(ConfigFromYml config) {
        this.config = config;
    }

    /**
     * 根据 TableMeta 执行建表或更新表
     */
    public void executeCreateOrUpdateTable(TableMeta table) throws Exception {
        // 检查表是否存在
        boolean exists = checkTableExists(table.getTableName());

        String ddl;
        if (!exists) {
            // 创建新表
            ddl = buildCreateTableSql(table);
            runSql(ddl);
        } else {
            // 增量更新表
            ddl = buildAlterTableSql(table);
            if (ddl != null && !ddl.isBlank()) {
                runSql(ddl);
            } else {
                ddl = "-- 表结构已是最新，无需修改";
            }
        }

        // 输出到文件
        writeSqlToFile(table.getTableName(), ddl);
    }

    /**
     * 检查表是否存在
     */
    private boolean checkTableExists(String tableName) throws Exception {
        Class.forName(config.getJdbcDriver());
        try (Connection conn = DriverManager.getConnection(config.getJdbcUrl(), config.getJdbcUsername(),
                config.getJdbcPassword())) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, tableName, new String[] { "TABLE" })) {
                return rs.next();
            }
        }
    }

    /**
     * 执行 SQL
     */
    private void runSql(String sql) throws Exception {
        log.info("🔧 执行 SQL：{}", sql);
        Class.forName(config.getJdbcDriver());
        try (Connection conn = DriverManager.getConnection(config.getJdbcUrl(), config.getJdbcUsername(),
                config.getJdbcPassword());
                Statement stmt = conn.createStatement()) {
            for (String s : sql.split(";")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    stmt.executeUpdate(trimmed);
                }
            }
        }
    }

    /**
     * 执行建表 SQL
     */
    public void executeCreateTable(TableMeta table) throws Exception {
        String ddl = buildCreateTableSql(table);
        log.info("🔧 执行建表语句：{}\n", ddl);

        Class.forName(config.getJdbcDriver());
        try (Connection conn = DriverManager.getConnection(config.getJdbcUrl(), config.getJdbcUsername(),
                config.getJdbcPassword());
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(ddl);
        }
    }

    /**
     * 构建 CREATE TABLE SQL
     */
    private String buildCreateTableSql(TableMeta table) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS `").append(table.getTableName()).append("` (\n");

        // 主键 + 基础字段
        if (table.isUseBaseEntity()) {
            sb.append("  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,\n");
        }

        // 业务字段
        for (int i = 0; i < table.getColumns().size(); i++) {
            ColumnMeta col = table.getColumns().get(i);
            sb.append("  `").append(col.getColumnName()).append("` ")
                    .append(col.getSqlType());

            if (col.isPrimaryKey() && !table.isUseBaseEntity()) {
                sb.append(" PRIMARY KEY");
            }
            if (col.getComment() != null && !col.getComment().isEmpty()) {
                sb.append(" COMMENT '").append(col.getComment()).append("'");
            }

            sb.append(",\n");
        }

        if (table.isUseBaseEntity()) {
            sb.append("  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n");
            sb.append("  `created_by` BIGINT COMMENT '创建人',\n");
            sb.append("  `created_by_name` VARCHAR(150) COMMENT '创建人名称',\n");

            sb.append(
                    "  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',\n");
            sb.append("  `updated_by` BIGINT COMMENT '更新人',\n");
            sb.append("  `updated_by_name` VARCHAR(150) COMMENT '更新人名称',\n");

            sb.append("  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标志',\n");

            sb.append("  `version` INT DEFAULT 1 COMMENT '版本号'\n");
        }

        sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n");
        return sb.toString();
    }

    /**
     * 构建 ALTER TABLE SQL（增量更新）
     */
    private String buildAlterTableSql(TableMeta table) throws Exception {
        Set<String> existingColumns = getExistingColumns(table.getTableName());
        StringBuilder sb = new StringBuilder();

        for (ColumnMeta col : table.getColumns()) {
            if (!existingColumns.contains(col.getColumnName())) {
                sb.append("ALTER TABLE `").append(table.getTableName()).append("` ADD COLUMN `")
                        .append(col.getColumnName()).append("` ")
                        .append(col.getSqlType());
                if (col.getComment() != null && !col.getComment().isEmpty()) {
                    sb.append(" COMMENT '").append(col.getComment()).append("'");
                }
                sb.append(";\n");
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }

    /**
     * 获取已存在字段
     */
    private Set<String> getExistingColumns(String tableName) throws Exception {
        Set<String> columns = new HashSet<>();
        Class.forName(config.getJdbcDriver());
        try (Connection conn = DriverManager.getConnection(config.getJdbcUrl(), config.getJdbcUsername(),
                config.getJdbcPassword())) {
            String sql = """
                    SELECT COLUMN_NAME FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tableName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        columns.add(rs.getString("COLUMN_NAME"));
                    }
                }
            }
        }
        return columns;
    }

    /**
     * 输出 SQL 到文件
     */
    private void writeSqlToFile(String tableName, String sql) throws Exception {
        String dir = config.getSqlOutputDir();
        Files.createDirectories(Paths.get(dir));
        String filePath = dir + "/" + tableName + "_schema.sql";

        // 检查文件是否存在且包含相同的 SQL
        if (isSqlAlreadyExists(filePath, sql)) {
            log.info("⏩ SQL 已存在，跳过写入：{}", tableName);
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath, true), StandardCharsets.UTF_8))) {

            writer.write("-- " + java.time.LocalDateTime.now() + "\n");
            writer.write(sql + "\n\n");
        }

        log.info("📄 SQL 已写入文件：{}", filePath);
    }

    private boolean isSqlAlreadyExists(String filePath, String sql) {
        File file = new File(filePath);

        if (!file.exists()) {
            return false;
        }

        try {
            // 读取文件所有行
            List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

            // 标准化要检查的 SQL
            String normalizedSql = normalizeSql(sql);

            // 检查每一段 SQL（以空行分隔）
            StringBuilder currentSqlBlock = new StringBuilder();
            for (String line : lines) {
                // 跳过注释行和空行
                if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                    // 如果遇到空行，检查当前 SQL 块
                    if (!currentSqlBlock.isEmpty()) {
                        String normalizedBlock = normalizeSql(currentSqlBlock.toString());
                        if (normalizedBlock.equals(normalizedSql)) {
                            return true;
                        }
                        currentSqlBlock.setLength(0); // 重置
                    }
                    continue;
                }

                currentSqlBlock.append(line).append(" ");
            }

            // 检查最后一个 SQL 块
            if (!currentSqlBlock.isEmpty()) {
                String normalizedBlock = normalizeSql(currentSqlBlock.toString());
                if (normalizedBlock.equals(normalizedSql)) {
                    return true;
                }
            }

            return false;
        } catch (IOException e) {
            log.warn("无法读取文件检查重复 SQL: {}", filePath, e);
            return false;
        }
    }

    /**
     * 标准化 SQL 字符串以便比较
     * 移除注释、多余空格和换行
     * 
     * @param sql SQL 字符串
     * @return 标准化后的 SQL 字符串
     */
    private String normalizeSql(String sql) {
        // 移除 SQL 注释
        String normalized = sql.replaceAll("--.*?\\n", " ") // 移除行注释
                .replaceAll("/\\*.*?\\*/", " ") // 移除块注释
                .replaceAll("\\s+", " ") // 将多个空格/换行替换为单个空格
                .trim(); // 移除首尾空格

        // 可选：移除分号（如果不需要）
        normalized = normalized.replace(";", "");

        return normalized;
    }
}
