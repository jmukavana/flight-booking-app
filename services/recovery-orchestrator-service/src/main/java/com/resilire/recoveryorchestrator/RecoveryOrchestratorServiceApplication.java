package com.resilire.recoveryorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class RecoveryOrchestratorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecoveryOrchestratorServiceApplication.class, args);
	}

}
