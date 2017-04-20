package com.seastar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
// 部署到tomcat中时，使用SpringBootServletInitializer
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}


}
