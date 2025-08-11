package com.web_game.Notification_Service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.web_game"})
@EnableJpaRepositories(basePackages = "com.web_game.Notification_Service.Repository")
@EnableFeignClients(basePackages = "com.web_game.Notification_Service.Client")
@EntityScan(basePackages = "com.web_game.common.Entity")
public class NotificationServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}
}