
# Maven 插件开发问题分析与解决方案

## 问题 1：用户配置文件路径错误

主要问题在于您的 `getSettingYml` 方法。查找用户本地配置文件的逻辑存在缺陷。

### 您的代码：

```java
File file = new File(project.getBuild().getOutputDirectory(), configFile);
if (!file.exists()) {
    // ... 回退逻辑
} else {
    inputStream = new FileInputStream(configFile); // 这行代码也是错误的
}
```

### 分析：

1.  **错误目录**：`project.getBuild().getOutputDirectory()` 指向 `target/classes` 目录。此目录仅在 Maven 的 `process-resources` 阶段运行后才包含文件。当您直接从命令行执行插件目标时，此阶段可能尚未运行，这意味着 `target` 目录可能为空或不存在。插件应始终在源目录（即 `src/main/resources`）中查找用户配置。

2.  **不正确的文件读取**：代码行 `inputStream = new FileInputStream(configFile);` 尝试仅使用文件名（例如 "setting.yml"）从执行 Maven 命令的根目录读取文件。这将失败，因为文件实际上位于子目录（`src/main/resources`）中。

## 问题 2：不一致的模板加载

第二个问题是 FreeMarker 模板的定位方式。

### 您的代码：

```java
// 在 Mojo 中
cfg.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "templates");

// 在 TemplateEngine.java 中
// 您有一个构造函数做同样的事情，但另一个构造函数这样做：
FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(templateDir);
cfg.setTemplateLoader(fileTemplateLoader);
```

### 分析：

1.  模板（`.ftl` 文件）是插件资源的一部分，而不是用户项目资源。因此，它们应始终从插件的类路径加载。

2.  代码行 `cfg.setClassLoaderForTemplateLoading(...)` 是正确的做法。它告诉 FreeMarker 在插件的 JAR 文件中查找 `templates` 文件夹。

3.  `FileTemplateLoader` 设计用于从文件系统上的外部目录加载模板。您的代码似乎有冲突或冗余的方式来初始化模板加载器，如果使用了错误的路径，很容易导致"找不到模板"错误。

## 解决方案：正确且健壮的文件加载逻辑

要修复这些问题，您应该采用此类插件的标准方法，这在我们一直使用的 Canvas 文档中已经使用。

### 1. 对于配置文件 (setting.yml, table-definitions.json)

您的 `getSettingYml` 方法应重写为遵循以下逻辑：

```java
private InputStream getSettingYml(String configFileName) throws IOException, MojoExecutionException {
    // 1. 构建用户配置文件的绝对路径
    File userConfigFile = new File(project.getBasedir(), "src/main/resources/" + configFileName);

    // 2. 检查用户的文件是否存在
    if (userConfigFile.exists() && userConfigFile.isFile()) {
        getLog().info("Using local configuration file: " + userConfigFile.getAbsolutePath());
        return new FileInputStream(userConfigFile);
    } else {
        // 3. 如果不存在，回退到插件的内部资源
        getLog().info("Local config not found. Using plugin's default: " + configFileName);
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configFileName);
        if (inputStream == null) {
            throw new MojoExecutionException("Could not find default config file in plugin: " + configFileName);
        }
        return inputStream;
    }
}
```

这种方法正确地使用 `project.getBasedir()` 来查找项目的根目录，并首先可靠地检查 `src/main/resources` 目录，然后再回退到插件的内部默认值。

### 2. 对于 FreeMarker 模板

您应该只有一种初始化 FreeMarker 配置的方式，并且它应始终从类路径加载。将此逻辑整合到您的 Mojo 类中。

```java
// 在您的 Mojo 的 execute() 方法或初始化方法中
Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);

// 这是您唯一需要的代码行。它正确地在插件的 JAR 文件中查找模板。
cfg.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "templates");

cfg.setDefaultEncoding("UTF-8");
```

通过进行这些更改，您的插件将正确地优先处理用户的本地文件，并可靠地找到其自己的内部模板，从而解决您面临的两个问题。
