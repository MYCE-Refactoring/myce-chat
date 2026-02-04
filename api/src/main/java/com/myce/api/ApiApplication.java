package com.myce.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {
		"com.myce.api",      // api 모듈
		"com.myce.domain",   // domain 모듈 (Repository, Config 등)
		"com.myce.common"    // common 모듈 (Exception, Util 등)
})
@EnableMongoRepositories(basePackages = "com.myce.domain.repository")
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
