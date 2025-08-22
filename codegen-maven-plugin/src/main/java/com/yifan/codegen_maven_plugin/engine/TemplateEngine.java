package com.yifan.codegen_maven_plugin.engine;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.cache.ClassTemplateLoader;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.yifan.codegen_maven_plugin.entity.TableMeta;
import com.yifan.codegen_maven_plugin.config.ConfigFromYml;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TemplateEngine {
    private final Configuration cfg;
    private final ConfigFromYml config;

    public TemplateEngine(ConfigFromYml config) throws Exception {
        cfg = new Configuration(Configuration.VERSION_2_3_33);
        // cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates");
        ClassTemplateLoader templateLoader = new ClassTemplateLoader(getClass(), "/templates");
        cfg.setTemplateLoader(templateLoader);

        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setDefaultEncoding("UTF-8");

        this.config = config;
    }

    // public void generateEntity(TableMeta table, GenerateConfig config) throws
    // Exception {
    // Template template = cfg.getTemplate("Entity.ftl");
    // File outputDir = new File(config.getOutputDir() + "/entity");
    // if (!outputDir.exists())
    // outputDir.mkdirs();
    // File outputFile = new File(outputDir, table.getEntityName() + ".java");

    // try (Writer writer = new FileWriter(outputFile)) {
    // template.process(table, writer);
    // }
    // }

    /**
     * 生成 BaseEntity
     */
    public void generateBaseEntity() {
        try {
            Template template = cfg.getTemplate("baseEntity.ftl");

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
    public void generateAll(TableMeta table) {
        generateFromTemplate("entity.ftl", config.getEntityPackage(),
                table.getEntityName() + ".java", table);

        generateFromTemplate("mapper.ftl", config.getMapperPackage(),
                table.getEntityName() + "Mapper.java", table);

        generateFromTemplate("service.ftl", config.getServicePackage(),
                table.getEntityName() + "Service.java", table);

        generateFromTemplate("serviceImpl.ftl", config.getServicePackage() + ".impl",
                table.getEntityName() + "ServiceImpl.java", table);

        generateFromTemplate("controller.ftl", config.getControllerPackage(),
                table.getEntityName() + "Controller.java", table);

        // mapper.xml 放 resources
        generateFromTemplate("mapperXml.ftl", "src/main/resources/mapper",
                table.getEntityName() + "Mapper.xml", table, false);
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
            TableMeta table) {
        generateFromTemplate(templateName, packageNameOrDir, fileName, table, true);
    }

    private void generateFromTemplate(String templateName,
            String packageNameOrDir,
            String fileName,
            TableMeta table,
            boolean isJavaFile) {
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
