package com.yifan.log_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;


@SpringBootApplication
@EnableErukaClient
public class LogServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LogServiceApplication.class, args);
	}

}
