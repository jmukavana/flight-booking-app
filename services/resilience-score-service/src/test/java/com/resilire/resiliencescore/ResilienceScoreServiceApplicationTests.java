package com.resilire.resiliencescore;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("requires running Redis + ML serving — covered by integration tests against local infra")
class ResilienceScoreServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
