package com.yifan.codegen_maven_plugin.mojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
 * 读取 table.json 配置的 Mojo.
 * 优先读取执行项目的 src/main/resources/table.json,
 * 如果不存在，则读取插件自带的默认 table.json.
 */
@Mojo(name = "read-json")
public class ReadJsonMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        InputStream inputStream = null;
        String configSource;

        try {
            // 1. 构造用户项目中的配置文件路径
            File userConfigFile = new File(project.getBasedir(), "src/main/resources/table.json");

            // 2. 检查用户配置文件是否存在
            if (userConfigFile.exists() && userConfigFile.isFile()) {
                configSource = "项目本地文件 (" + userConfigFile.getAbsolutePath() + ")";
                getLog().info("发现并使用项目本地配置文件: " + userConfigFile.getAbsolutePath());
                inputStream = new FileInputStream(userConfigFile);
            } else {
                // 3. 如果不存在，则回退到插件自带的默认配置文件
                configSource = "插件默认文件 (classpath:table.json)";
                getLog().info("未找到项目本地配置文件，使用插件默认配置。");
                inputStream = getClass().getClassLoader().getResourceAsStream("table.json");
                if (inputStream == null) {
                    throw new MojoExecutionException("致命错误: 插件默认配置文件 'table.json' 在jar包中丢失!");
                }
            }

            // 4. 使用 Jackson 解析 JSON
            ObjectMapper objectMapper = new ObjectMapper();
            // 为了美化输出，启用缩进
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            // 将 JSON 解析为 Map
            Map<String, Object> configMap = objectMapper.readValue(inputStream, Map.class);

            // 5. 将解析后的 Map 对象重新格式化为漂亮的 JSON 字符串并打印
            String prettyJson = objectMapper.writeValueAsString(configMap);
            getLog().info("-------------------- 配置内容 (" + configSource + ") --------------------");
            // 逐行打印，避免日志前缀弄乱格式
            for (String line : prettyJson.split("\n")) {
                getLog().info(line);
            }
            getLog().info("--------------------------------------------------------------------");

        } catch (Exception e) {
            throw new MojoExecutionException("读取或解析 table.json 时出错", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    getLog().error("关闭文件输入流失败", e);
                }
            }
        }
    }
}