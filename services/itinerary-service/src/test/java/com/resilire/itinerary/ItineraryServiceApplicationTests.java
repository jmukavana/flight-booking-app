package com.resilire.itinerary;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("requires running Kafka + Redis + Keycloak — covered by integration tests against local infra")
class ItineraryServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
