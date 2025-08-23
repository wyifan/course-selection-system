package com.yifan.codegen_maven_plugin.mojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "generate-crud")
public class GenerateCrudMojo extends AbstractMojo {
    
    
    // [MODIFIED] 提取所有字符串为常量
    private static final String SETTINGS_FILE = "setting.yml";
    private static final String TABLE_DEFINITIONS_FILE = "table-definitions.json";
    private static final String APPLICATION_YML_FILE = "application.yml";
    private static final String SCHEMA_SQL_FILE = "sql/schema.sql";
    private static final String TEMPLATES_DIR = "/templates";
    private static final String LOG_PREFIX = "crud-generator-plugin: ";

    private static final String PACKAGE_KEY_BASE = "basePackage";
    private static final String PACKAGE_KEY_ENTITY = "entity";
    private static final String PACKAGE_KEY_BASE_ENTITY = "baseEntity";
    private static final String PACKAGE_KEY_DTO = "dto";
    private static final String PACKAGE_KEY_SERVICE = "service";
    private static final String PACKAGE_KEY_SERVICE_IMPL = "serviceImpl";
    private static final String PACKAGE_KEY_CONTROLLER = "controller";
    private static final String PACKAGE_KEY_MAPPER = "mapper";

    private static final Set<String> PROTECTED_COLUMNS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "id", "created_by", "created_by_name", "create_time", 
        "updated_by", "updated_by_name", "updated_time", "is_deleted", "version"
    )));

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "executeSql", defaultValue = "false")
    private boolean executeSql;

    private Configuration freemarkerCfg;

    private static class DependencyInfo {
        final String groupId;
        final String artifactId;
        final String version;
        final String scope;
        DependencyInfo(String groupId, String artifactId, String version, String scope) {
            this.groupId = groupId; this.artifactId = artifactId; this.version = version; this.scope = scope;
        }
        String getManagementId() { return groupId + ":" + artifactId; }
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            initFreeMarker();
            Map<String, Object> settings = loadConfig(SETTINGS_FILE, true);
            Map<String, Object> tableDefinitions = loadConfig(TABLE_DEFINITIONS_FILE, false);
            Map<String, Object> generatorConfig = (Map<String, Object>) settings.get("generator");

            manageProjectConfiguration(generatorConfig);

            Map<String, Object> rootDataModel = new HashMap<>();
            rootDataModel.put("generator", generatorConfig);

            generateCode(rootDataModel, tableDefinitions, generatorConfig);
            
            String sqlScript = generateAndCompareSchema(generatorConfig, tableDefinitions);
            saveSqlScript(sqlScript);
            
            if (executeSql) {
                executeSqlScript(sqlScript, generatorConfig);
            }

            getLog().info(LOG_PREFIX + "✅ 所有任务执行成功!");

        } catch (Exception e) {
            getLog().error(LOG_PREFIX + "代码生成失败", e);
            throw new MojoExecutionException("代码生成过程中发生错误", e);
        }
    }
    
    private void manageProjectConfiguration(Map<String, Object> generatorConfig) throws Exception {
        getLog().info(LOG_PREFIX + "--- 正在初始化项目配置 ---");
        File pomFile = project.getFile();
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        try (FileReader fileReader = new FileReader(pomFile, StandardCharsets.UTF_8)) {
            model = reader.read(fileReader);
        }

        boolean hasParent = model.getParent() != null && "spring-boot-starter-parent".equals(model.getParent().getArtifactId());
        boolean pomModified = false;

        pomModified |= manageProperties(model, hasParent);
        pomModified |= manageDependencies(model, hasParent);
        pomModified |= manageBuild(model, hasParent);
        
        if (pomModified) {
            MavenXpp3Writer writer = new MavenXpp3Writer();
            try (FileWriter fileWriter = new FileWriter(pomFile, StandardCharsets.UTF_8)) {
                writer.write(fileWriter, model);
                getLog().info(LOG_PREFIX + "pom.xml 已更新。");
            }
        } else {
            getLog().info(LOG_PREFIX + "pom.xml 配置完整，无需修改。");
        }

        overrideBasePackageFromPom(generatorConfig);
        manageApplicationYml(generatorConfig);
    }
    
    private Map<String, String> getRequiredProperties(boolean hasParent) {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("java.version", "17");
        props.put("project.build.sourceEncoding", "UTF-8");

        if (!hasParent) {
            getLog().info(LOG_PREFIX + "未找到 Spring Boot 父POM，将添加版本属性。");
            props.put("spring-boot.version", "3.2.5"); 
            props.put("mybatis-plus.version", "3.5.5");
            props.put("mysql.version", "8.3.0");
            props.put("lombok.version", "1.18.32");
        }
        return props;
    }

    private boolean manageProperties(Model model, boolean hasParent) {
        boolean modified = false;
        Properties properties = model.getProperties();
        if (properties == null) {
            properties = new Properties();
            model.setProperties(properties);
        }
        
        Map<String, String> requiredProps = getRequiredProperties(hasParent);
        for (Map.Entry<String, String> entry : requiredProps.entrySet()) {
            if (!entry.getValue().equals(properties.getProperty(entry.getKey()))) {
                properties.setProperty(entry.getKey(), entry.getValue());
                modified = true;
            }
        }
        return modified;
    }

    private List<DependencyInfo> getRequiredDependencies(boolean hasParent) {
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("org.springframework.boot", "spring-boot-starter-web", hasParent ? null : "${spring-boot.version}", null));
        deps.add(new DependencyInfo("com.baomidou", "mybatis-plus-spring-boot3-starter", hasParent ? "3.5.5" : "${mybatis-plus.version}", null));
        deps.add(new DependencyInfo("com.mysql", "mysql-connector-j", hasParent ? null : "${mysql.version}", null));
        deps.add(new DependencyInfo("org.projectlombok", "lombok", hasParent ? null : "${lombok.version}", "provided"));
        return deps;
    }

    private boolean manageDependencies(Model model, boolean hasParent) {
        List<DependencyInfo> requiredDeps = getRequiredDependencies(hasParent);
        Set<String> existingDeps = model.getDependencies().stream()
                .map(d -> d.getGroupId() + ":" + d.getArtifactId())
                .collect(Collectors.toSet());
        
        boolean modified = false;
        for (DependencyInfo depInfo : requiredDeps) {
            if (!existingDeps.contains(depInfo.getManagementId())) {
                modified = true;
                Dependency dep = new Dependency();
                dep.setGroupId(depInfo.groupId);
                dep.setArtifactId(depInfo.artifactId);
                if (depInfo.version != null) dep.setVersion(depInfo.version);
                if (depInfo.scope != null) dep.setScope(depInfo.scope);
                model.addDependency(dep);
                getLog().info(LOG_PREFIX + "已添加缺失的依赖: " + depInfo.getManagementId());
            }
        }
        return modified;
    }
    
    private boolean manageBuild(Model model, boolean hasParent) {
        boolean modified = false;
        Build build = model.getBuild();
        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }

        if (build.getPlugins().stream().noneMatch(p -> "maven-compiler-plugin".equals(p.getArtifactId()))) {
            Plugin compiler = new Plugin();
            compiler.setGroupId("org.apache.maven.plugins");
            compiler.setArtifactId("maven-compiler-plugin");
            compiler.setVersion("3.11.0");
            Xpp3Dom config = new Xpp3Dom("configuration");
            Xpp3Dom source = new Xpp3Dom("source");
            source.setValue("${java.version}");
            Xpp3Dom target = new Xpp3Dom("target");
            target.setValue("${java.version}");
            config.addChild(source);
            config.addChild(target);
            compiler.setConfiguration(config);
            build.addPlugin(compiler);
            modified = true;
            getLog().info(LOG_PREFIX + "已添加 maven-compiler-plugin 配置。");
        }

        if (build.getPlugins().stream().noneMatch(p -> "spring-boot-maven-plugin".equals(p.getArtifactId()))) {
            Plugin springBoot = new Plugin();
            springBoot.setGroupId("org.springframework.boot");
            springBoot.setArtifactId("spring-boot-maven-plugin");
            if (!hasParent) springBoot.setVersion(model.getProperties().getProperty("spring-boot.version"));
            Xpp3Dom config = new Xpp3Dom("configuration");
            Xpp3Dom excludes = new Xpp3Dom("excludes");
            Xpp3Dom exclude = new Xpp3Dom("exclude");
            Xpp3Dom groupId = new Xpp3Dom("groupId");
            groupId.setValue("org.projectlombok");
            Xpp3Dom artifactId = new Xpp3Dom("artifactId");
            artifactId.setValue("lombok");
            exclude.addChild(groupId);
            exclude.addChild(artifactId);
            excludes.addChild(exclude);
            config.addChild(excludes);
            springBoot.setConfiguration(config);
            build.addPlugin(springBoot);
            modified = true;
            getLog().info(LOG_PREFIX + "已添加 spring-boot-maven-plugin 配置 (含 lombok 排除)。");
        }
        return modified;
    }

    private void overrideBasePackageFromPom(Map<String, Object> generatorConfig) {
        String groupId = project.getGroupId();
        String artifactId = project.getArtifactId();
        if (groupId == null || artifactId == null) {
            getLog().warn(LOG_PREFIX + "无法从 pom.xml 中获取 groupId 或 artifactId，将使用 setting.yml 中的配置。");
            return;
        }
        String sanitizedArtifactId = artifactId.replace("-", "");
        String pomBasePackage = groupId + "." + sanitizedArtifactId;
        getLog().info(LOG_PREFIX + "从 pom.xml 推断出 basePackage: " + pomBasePackage + "，将覆盖 setting.yml 中的配置。");
        Map<String, Object> packageConfig = (Map<String, Object>) generatorConfig.computeIfAbsent("package", k -> new HashMap<>());
        packageConfig.put(PACKAGE_KEY_BASE, pomBasePackage);
    }

    private void initFreeMarker() {
        freemarkerCfg = new Configuration(Configuration.VERSION_2_3_31);
        freemarkerCfg.setClassForTemplateLoading(this.getClass(), TEMPLATES_DIR);
        freemarkerCfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarkerCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    private Map<String, Object> loadConfig(String fileName, boolean isYaml) throws Exception {
        File userFile = new File(project.getBasedir(), "src/main/resources/" + fileName);
        InputStream inputStream;
        if (userFile.exists() && userFile.isFile()) {
            getLog().info(LOG_PREFIX + "使用项目本地配置文件: " + userFile.getAbsolutePath());
            inputStream = new FileInputStream(userFile);
        } else {
            getLog().info(LOG_PREFIX + "未找到项目本地配置文件 '" + fileName + "', 使用插件默认配置。");
            inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
            if (inputStream == null) throw new MojoExecutionException("插件默认配置文件 '" + fileName + "' 丢失!");
        }
        ObjectMapper mapper = isYaml ? new ObjectMapper(new YAMLFactory()) : new ObjectMapper();
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return mapper.readValue(reader, Map.class);
        }
    }

    private void generateCode(Map<String, Object> rootDataModel, Map<String, Object> tableDefinitions, Map<String, Object> generatorConfig) throws Exception {
        generateBaseFiles(rootDataModel, generatorConfig);
        generateCodeForTables(rootDataModel, tableDefinitions, generatorConfig);
    }

    private void generateBaseFiles(Map<String, Object> dataModel, Map<String, Object> generatorConfig) throws Exception {
        getLog().info(LOG_PREFIX + "--- 正在生成基础文件 ---");
        processTemplate("baseEntity.ftl", dataModel, getJavaOutputPath(generatorConfig, PACKAGE_KEY_BASE_ENTITY, "BaseEntity.java"));
    }

    private void generateCodeForTables(Map<String, Object> rootDataModel, Map<String, Object> tableDefinitions, Map<String, Object> generatorConfig) throws Exception {
        getLog().info(LOG_PREFIX + "--- 正在为表定义生成代码 ---");
        List<Map<String, Object>> tables = (List<Map<String, Object>>) tableDefinitions.get("tables");
        if (tables == null || tables.isEmpty()) {
            getLog().warn(LOG_PREFIX + "table-definitions.json 中没有找到任何表格定义，跳过。");
            return;
        }
        for (Map<String, Object> table : tables) {
            getLog().info(LOG_PREFIX + "正在处理表: " + table.get("tableName"));
            rootDataModel.put("table", table);
            generateTableSpecificFiles(rootDataModel, generatorConfig);
        }
    }

    private void generateTableSpecificFiles(Map<String, Object> dataModel, Map<String, Object> generatorConfig) throws Exception {
        String entityName = (String) ((Map<String, Object>) dataModel.get("table")).get("entityName");
        processTemplate("dto.ftl", dataModel, getJavaOutputPath(generatorConfig, PACKAGE_KEY_DTO, entityName + "DTO.java"));
        processTemplate("entity.ftl", dataModel, getJavaOutputPath(generatorConfig, PACKAGE_KEY_ENTITY, entityName + ".java"));
        processTemplate("service.ftl", dataModel, getJavaOutputPath(generatorConfig, PACKAGE_KEY_SERVICE, "I" + entityName + "Service.java"));
        processTemplate("serviceImpl.ftl", dataModel, getJavaOutputPath(generatorConfig, PACKAGE_KEY_SERVICE_IMPL, entityName + "ServiceImpl.java"));
        processTemplate("controller.ftl", dataModel, getJavaOutputPath(generatorConfig, PACKAGE_KEY_CONTROLLER, entityName + "Controller.java"));
        processTemplate("mapper.ftl", dataModel, getJavaOutputPath(generatorConfig, PACKAGE_KEY_MAPPER, entityName + "Mapper.java"));
        processTemplate("mapperxml.ftl", dataModel, getResourceOutputPath("mapper", entityName + "Mapper.xml"));
    }

    private void manageApplicationYml(Map<String, Object> generatorConfig) throws Exception {
        getLog().info(LOG_PREFIX + "--- 正在检查和更新 application.yml ---");
        File ymlFile = new File(project.getBasedir(), "src/main/resources/" + APPLICATION_YML_FILE);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> appConfig = ymlFile.exists() ? mapper.readValue(ymlFile, Map.class) : new LinkedHashMap<>();
        if (appConfig == null) appConfig = new LinkedHashMap<>();
        boolean isModified = false;

        Map<String, Object> mybatisPlus = (Map<String, Object>) appConfig.computeIfAbsent("mybatis-plus", k -> new LinkedHashMap<>());
        String expectedLocation = "classpath*:/mapper/**/*.xml";
        if (!expectedLocation.equals(mybatisPlus.get("mapper-locations"))) {
            mybatisPlus.put("mapper-locations", expectedLocation);
            isModified = true;
            getLog().info(LOG_PREFIX + "application.yml: 已添加 mybatis-plus.mapper-locations 配置。");
        }

        Map<String, Object> spring = (Map<String, Object>) appConfig.computeIfAbsent("spring", k -> new LinkedHashMap<>());
        Map<String, Object> datasource = (Map<String, Object>) spring.computeIfAbsent("datasource", k -> new LinkedHashMap<>());
        
        // [MODIFIED] Cast to Map<String, Object> and convert values to String to prevent ClassCastException
        Map<String, Object> jdbcConfig = (Map<String, Object>) generatorConfig.get("jdbc");
        String url = String.valueOf(jdbcConfig.get("url"));
        String username = String.valueOf(jdbcConfig.get("username"));
        String password = String.valueOf(jdbcConfig.get("password"));
        String driver = String.valueOf(jdbcConfig.get("driver"));
        
        if (!Objects.equals(url, datasource.get("url")) ||
            !Objects.equals(username, datasource.get("username")) ||
            !Objects.equals(password, datasource.get("password")) ||
            !Objects.equals(driver, datasource.get("driver-class-name"))) {
            
            datasource.put("url", url);
            datasource.put("username", username);
            datasource.put("password", password);
            datasource.put("driver-class-name", driver);
            isModified = true;
            getLog().info(LOG_PREFIX + "application.yml: 已更新 spring.datasource 配置。");
        }

        if (isModified) {
            mapper.writeValue(ymlFile, appConfig);
            getLog().info(LOG_PREFIX + "application.yml 已保存。");
        } else {
            getLog().info(LOG_PREFIX + "application.yml 配置已是最新，无需修改。");
        }
    }

    private String generateAndCompareSchema(Map<String, Object> generatorConfig, Map<String, Object> tableDefinitions) throws Exception {
        getLog().info(LOG_PREFIX + "--- 正在生成和比对数据库 Schema ---");
        List<Map<String, Object>> tables = (List<Map<String, Object>>) tableDefinitions.get("tables");
        StringBuilder sqlScript = new StringBuilder("-- Auto-generated by crud-generator-plugin\n\n");

        try (Connection conn = getDbConnection(generatorConfig)) {
            for (Map<String, Object> table : tables) {
                sqlScript.append(generateSchemaUpdateScriptForTable(table, conn, generatorConfig));
            }
        }
        return sqlScript.toString();
    }

    private String generateSchemaUpdateScriptForTable(Map<String, Object> table, Connection conn, Map<String, Object> generatorConfig) throws SQLException {
        String tableName = (String) table.get("tableName");
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet rs = metaData.getTables(null, null, tableName, null);

        if (rs.next()) {
            return generateAlterTableScript(table, metaData, generatorConfig);
        } else {
            return generateCreateTableSql(table, (Map<String, String>) generatorConfig.get("type-mapping"));
        }
    }

    private String generateAlterTableScript(Map<String, Object> table, DatabaseMetaData metaData, Map<String, Object> generatorConfig) throws SQLException {
        String tableName = (String) table.get("tableName");
        StringBuilder script = new StringBuilder("-- Updating table '").append(tableName).append("'\n");
        
        Map<String, Map<String, String>> existingColumnsMap = getExistingColumns(metaData, tableName);
        List<Map<String, String>> desiredColumnsList = (List<Map<String, String>>) table.get("columns");
        Set<String> desiredColumnNames = desiredColumnsList.stream().map(c -> toSnakeCase(c.get("javaName"))).collect(Collectors.toSet());

        List<String> columnsToDrop = existingColumnsMap.keySet().stream()
            .filter(existingCol -> !desiredColumnNames.contains(existingCol) && !PROTECTED_COLUMNS.contains(existingCol))
            .collect(Collectors.toList());
        
        script.append(generateDropColumnStatements(tableName, columnsToDrop));

        List<Map<String, String>> columnsToAdd = desiredColumnsList.stream()
            .filter(c -> !existingColumnsMap.containsKey(toSnakeCase(c.get("javaName"))))
            .collect(Collectors.toList());
        
        script.append(generateAddColumnStatements(tableName, columnsToAdd, existingColumnsMap.keySet(), desiredColumnsList, (Map<String, String>) generatorConfig.get("type-mapping")));

        if (columnsToAdd.isEmpty() && columnsToDrop.isEmpty()) {
            script.append("-- Table '").append(tableName).append("' structure is up to date.\n");
        }
        return script.toString();
    }

    private String generateDropColumnStatements(String tableName, List<String> columnsToDrop) {
        StringBuilder script = new StringBuilder();
        for (String columnName : columnsToDrop) {
            script.append("ALTER TABLE `").append(tableName).append("` DROP COLUMN `").append(columnName).append("`;\n");
        }
        return script.toString();
    }

    private String generateAddColumnStatements(String tableName, List<Map<String, String>> columnsToAdd, Set<String> existingColumnNames, List<Map<String, String>> allDesiredColumns, Map<String, String> typeMapping) {
        if (columnsToAdd.isEmpty()) {
            return "";
        }
        StringBuilder script = new StringBuilder();
        String lastKnownColumn = "id"; 
        List<String> desiredColumnsInOrder = allDesiredColumns.stream().map(c -> toSnakeCase(c.get("javaName"))).collect(Collectors.toList());
        
        for (int i = desiredColumnsInOrder.size() - 1; i >= 0; i--) {
            String colName = desiredColumnsInOrder.get(i);
            if (existingColumnNames.contains(colName)) {
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
            script.append("ALTER TABLE `").append(tableName).append("` ADD COLUMN `").append(columnName)
                  .append("` ").append(dbType)
                  .append(" COMMENT '").append(comment).append("'")
                  .append(" AFTER `").append(lastKnownColumn).append("`;\n");
            lastKnownColumn = columnName; 
        }
        return script.toString();
    }
    
    private void saveSqlScript(String sqlScript) throws IOException {
        File sqlFile = new File(project.getBasedir(), "src/main/resources/" + SCHEMA_SQL_FILE);
        sqlFile.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(sqlFile), StandardCharsets.UTF_8)) {
            writer.write(sqlScript);
            getLog().info(LOG_PREFIX + "SQL 脚本已生成: " + sqlFile.getAbsolutePath());
        }
    }

    private void executeSqlScript(String sqlScript, Map<String, Object> generatorConfig) {
        getLog().info(LOG_PREFIX + "--- 正在执行 SQL 脚本到数据库 ---");
        Connection executionConn = null;
        try {
            executionConn = getDbConnection(generatorConfig);
            executionConn.setAutoCommit(false); 
            
            String executableScript = Arrays.stream(sqlScript.split("\n"))
                                            .filter(line -> !line.trim().startsWith("--"))
                                            .collect(Collectors.joining("\n"));

            try (Statement stmt = executionConn.createStatement()) {
                for (String sql : executableScript.split(";")) {
                    if (sql != null && !sql.trim().isEmpty()) {
                        getLog().info(LOG_PREFIX + "执行: " + sql.trim());
                        stmt.execute(sql.trim());
                    }
                }
            }
            
            executionConn.commit(); 
            getLog().info(LOG_PREFIX + "✅ SQL 脚本执行成功并已提交!");
        } catch (Exception e) {
            getLog().error(LOG_PREFIX + "SQL 脚本执行失败! 正在回滚事务...", e);
            if (executionConn != null) {
                try {
                    executionConn.rollback(); 
                    getLog().info(LOG_PREFIX + "事务已回滚。");
                } catch (SQLException rollbackEx) {
                    getLog().error(LOG_PREFIX + "事务回滚失败!", rollbackEx);
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

    private Connection getDbConnection(Map<String, Object> generatorConfig) throws SQLException, ClassNotFoundException {
        // [MODIFIED] Cast to Map<String, Object> and convert values to String to prevent ClassCastException
        Map<String, Object> jdbc = (Map<String, Object>) generatorConfig.get("jdbc");
        String url = String.valueOf(jdbc.get("url"));
        String username = String.valueOf(jdbc.get("username"));
        String password = String.valueOf(jdbc.get("password"));
        String driver = String.valueOf(jdbc.get("driver"));
        
        Class.forName(driver);
        return DriverManager.getConnection(url, username, password);
    }

    private Map<String, Map<String, String>> getExistingColumns(DatabaseMetaData metaData, String tableName) throws SQLException {
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
            sb.append("  `").append(columnName).append("` ").append(dbType).append(" COMMENT '").append(comment).append("',\n");
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

    private void processTemplate(String templateName, Map<String, Object> dataModel, String outputPath) throws Exception {
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            Template template = freemarkerCfg.getTemplate(templateName);
            template.process(dataModel, writer);
            getLog().info(LOG_PREFIX + "已生成文件: " + outputPath);
        }
    }

    private String getJavaOutputPath(Map<String, Object> generatorConfig, String packageKey, String fileName) {
        Map<String, Object> packageConf = (Map<String, Object>) generatorConfig.get("package");
        String basePackage = (String) packageConf.get(PACKAGE_KEY_BASE);
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
