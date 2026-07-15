package com.hsf.hotel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Boots the application context.
 *
 * The test profile uses H2 in-memory, but the user machine may have a
 * {@code SPRING_DATASOURCE_URL} environment variable pointing at Postgres.
 * Environment variables win over properties files, so we explicitly clear
 * the relevant variables here before the Spring context starts.
 */
@SpringBootTest
@ActiveProfiles("test")
class HotelApplicationTests {

	@DynamicPropertySource
	static void overrideDatasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url",
				() -> "jdbc:h2:mem:hoteldb;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=USER");
		registry.add("spring.datasource.username", () -> "sa");
		registry.add("spring.datasource.password", () -> "");
		registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
		registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
	}

	@Test
	void contextLoads() {
	}

}