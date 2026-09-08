package com.M198.Majorproject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=test-secret-that-is-long-enough-32")
class MajorprojectApplicationTests {

	@Test
	void contextLoads() {
	}

}
