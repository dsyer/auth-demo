package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthDemoApplicationTests {

	@Test
	void contextLoads() {
	}

	public static void main(String[] args) {
		System.setProperty("spring.main.cloud-platform", "kubernetes");
		// System.setProperty("kubernetes.service.host", "localhost:43319");
		System.setProperty("management.kubernetes.skip-ssl-validation", "true");
		SpringApplication.run(AuthDemoApplication.class);
	}

}
