package com.yifan.code_generater.config;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class GenerateConfig {
    /** 生成的包名 */
    private String basePackage = "com.example.demo";

    /** 输出目录 */
    private String outputDir = "generated-code";

    /** 模式：definition=字段输入，database=数据库解析 */
    private String mode = "definition";

    /** 数据库配置（模式二用到） */
    private String jdbcUrl = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC";
    private String jdbcUsername = "root";
    private String jdbcPassword = "123456";
    private String jdbcDriver = "com.mysql.cj.jdbc.Driver";

    /** 类型映射（Java -> SQL） */
    private Map<String, String> javaToSqlMap = new HashMap<>();

    public GenerateConfig() {
        javaToSqlMap.put("String", "VARCHAR(255)");
        javaToSqlMap.put("Integer", "INT");
        javaToSqlMap.put("Long", "BIGINT");
        javaToSqlMap.put("Double", "DOUBLE");
        javaToSqlMap.put("BigDecimal", "DECIMAL(18,2)");
        javaToSqlMap.put("LocalDate", "DATE");
        javaToSqlMap.put("LocalDateTime", "DATETIME");
        javaToSqlMap.put("Boolean", "TINYINT(1)");
    }

     public String mapJavaToSql(String javaType) {
        return javaToSqlMap.getOrDefault(javaType, "VARCHAR(255)");
    }

}
