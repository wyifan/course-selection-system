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

import java.util.List;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Mojo(name = "crud-generator", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Slf4j
public class CrudGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "configFile", defaultValue = "setting.yml")
    private String configFile;

    @Parameter(property = "tableDefinitionFile", defaultValue = "table-definitions.json")
    private String tableDefinitionFile = "table-definitions.json";

    @Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
    private File basedir;

    @Parameter(property = "templateDirectory", defaultValue = "/src/main/resources/templates/")
    private String templateDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            generateCodes();
            getLog().info("CRUD code generation completed successfully.");

        } catch (Exception e) {
            throw new MojoExecutionException("Error executing CRUD generator", e);
        }
    }

    private void generateCodes() {
        // Construct the full path to the templates directory
        File templateDir = new File(basedir, templateDirectory);

        // 从 yml 读取配置
        ConfigFromYml config = ConfigFromYml.loadFromYaml(configFile);

        // 模式一：基于 JSON 定义表
        if ("definition".equalsIgnoreCase("definition")) {
            try {
                List<TableMeta> tables = JsonTableLoader.loadFromJson(tableDefinitionFile, config);

                log.info("Loaded table definitions: " + tables.size() + " tables found.");

                SqlExecutor executor = new SqlExecutor(config);
                TemplateEngine engine = new TemplateEngine(templateDir, config);

                engine.generateBaseEntity(); // 先生成 BaseEntity

                for (TableMeta table : tables) {
                    executor.executeCreateOrUpdateTable(table);
                    engine.generateAll(table);
                }

                // generate application.yml
                engine.ensureMybatisPlusConfig();
            } catch (Exception e) {
                log.error("Error generating codes", e);
                // 这里可以抛出异常，或者记录日志
                e.printStackTrace();
            }
        }
    }

}
