package com.busbus.backend;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;

// An empty key disables polling, so this test never calls the real NTA API, even if .env has a key.
@SpringBootTest(properties = "nta.api-key=")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
