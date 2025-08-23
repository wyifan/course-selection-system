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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mojo(name = "generate-crud")
public class GenerateCrudMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    private Configuration freemarkerCfg;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            // 1. 初始化FreeMarker
            initFreeMarker();

            // 2. 加载配置
            Map<String, Object> settings = loadConfig("setting.yml", true);
            Map<String, Object> tableDefinitions = loadConfig("table-definitions.json", false);

            // 3. 准备数据模型
            Map<String, Object> rootDataModel = new HashMap<>();
            rootDataModel.put("generator", settings.get("generator"));

            getLog().info("代码生成配置: " + settings);
            getLog().info("表格定义: " + tableDefinitions);
            getLog().info("数据信息: " + rootDataModel);
            getLog().info("开始生成代码...");

            // 4. 生成一次性的基础文件 (如 BaseEntity)
            generateBaseFiles(rootDataModel);

            List<Map<String, Object>> tables = (List<Map<String, Object>>) tableDefinitions.get("tables");
            if (tables == null || tables.isEmpty()) {
                getLog().warn("table-definitions.json 中没有找到任何表格定义，跳过代码生成。");
                return;
            }

            // 4. 遍历每个表并生成代码
            for (Map<String, Object> table : tables) {
                getLog().info("正在为表 '" + table.get("tableName") + "' 生成代码...");
                rootDataModel.put("table", table);
                generateFiles(rootDataModel);
            }

            getLog().info("✅ 所有代码文件生成成功!");

        } catch (Exception e) {
            getLog().error("代码生成失败", e);
            throw new MojoExecutionException("代码生成过程中发生错误", e);
        }
    }

    private void initFreeMarker() {
        freemarkerCfg = new Configuration(Configuration.VERSION_2_3_31);
        // 设置从classpath的/templates目录加载模板
        freemarkerCfg.setClassForTemplateLoading(this.getClass(), "/templates");
        freemarkerCfg.setDefaultEncoding("UTF-8");
        freemarkerCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarkerCfg.setLogTemplateExceptions(false);
        freemarkerCfg.setWrapUncheckedExceptions(true);
    }

    private Map<String, Object> loadConfig(String fileName, boolean isYaml) throws Exception {
        InputStream inputStream = null;
        File userFile = new File(project.getBasedir(), "src/main/resources/" + fileName);

        if (userFile.exists() && userFile.isFile()) {
            getLog().info("使用项目本地配置文件: " + userFile.getAbsolutePath());
            inputStream = new FileInputStream(userFile);
        } else {
            getLog().info("未找到项目本地配置文件 '" + fileName + "', 使用插件默认配置。");
            inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
            if (inputStream == null) {
                throw new MojoExecutionException("插件默认配置文件 '" + fileName + "' 丢失!");
            }
        }

        ObjectMapper mapper = isYaml ? new ObjectMapper(new YAMLFactory()) : new ObjectMapper();
        try (InputStream in = inputStream) {
            return mapper.readValue(in, Map.class);
        }
    }

    private void generateBaseFiles(Map<String, Object> dataModel) throws Exception {
        getLog().info("正在生成基础文件...");
        Map<String, Object> generatorConfig = (Map<String, Object>) dataModel.get("generator");
        Map<String, String> packageConfig = (Map<String, String>) generatorConfig.get("package");

        String baseEntityPackage = packageConfig.get("baseEntity");
        if (baseEntityPackage == null || baseEntityPackage.isEmpty()) {
            getLog().warn("setting.yml 中未配置 'package.baseEntity' 路径, 跳过 BaseEntity 生成。");
            return;
        }

        String outputPath = getOutputPath(baseEntityPackage, "BaseEntity.java");
        processTemplate("baseEntity.ftl", dataModel, outputPath);
    }

    private void generateFiles(Map<String, Object> dataModel) throws Exception {
        Map<String, Object> generatorConfig = (Map<String, Object>) dataModel.get("generator");
        Map<String, String> packageConfig = (Map<String, String>) generatorConfig.get("package");
        Map<String, Object> tableConfig = (Map<String, Object>) dataModel.get("table");
        String entityName = (String) tableConfig.get("entityName");

        // 定义模板和输出路径的映射关系
        Map<String, String> templateOutputMap = new HashMap<>();
        templateOutputMap.put("entity.ftl", getOutputPath(packageConfig.get("entity"), entityName + ".java"));
        templateOutputMap.put("service.ftl",
                getOutputPath(packageConfig.get("service"), "I" + entityName + "Service.java"));
        templateOutputMap.put("controller.ftl",
                getOutputPath(packageConfig.get("controller"), entityName + "Controller.java"));
        templateOutputMap.put("mapper.ftl", getOutputPath(packageConfig.get("mapper"), entityName + "Mapper.java"));
        templateOutputMap.put("mapperxml.ftl", getResourceOutputPath("mapper", entityName + "Mapper.xml"));

        for (Map.Entry<String, String> entry : templateOutputMap.entrySet()) {
            String templateName = entry.getKey();
            String outputPath = entry.getValue();

            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs(); // 确保目录存在

            try (Writer writer = new FileWriter(outputFile)) {
                Template template = freemarkerCfg.getTemplate(templateName);
                template.process(dataModel, writer);
                getLog().info("  -> 已生成文件: " + outputPath);
            }
        }
    }

    private void processTemplate(String templateName, Map<String, Object> dataModel, String outputPath)
            throws Exception {
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs(); // 确保目录存在

        try (Writer writer = new FileWriter(outputFile)) {
            Template template = freemarkerCfg.getTemplate(templateName);
            template.process(dataModel, writer);
            getLog().info("  -> 已生成文件: " + outputPath);
        }
    }

    private String getOutputPath(String packageName, String fileName) {
        String packagePath = packageName.replace('.', '/');
        return project.getBasedir().getAbsolutePath() + "/src/main/java/" + packagePath + "/" + fileName;
    }

    private String getResourceOutputPath(String dir, String fileName) {
        return project.getBasedir().getAbsolutePath() + "/src/main/resources/" + dir + "/" + fileName;
    }
}