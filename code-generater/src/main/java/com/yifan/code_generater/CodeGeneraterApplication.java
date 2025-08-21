package com.yifan.code_generater;

import java.util.List;

import com.yifan.code_generater.engine.TemplateEngine;
import com.yifan.code_generater.entity.TableMeta;
import com.yifan.code_generater.executor.JsonTableLoader;
import com.yifan.code_generater.executor.SqlExecutor;

import com.yifan.code_generater.config.ConfigFromYml;

public class CodeGeneraterApplication {

	public static void main(String[] args) {
		// SpringApplication.run(CodeGeneraterApplication.class, args);

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
