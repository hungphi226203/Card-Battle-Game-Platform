package com.web_game.Authentication_Service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.web_game.common.Entity"})
@EnableJpaRepositories(basePackages = "com.web_game.Authentication_Service.Repository")
@ComponentScan(basePackages = {
		"com.web_game.Authentication_Service",
		"com.web_game.common"  // để load cả các bean/service trong common nếu có
})
public class AuthenticationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthenticationServiceApplication.class, args);
	}

}