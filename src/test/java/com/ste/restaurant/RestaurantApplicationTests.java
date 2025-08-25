package com.ste.restaurant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {RestaurantApplication.class, TestConfig.class})
@ActiveProfiles("test")
class RestaurantApplicationTests {

	@Test
	void contextLoads() {
	}

}
