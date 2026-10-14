package com.resilire.apigateway;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("requires running Keycloak + Redis — covered by integration tests against local infra")
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
