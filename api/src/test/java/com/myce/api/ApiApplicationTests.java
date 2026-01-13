package com.myce.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
		"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
class ApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
