# MyBatis Plus CRUD 代码生成器 Maven 插件

## 简介

本插件是一个功能强大的开发辅助工具，旨在极大提升基于 **Spring Boot + MyBatis Plus** 技术栈的项目开发效率。它通过自动化处理项目初始化、代码生成和数据库结构同步等繁琐任务，让开发者可以更专注于业务逻辑的实现。

插件的核心思想是“配置驱动”，您只需要维护两个核心配置文件（`setting.yml` 和 `table-definitions.json`），即可一键完成大部分基础工作。

---

## 核心功能

* **🚀 项目智能初始化**
    * **POM 依赖管理**：自动检查并添加项目所需的核心依赖（如 `spring-boot-starter-web`, `mybatis-plus-spring-boot3-starter`, `mysql-connector-j`, `lombok`）。
    * **智能版本处理**：能够识别项目是否有 Spring Boot 父 POM，并据此决定是添加带版本的依赖还是无版本的依赖。
    * **构建配置**：自动添加并配置 `maven-compiler-plugin`（设置 Java 17）和 `spring-boot-maven-plugin`（含 Lombok 排除）。
    * **`basePackage` 自动推断**：自动从项目的 `groupId` 和 `artifactId` 推断出最合理的根包名，减少配置错误。

* **✍️ 全方位代码生成**
    * 基于 **FreeMarker** 模板引擎，可轻松定制生成的代码风格。
    * 一键生成包括 **Entity**, **DTO**, **Service (Interface)**, **ServiceImpl**, **Controller**, **Mapper (Interface)**, 和 **Mapper.xml** 在内的全套 CRUD 代码。
    * 自动生成通用的 `BaseEntity`，包含ID、创建/更新信息、逻辑删除和版本号等公共字段。

* **🔄 数据库 Schema 同步**
    * **SQL 脚本生成**：根据 `table-definitions.json` 的定义，自动生成 `CREATE TABLE` 或 `ALTER TABLE` 语句，并保存到 `src/main/resources/sql/schema.sql`。
    * **智能差异比对**：连接数据库，比对 `table-definitions.json` 与实际表结构，精确生成差异化更新脚本。
    * **字段新增**：新增的字段会自动插入到业务字段区域的末尾（`created_by` 之前），而不是表末尾，保持结构清晰。
    * **字段删除**：当您从配置中移除字段时，插件会自动生成 `DROP COLUMN` 语句。
    * **一键执行**：提供参数，可选择将生成的 SQL 脚本直接在目标数据库中执行。

* **⚙️ 自动化配置文件管理**
    * 自动创建或更新 `src/main/resources/application.yml` 文件。
    * 将 `setting.yml` 中配置的数据库连接信息（URL, username, password, driver）自动写入 `application.yml`。
    * 自动添加或修正 MyBatis Plus 的 `mapper-locations` 配置，并为其增加清晰的注释。

---

## 如何使用

### 步骤 1: 安装插件

首先，您需要将本插件安装到您本地的 Maven 仓库。

```bash
# 进入插件项目根目录 (crud-generator-plugin)
mvn clean install
```

### 步骤 2: 在您的项目中使用插件

在您需要生成代码的目标项目（例如一个新建的 Spring Boot 项目）的 `pom.xml` 文件中，添加此插件的引用。

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.yifan.codegen</groupId>
            <artifactId>crud-generator-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </plugin>
    </plugins>
</build>
```

### 步骤 3: 创建配置文件

在目标项目的 `src/main/resources/` 目录下，创建以下两个核心配置文件。如果这两个文件不存在，插件会使用自带的默认配置。

**1. `setting.yml` (全局设置)**

```yaml
generator:
  # 数据库连接信息
  jdbc:
    url: jdbc:mysql://localhost:3306/your_db?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
    username: root
    password: your_password
    driver: com.mysql.cj.jdbc.Driver
  
  # 包名配置 (basePackage 会被 pom.xml 自动覆盖)
  package:
    basePackage: com.yourcompany.yourproject # 建议保留，作为 pom.xml 读取失败时的备用
    entity: model.entity
    dto: model.dto
    mapper: repository
    service: service
    serviceImpl: service.impl
    controller: controller
    baseEntity: model.base
    
  # Java 类型到数据库类型的映射
  type-mapping:
    String: VARCHAR(255)
    Integer: INT
    Long: BIGINT
    Double: DOUBLE
    BigDecimal: DECIMAL(18,2)
    LocalDate: DATE
    LocalDateTime: DATETIME
    Boolean: TINYINT(1)
```

**2. `table-definitions.json` (表结构定义)**

```json
{
  "tables": [
    {
      "tableName": "user",
      "entityName": "User",
      "useBaseEntity": true,
      "columns": [
        { "javaName": "username", "javaType": "String", "comment": "用户名" },
        { "javaName": "nickName", "javaType": "String", "comment": "用户昵称" },
        { "javaName": "age", "javaType": "Integer", "comment": "年龄" },
        { "javaName": "email", "javaType": "String", "comment": "邮箱" }
      ]
    },
    {
      "tableName": "product",
      "entityName": "Product",
      "useBaseEntity": true,
      "columns": [
        { "javaName": "productName", "javaType": "String", "comment": "产品名称" },
        { "javaName": "price", "javaType": "BigDecimal", "comment": "价格" }
      ]
    }
  ]
}
```

### 步骤 4: 执行插件

打开终端，在**目标项目**的根目录下执行以下命令。

```bash
mvn com.example.codegen:crud-generator-plugin:generate
```

执行后，插件会自动完成 **POM 初始化**、**`application.yml` 配置**、**代码生成** 和 **`schema.sql` 生成** 的所有工作。

---

## 执行命令详解

* **标准执行（推荐）**:
  此命令会执行所有代码生成和文件配置任务，并生成 `schema.sql` 脚本，但**不会**自动执行 SQL。

  ```bash
  mvn com.yifan.codegen:crud-generator-plugin:generate
  ```

* **执行 SQL 同步**:
  在标准执行的基础上，增加 `-DexecuteSql=true` 参数，插件会在生成 `schema.sql` 后，立即将其中的 `CREATE` 和 `ALTER` 语句在目标数据库中执行。

  > **警告**: 此操作会直接修改您的数据库结构，请在开发和测试环境中谨慎使用！

  ```bash
  mvn com.yifan.codegen:crud-generator-plugin:generate -DexecuteSql=true
  ```

---

## 定制与优化

本插件具有高度的可扩展性，您可以通过以下方式进行定制：

1. **修改代码模板**:
   所有代码模板都位于插件项目的 `src/main/resources/templates/` 目录下（例如 `entity.ftl`, `controller.ftl`）。您可以直接修改这些 **FreeMarker** 模板文件，来改变生成代码的结构、注解或风格。修改后，重新 `mvn clean install` 安装插件即可生效。

2. **扩展依赖管理**:
   如果您希望插件能自动添加更多的默认依赖（例如 `spring-boot-starter-validation`），只需在 `GenerateCrudMojo.java` 的 `getRequiredDependencies()` 方法中，按照现有格式添加新的 `DependencyInfo` 实例即可。

3. **支持更多数据库**:
   当前的 SQL 生成逻辑是针对 MySQL 的。您可以通过修改 `generateCreateTableSql` 和 `generateAlterTableScript` 方法，增加对 PostgreSQL, Oracle 等其他数据库方言的支持。
