package com.resilire.recoveryorchestrator;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("requires running Kafka + Postgres + Keycloak — covered by integration tests against local infra")
class RecoveryOrchestratorServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
