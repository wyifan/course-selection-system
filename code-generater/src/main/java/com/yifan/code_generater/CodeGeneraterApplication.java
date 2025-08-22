package com.yifan.code_generater;

import java.util.List;

import com.yifan.code_generater.engine.TemplateEngine;
import com.yifan.code_generater.entity.TableMeta;
import com.yifan.code_generater.executor.JsonTableLoader;
import com.yifan.code_generater.executor.SqlExecutor;

import com.yifan.code_generater.config.ConfigFromYml;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CodeGeneraterApplication {

	public static void main(String[] args) {
		// SpringApplication.run(CodeGeneraterApplication.class, args);

		// 假设当前目录就是 Spring Boot 项目根目录
		String projectPath = System.getProperty("user.dir");
		log.info("当前项目路径: {}", projectPath);

		// 从 yml 读取配置
		ConfigFromYml config = ConfigFromYml.loadFromYaml("setting.yml");

		// 模式一：基于 JSON 定义表
		if ("definition".equalsIgnoreCase("definition")) {
			try {
				List<TableMeta> tables = JsonTableLoader.loadFromJson("table-definitions.json", config);

				SqlExecutor executor = new SqlExecutor(config);
				TemplateEngine engine = new TemplateEngine(config);

				engine.generateBaseEntity(); // 先生成 BaseEntity

				for (TableMeta table : tables) {
					executor.executeCreateOrUpdateTable(table);
					engine.generateAll(table);
				}

				// generate application.yml
				engine.ensureMybatisPlusConfig();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
