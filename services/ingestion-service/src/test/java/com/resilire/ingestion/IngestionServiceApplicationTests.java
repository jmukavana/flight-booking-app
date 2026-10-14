package com.resilire.ingestion;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("requires running Kafka — covered by integration tests against local infra")
class IngestionServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
