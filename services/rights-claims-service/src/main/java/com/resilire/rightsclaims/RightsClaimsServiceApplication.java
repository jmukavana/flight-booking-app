package com.resilire.rightsclaims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RightsClaimsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RightsClaimsServiceApplication.class, args);
	}

}
