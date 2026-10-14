package com.resilire.booking;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("requires running Postgres + Kafka + Keycloak — covered by integration tests against local infra")
class BookingServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
