package com.yifan.codegen_maven_plugin.mojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * 读取 setting.yml 配置的 Mojo.
 * 优先读取执行项目的 src/main/resources/setting.yml,
 * 如果不存在，则读取插件自带的默认 setting.yml.
 */
@Mojo(name = "read-config")
public class MyConfigMojo extends AbstractMojo {

    /**
     * 注入当前执行的 Maven 项目对象，以便获取项目路径.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        InputStream inputStream = null;
        String configSource;

        try {
            // 1. 构造用户项目中的配置文件路径
            File userConfigFile = new File(project.getBasedir(), "src/main/resources/test.yml");

            // 2. 检查用户配置文件是否存在
            if (userConfigFile.exists() && userConfigFile.isFile()) {
                configSource = "项目本地文件 (" + userConfigFile.getAbsolutePath() + ")";
                getLog().info("发现并使用项目本地配置文件: " + userConfigFile.getAbsolutePath());
                inputStream = new FileInputStream(userConfigFile);
            } else {
                // 3. 如果不存在，则回退到插件自带的默认配置文件
                configSource = "插件默认文件 (classpath:test.yml)";
                getLog().info("未找到项目本地配置文件，使用插件默认配置。");
                inputStream = getClass().getClassLoader().getResourceAsStream("test.yml");

                if (inputStream == null) {
                    throw new MojoExecutionException("致命错误: 插件默认配置文件 'test.yml' 在jar包中丢失!");
                }
            }

            // 4. 使用 Jackson 解析 YAML
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            // 将 YAML 内容解析为 Map 结构
            Map<String, Object> configMap = objectMapper.readValue(inputStream, Map.class);

            // 5. 打印解析结果
            getLog().info("-------------------- 配置内容 (" + configSource + ") --------------------");
            printMap(configMap, 0);
            getLog().info("--------------------------------------------------------------------");

        } catch (Exception e) {
            throw new MojoExecutionException("读取或解析配置文件时出错", e);
        } finally {
            // 确保输入流被关闭
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    getLog().error("关闭文件输入流失败", e);
                }
            }
        }
    }

    /**
     * 递归打印 Map 内容，使其格式化输出.
     */
    private void printMap(Map<String, Object> map, int indentLevel) {
        String indent = new String(new char[indentLevel * 2]).replace('\0', ' ');
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                getLog().info(indent + entry.getKey() + ":");
                printMap((Map<String, Object>) entry.getValue(), indentLevel + 1);
            } else {
                getLog().info(indent + entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}
