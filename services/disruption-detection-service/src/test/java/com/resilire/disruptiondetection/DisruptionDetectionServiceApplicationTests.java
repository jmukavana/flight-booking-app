package com.resilire.disruptiondetection;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("requires running Kafka + Postgres — covered by integration tests against local infra")
class DisruptionDetectionServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
