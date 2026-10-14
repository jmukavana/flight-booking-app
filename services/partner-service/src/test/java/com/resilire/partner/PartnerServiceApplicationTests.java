package com.resilire.partner;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("requires running Postgres + Keycloak — covered by integration tests against local infra")
class PartnerServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
