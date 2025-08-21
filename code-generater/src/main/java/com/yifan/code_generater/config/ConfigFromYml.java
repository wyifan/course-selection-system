package com.yifan.code_generater.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

import lombok.Data;

@Data
public class ConfigFromYml {
    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;
    private String jdbcDriver;

    private String basePackage;
    private String entityPackage;
    private String mapperPackage;
    private String servicePackage;
    private String controllerPackage;
    private String basePackageForBaseEntity;

    private String sqlOutputDir;
    private String sqlTemplateDir = "templates/sql";

    private Map<String, String> javaToSqlMap = new HashMap<>();

    public String mapJavaToSql(String javaType) {
        return javaToSqlMap.getOrDefault(javaType, "VARCHAR(255)");
    }

    // ================== 加载配置 ==================
    @SuppressWarnings("unchecked")
    public static ConfigFromYml loadFromYaml(String yamlFile) {
        try (InputStream input = ConfigFromYml.class.getClassLoader().getResourceAsStream(yamlFile)) {
            if (input == null) {
                throw new RuntimeException("配置文件未找到: " + yamlFile);
            }

            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(input);
            Map<String, Object> gen = (Map<String, Object>) root.get("generator");

            ConfigFromYml cfg = new ConfigFromYml();

            // JDBC
            Map<String, Object> jdbc = (Map<String, Object>) gen.get("jdbc");
            cfg.jdbcUrl = (String) jdbc.get("url");
            cfg.jdbcUsername = (String) jdbc.get("username");
            cfg.jdbcPassword = jdbc.get("password") == null ? null : String.valueOf(jdbc.get("password"));
            cfg.jdbcDriver = (String) jdbc.get("driver");

            // sql
            Map<String, Object> sql = (Map<String, Object>) gen.get("sql");
            cfg.sqlOutputDir = (String) sql.get("output");      

            // 包配置
            Map<String, Object> pkg = (Map<String, Object>) gen.get("package");
            cfg.basePackage = (String) pkg.get("base");
            cfg.entityPackage = (String) pkg.get("entity");
            cfg.mapperPackage = (String) pkg.get("mapper");
            cfg.servicePackage = (String) pkg.get("service");
            cfg.controllerPackage = (String) pkg.get("controller");
            cfg.basePackageForBaseEntity = (String) pkg.get("baseEntity");

            // 类型映射
            Map<String, String> typeMapping = (Map<String, String>) gen.get("type-mapping");
            cfg.javaToSqlMap.putAll(typeMapping);

            return cfg;
        } catch (Exception e) {
            throw new RuntimeException("加载配置失败: " + yamlFile, e);
        }
    }
}
