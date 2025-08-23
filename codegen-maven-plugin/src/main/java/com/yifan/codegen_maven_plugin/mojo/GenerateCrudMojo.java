package com.yifan.codegen_maven_plugin.mojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "generate-crud")
public class GenerateCrudMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "executeSql", defaultValue = "false")
    private boolean executeSql;

    private Configuration freemarkerCfg;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            initFreeMarker();
            Map<String, Object> settings = loadConfig("setting.yml", true);
            Map<String, Object> tableDefinitions = loadConfig("table-definitions.json", false);
            Map<String, Object> generatorConfig = (Map<String, Object>) settings.get("generator");

            // [MODIFIED] 覆盖 basePackage
            overrideBasePackageFromPom(generatorConfig);

            Map<String, Object> rootDataModel = new HashMap<>();
            rootDataModel.put("generator", generatorConfig);

            generateBaseFiles(rootDataModel, generatorConfig);
            generateCodeForTables(rootDataModel, tableDefinitions, generatorConfig);
            manageApplicationYml(generatorConfig);
            manageDatabaseSchema(generatorConfig, tableDefinitions);

            getLog().info("✅ 所有任务执行成功!");

        } catch (Exception e) {
            getLog().error("代码生成失败", e);
            throw new MojoExecutionException("代码生成过程中发生错误", e);
        }
    }

    /**
     * [NEW] 新增方法：从POM中读取groupId和artifactId来覆盖basePackage
     */
    private void overrideBasePackageFromPom(Map<String, Object> generatorConfig) {
        String groupId = project.getGroupId();
        String artifactId = project.getArtifactId();

        if (groupId == null || artifactId == null) {
            getLog().warn("无法从 pom.xml 中获取 groupId 或 artifactId，将使用 setting.yml 中的配置。");
            return;
        }

        // 清理 artifactId 中的 '-' 符号
        String sanitizedArtifactId = artifactId.replace("-", "_");
        String pomBasePackage = groupId + "." + sanitizedArtifactId;

        getLog().info("从 pom.xml 推断出 basePackage: " + pomBasePackage + "，将覆盖 setting.yml 中的配置。");

        // 获取或创建 package 配置 map
        Map<String, Object> packageConfig = (Map<String, Object>) generatorConfig.computeIfAbsent("package",
                k -> new HashMap<>());

        // 覆盖 basePackage 的值
        packageConfig.put("basePackage", pomBasePackage);
    }

    private void initFreeMarker() {
        freemarkerCfg = new Configuration(Configuration.VERSION_2_3_31);
        freemarkerCfg.setClassForTemplateLoading(this.getClass(), "/templates");
        freemarkerCfg.setDefaultEncoding("UTF-8");
        freemarkerCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    private Map<String, Object> loadConfig(String fileName, boolean isYaml) throws Exception {
        File userFile = new File(project.getBasedir(), "src/main/resources/" + fileName);
        InputStream inputStream;
        if (userFile.exists() && userFile.isFile()) {
            getLog().info("使用项目本地配置文件: " + userFile.getAbsolutePath());
            inputStream = new FileInputStream(userFile);
        } else {
            getLog().info("未找到项目本地配置文件 '" + fileName + "', 使用插件默认配置。");
            inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
            if (inputStream == null)
                throw new MojoExecutionException("插件默认配置文件 '" + fileName + "' 丢失!");
        }
        ObjectMapper mapper = isYaml ? new ObjectMapper(new YAMLFactory()) : new ObjectMapper();
        try (InputStream in = inputStream) {
            return mapper.readValue(in, Map.class);
        }
    }

    private void generateBaseFiles(Map<String, Object> dataModel, Map<String, Object> generatorConfig)
            throws Exception {
        getLog().info("--- 正在生成基础文件 ---");
        processTemplate("baseEntity.ftl", dataModel,
                getJavaOutputPath(generatorConfig, "baseEntity", "BaseEntity.java"));
    }

    private void generateCodeForTables(Map<String, Object> rootDataModel, Map<String, Object> tableDefinitions,
            Map<String, Object> generatorConfig) throws Exception {
        getLog().info("--- 正在为表定义生成代码 ---");
        List<Map<String, Object>> tables = (List<Map<String, Object>>) tableDefinitions.get("tables");
        if (tables == null || tables.isEmpty()) {
            getLog().warn("table-definitions.json 中没有找到任何表格定义，跳过。");
            return;
        }
        for (Map<String, Object> table : tables) {
            getLog().info("正在处理表: " + table.get("tableName"));
            rootDataModel.put("table", table);
            generateTableSpecificFiles(rootDataModel, generatorConfig);
        }
    }

    private void generateTableSpecificFiles(Map<String, Object> dataModel, Map<String, Object> generatorConfig)
            throws Exception {
        String entityName = (String) ((Map<String, Object>) dataModel.get("table")).get("entityName");
        processTemplate("dto.ftl", dataModel, getJavaOutputPath(generatorConfig, "dto", entityName + "DTO.java"));
        processTemplate("entity.ftl", dataModel, getJavaOutputPath(generatorConfig, "entity", entityName + ".java"));
        processTemplate("service.ftl", dataModel,
                getJavaOutputPath(generatorConfig, "service", "I" + entityName + "Service.java"));
        processTemplate("serviceImpl.ftl", dataModel,
                getJavaOutputPath(generatorConfig, "serviceImpl", entityName + "ServiceImpl.java"));
        processTemplate("controller.ftl", dataModel,
                getJavaOutputPath(generatorConfig, "controller", entityName + "Controller.java"));
        processTemplate("mapper.ftl", dataModel,
                getJavaOutputPath(generatorConfig, "mapper", entityName + "Mapper.java"));
        processTemplate("mapperxml.ftl", dataModel, getResourceOutputPath("mapper", entityName + "Mapper.xml"));
    }

    private void manageApplicationYml(Map<String, Object> generatorConfig) throws Exception {
        getLog().info("--- 正在检查和更新 application.yml ---");
        File ymlFile = new File(project.getBasedir(), "src/main/resources/application.yml");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> appConfig = ymlFile.exists() ? mapper.readValue(ymlFile, Map.class) : new LinkedHashMap<>();

        Map<String, Object> mybatisPlus = (Map<String, Object>) appConfig.computeIfAbsent("mybatis-plus",
                k -> new LinkedHashMap<>());
        String expectedLocation = "classpath*:/mapper/**/*.xml";
        if (!expectedLocation.equals(mybatisPlus.get("mapper-locations"))) {
            mybatisPlus.put("mapper-locations", expectedLocation);
            mapper.writeValue(ymlFile, appConfig);
            getLog().info("application.yml 中 mybatis-plus.mapper-locations 已更新。");
        } else {
            getLog().info("application.yml 配置已是最新，无需修改。");
        }
    }

private void manageDatabaseSchema(Map<String, Object> generatorConfig, Map<String, Object> tableDefinitions) throws Exception {
        getLog().info("--- 正在生成和比对数据库 Schema ---");
        Map<String, String> typeMapping = (Map<String, String>) generatorConfig.get("type-mapping");
        List<Map<String, Object>> tables = (List<Map<String, Object>>) tableDefinitions.get("tables");
        StringBuilder sqlScript = new StringBuilder("-- Auto-generated by crud-generator-plugin\n\n");

        Set<String> protectedColumns = new HashSet<>(Arrays.asList(
            "id", "created_by", "created_by_name", "create_time", 
            "updated_by", "updated_by_name", "updated_time", "is_deleted", "version"
        ));

        Connection conn = null;
        try {
            conn = getDbConnection(generatorConfig);
            for (Map<String, Object> table : tables) {
                String tableName = (String) table.get("tableName");
                DatabaseMetaData metaData = conn.getMetaData();
                ResultSet rs = metaData.getTables(null, null, tableName, null);

                if (rs.next()) { // 表存在，比对列
                    sqlScript.append("-- Updating table '").append(tableName).append("'\n");
                    
                    Map<String, Map<String, String>> existingColumnsMap = getExistingColumns(metaData, tableName);
                    List<Map<String, String>> desiredColumnsList = (List<Map<String, String>>) table.get("columns");
                    Set<String> desiredColumnNames = desiredColumnsList.stream()
                                                                       .map(c -> toSnakeCase(c.get("javaName")))
                                                                       .collect(Collectors.toSet());

                    // 1. 识别需要新增的字段
                    List<Map<String, String>> columnsToAdd = desiredColumnsList.stream()
                        .filter(c -> !existingColumnsMap.containsKey(toSnakeCase(c.get("javaName"))))
                        .collect(Collectors.toList());

                    // 2. 识别需要删除的字段
                    List<String> columnsToDrop = existingColumnsMap.keySet().stream()
                        .filter(existingCol -> !desiredColumnNames.contains(existingCol) && !protectedColumns.contains(existingCol))
                        .collect(Collectors.toList());

                    boolean hasChanges = false;

                    // 3. 生成 DROP 语句
                    if (!columnsToDrop.isEmpty()) {
                        hasChanges = true;
                        for (String columnName : columnsToDrop) {
                            sqlScript.append("ALTER TABLE `").append(tableName).append("` DROP COLUMN `").append(columnName).append("`;\n");
                        }
                    }

                    // 4. 生成 ADD 语句 (带位置)
                    if (!columnsToAdd.isEmpty()) {
                        hasChanges = true;
                        String lastKnownColumn = "id"; // 默认加在id后面
                        List<String> desiredColumnsInOrder = desiredColumnsList.stream()
                                                                               .map(c -> toSnakeCase(c.get("javaName")))
                                                                               .collect(Collectors.toList());
                        
                        for (int i = desiredColumnsInOrder.size() - 1; i >= 0; i--) {
                            String colName = desiredColumnsInOrder.get(i);
                            if (existingColumnsMap.containsKey(colName)) {
                                lastKnownColumn = colName;
                                break;
                            }
                        }

                        for (Map<String, String> column : columnsToAdd) {
                            String javaName = column.get("javaName");
                            String columnName = toSnakeCase(javaName);
                            String javaType = column.get("javaType");
                            String dbType = typeMapping.get(javaType);
                            String comment = column.get("comment");
                            sqlScript.append("ALTER TABLE `").append(tableName).append("` ADD COLUMN `").append(columnName)
                                     .append("` ").append(dbType)
                                     .append(" COMMENT '").append(comment).append("'")
                                     .append(" AFTER `").append(lastKnownColumn).append("`;\n");
                            lastKnownColumn = columnName; // 更新锚点，确保后续字段能跟在后面
                        }
                    }

                    if (!hasChanges) {
                        sqlScript.append("-- Table '").append(tableName).append("' structure is up to date.\n");
                    }

                } else { // 表不存在，生成 CREATE 语句
                    sqlScript.append(generateCreateTableSql(table, typeMapping));
                }
                sqlScript.append("\n");
            }
        } finally {
            if (conn != null) conn.close();
        }

        File sqlFile = new File(project.getBasedir(), "src/main/resources/sql/schema.sql");
        sqlFile.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(sqlFile), StandardCharsets.UTF_8)) {
            writer.write(sqlScript.toString());
            getLog().info("SQL 脚本已生成: " + sqlFile.getAbsolutePath());
        }

        if (executeSql) {
            getLog().info("--- 正在执行 SQL 脚本到数据库 ---");
            Connection executionConn = null;
            try {
                executionConn = getDbConnection(generatorConfig);
                executionConn.setAutoCommit(false); 
                
                String fullScript = sqlScript.toString();
                String executableScript = Arrays.stream(fullScript.split("\n"))
                                                .filter(line -> !line.trim().startsWith("--"))
                                                .collect(Collectors.joining("\n"));

                try (Statement stmt = executionConn.createStatement()) {
                    for (String sql : executableScript.split(";")) {
                        if (sql != null && !sql.trim().isEmpty()) {
                            getLog().info("执行: " + sql.trim());
                            stmt.execute(sql.trim());
                        }
                    }
                }
                
                executionConn.commit(); 
                getLog().info("✅ SQL 脚本执行成功并已提交!");
            } catch (Exception e) {
                getLog().error("SQL 脚本执行失败! 正在回滚事务...", e);
                if (executionConn != null) {
                    try {
                        executionConn.rollback(); 
                        getLog().info("事务已回滚。");
                    } catch (SQLException rollbackEx) {
                        getLog().error("事务回滚失败!", rollbackEx);
                    }
                }
            } finally {
                if (executionConn != null) {
                    try {
                        executionConn.setAutoCommit(true); 
                        executionConn.close();
                    } catch (SQLException closeEx) {
                        // ignore
                    }
                }
            }
        }
    }
    private Connection getDbConnection(Map<String, Object> generatorConfig)
            throws SQLException, ClassNotFoundException {
        Map<String, String> jdbc = (Map<String, String>) generatorConfig.get("jdbc");
        Class.forName(jdbc.get("driver"));
        return DriverManager.getConnection(jdbc.get("url"), jdbc.get("username"), String.valueOf(jdbc.get("password")));
    }

    private Map<String, Map<String, String>> getExistingColumns(DatabaseMetaData metaData, String tableName)
            throws SQLException {
        Map<String, Map<String, String>> columns = new HashMap<>();
        ResultSet rs = metaData.getColumns(null, null, tableName, null);
        while (rs.next()) {
            Map<String, String> colInfo = new HashMap<>();
            String colName = rs.getString("COLUMN_NAME");
            colInfo.put("type", rs.getString("TYPE_NAME"));
            colInfo.put("size", rs.getString("COLUMN_SIZE"));
            columns.put(colName, colInfo);
        }
        return columns;
    }

    private String generateCreateTableSql(Map<String, Object> table, Map<String, String> typeMapping) {
        String tableName = (String) table.get("tableName");
        List<Map<String, String>> columns = (List<Map<String, String>>) table.get("columns");
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE `").append(tableName).append("` (\n");
        sb.append("  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',\n");
        for (Map<String, String> column : columns) {
            String javaName = column.get("javaName");
            String columnName = toSnakeCase(javaName);
            String javaType = column.get("javaType");
            String dbType = typeMapping.get(javaType);
            String comment = column.get("comment");
            sb.append("  `").append(columnName).append("` ").append(dbType).append(" COMMENT '").append(comment)
                    .append("',\n");
        }
        sb.append("  `created_by` BIGINT COMMENT '创建人ID',\n");
        sb.append("  `created_by_name` VARCHAR(255) COMMENT '创建人名称',\n");
        sb.append("  `create_time` DATETIME COMMENT '创建时间',\n");
        sb.append("  `updated_by` BIGINT COMMENT '更新人ID',\n");
        sb.append("  `updated_by_name` VARCHAR(255) COMMENT '更新人名称',\n");
        sb.append("  `updated_time` DATETIME COMMENT '更新时间',\n");
        sb.append("  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标志',\n");
        sb.append("  `version` INT DEFAULT 1 COMMENT '乐观锁版本号',\n");
        sb.append("  PRIMARY KEY (`id`)\n");
        sb.append(") ENGINE=InnoDB COMMENT='").append(table.get("entityName")).append("表';\n");
        return sb.toString();
    }

    private void processTemplate(String templateName, Map<String, Object> dataModel, String outputPath)
            throws Exception {
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            Template template = freemarkerCfg.getTemplate(templateName);
            template.process(dataModel, writer);
            getLog().info("  -> 已生成文件: " + outputPath);
        }
    }

    private String getJavaOutputPath(Map<String, Object> generatorConfig, String packageKey, String fileName) {
        // [FIXED] Pass generatorConfig as a parameter instead of reading from
        // FreeMarker
        Map<String, Object> packageConf = (Map<String, Object>) generatorConfig.get("package");
        String basePackage = (String) packageConf.get("basePackage");
        String subPackage = (String) packageConf.get(packageKey);
        String fullPackage = basePackage + "." + subPackage;
        String packagePath = fullPackage.replace('.', '/');
        return project.getBasedir().getAbsolutePath() + "/src/main/java/" + packagePath + "/" + fileName;
    }

    private String getResourceOutputPath(String dir, String fileName) {
        return project.getBasedir().getAbsolutePath() + "/src/main/resources/" + dir + "/" + fileName;
    }

    private String toSnakeCase(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
}