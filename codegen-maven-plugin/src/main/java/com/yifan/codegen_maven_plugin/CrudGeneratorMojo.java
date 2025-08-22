package com.yifan.codegen_maven_plugin;

import com.yifan.codegen_maven_plugin.config.ConfigFromYml;
import com.yifan.codegen_maven_plugin.executor.SqlExecutor;
import com.yifan.codegen_maven_plugin.entity.TableMeta;
import com.yifan.codegen_maven_plugin.executor.JsonTableLoader;
import com.yifan.codegen_maven_plugin.engine.TemplateEngine;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import freemarker.template.Configuration;
import freemarker.template.Template;

import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.io.File;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Mojo(name = "crud-generator", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Slf4j
public class CrudGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "configFile", defaultValue = "setting.yml")
    private String configFile;

    @Parameter(property = "tableDefinitionFile", defaultValue = "table-definitions.json")
    private String tableDefinitionFile;

    @Parameter(property = "templateDirectory", defaultValue = "/src/main/resources/templates/")
    private String templateDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);

            cfg.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "templates");

            cfg.setDefaultEncoding("UTF-8");

            ConfigFromYml config = ConfigFromYml.loadFromYaml(configFile);

            List<TableMeta> tables = JsonTableLoader.loadFromJson("table-definitions.json", config);

            SqlExecutor executor = new SqlExecutor(config);

            generateBaseEntity(cfg, config); // 先生成 BaseEntity

            for (TableMeta table : tables) {
                executor.executeCreateOrUpdateTable(table);
                generateAll(cfg, config, table);
            }

            // generate application.yml
            ensureMybatisPlusConfig();

            // Create a writer to output generated content

            getLog().info("CRUD code generation completed successfully.");

        } catch (Exception e) {
            throw new MojoExecutionException("Error executing CRUD generator", e);
        }
    }

    /**
     * 生成 BaseEntity
     */
    public void generateBaseEntity(Configuration cfg, ConfigFromYml config) {
        try {
            Template template = cfg.getTemplate("BaseEntity.ftl");

            Map<String, Object> data = new HashMap<>();
            data.put("basePackage", config.getBasePackageForBaseEntity());

            String packageName = config.getBasePackageForBaseEntity();
            String className = "BaseEntity";
            String dir = "src/main/java/" + packageName.replace(".", "/");

            writeToFile(template, data, dir, className + ".java");
        } catch (Exception e) {
            throw new RuntimeException("生成 BaseEntity 失败", e);
        }
    }

    /**
     * 一次性生成所有文件
     */
    public void generateAll(Configuration cfg, ConfigFromYml config, TableMeta table) {
        generateFromTemplate("Entity.ftl", config.getEntityPackage(),
                table.getEntityName() + ".java", table, cfg, config);

        generateFromTemplate("Mapper.ftl", config.getMapperPackage(),
                table.getEntityName() + "Mapper.java", table, cfg, config);

        generateFromTemplate("Service.ftl", config.getServicePackage(),
                table.getEntityName() + "Service.java", table, cfg, config);

        generateFromTemplate("ServiceImpl.ftl", config.getServicePackage() + ".impl",
                table.getEntityName() + "ServiceImpl.java", table, cfg, config);

        generateFromTemplate("Controller.ftl", config.getControllerPackage(),
                table.getEntityName() + "Controller.java", table, cfg, config);

        // mapper.xml 放 resources
        generateFromTemplate("MapperXml.ftl", "src/main/resources/mapper",
                table.getEntityName() + "Mapper.xml", table, false, cfg, config);
    }

    public void ensureMybatisPlusConfig() throws IOException {
        File ymlFile = new File("src/main/resources/application.yml");
        if (!ymlFile.exists()) {
            // ✅ 没有就创建
            String content = "mybatis-plus:\n  mapper-locations: classpath:/mapper/**/*.xml\n";
            Files.write(ymlFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
            return;
        }

        // ✅ 已有 application.yml -> 检查是否有配置
        List<String> lines = Files.readAllLines(ymlFile.toPath(), StandardCharsets.UTF_8);
        boolean hasConfig = lines.stream().anyMatch(line -> line.contains("mybatis-plus:"));

        if (!hasConfig) {
            lines.add("");
            lines.add("mybatis-plus:");
            lines.add("  mapper-locations: classpath:/mapper/*.xml");
            Files.write(ymlFile.toPath(), lines, StandardCharsets.UTF_8);
        } else {
            boolean hasMapperLoc = lines.stream().anyMatch(line -> line.contains("mapper-locations:"));
            if (!hasMapperLoc) {
                // 插到 mybatis-plus: 节点下
                List<String> newLines = new ArrayList<>();
                for (String line : lines) {
                    newLines.add(line);
                    if (line.trim().equals("mybatis-plus:")) {
                        newLines.add("  mapper-locations: classpath:/mapper/**/*.xml");
                    }
                }
                Files.write(ymlFile.toPath(), newLines, StandardCharsets.UTF_8);
            }
        }
    }

    private void generateFromTemplate(String templateName,
            String packageNameOrDir,
            String fileName,
            TableMeta table, Configuration cfg, ConfigFromYml config) {
        generateFromTemplate(templateName, packageNameOrDir, fileName, table, true, cfg, config);
    }

    private void generateFromTemplate(String templateName,
            String packageNameOrDir,
            String fileName,
            TableMeta table,
            boolean isJavaFile, Configuration cfg, ConfigFromYml config) {
        try {
            Template template = cfg.getTemplate(templateName);

            Map<String, Object> data = new HashMap<>();
            data.put("basePackage", config.getBasePackage());
            data.put("useBaseEntity", true);
            data.put("config", config);
            data.put("table", table);
            data.put("columns", table.getColumns());
            data.put("entityName", table.getEntityName());
            data.put("tableName", table.getTableName());

            String dir;
            if (isJavaFile) {
                dir = "src/main/java/" + packageNameOrDir.replace(".", "/");
            } else {
                dir = packageNameOrDir; // 直接是目录
            }

            writeToFile(template, data, dir, fileName);
        } catch (Exception e) {
            throw new RuntimeException("生成 " + templateName + " 失败", e);
        }
    }

    private void writeToFile(Template template, Map<String, Object> data, String dir, String fileName) {
        try {
            File folder = new File(dir);
            if (!folder.exists())
                folder.mkdirs();

            File targetFile = new File(folder, fileName);
            try (Writer out = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
                template.process(data, out);
            }
        } catch (Exception e) {
            throw new RuntimeException("写文件失败: " + fileName, e);
        }
    }

}
