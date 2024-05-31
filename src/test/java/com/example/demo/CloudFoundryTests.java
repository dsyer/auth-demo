package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CloudFoundryTests {

	@Test
	void contextLoads() {
	}

	public static void main(String[] args) {
		System.setProperty("spring.main.cloud-platform", "cloud_foundry");
		// System.setProperty("debug", "true");
		SpringApplication.run(AuthDemoApplication.class);
	}

}
