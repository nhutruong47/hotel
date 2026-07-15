package com.hsf.hotel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotelApplication {

	private static final Logger log = LoggerFactory.getLogger(HotelApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(HotelApplication.class, args);
		log.info("Hotel management application started successfully");
	}

}