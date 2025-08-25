package io.harness.fme.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FeatureFlagDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeatureFlagDemoApplication.class, args);
		System.out.println("Hello SpringBoot!");
	}

}
