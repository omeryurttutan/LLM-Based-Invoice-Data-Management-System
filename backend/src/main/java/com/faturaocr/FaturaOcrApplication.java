package com.faturaocr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FaturaOcrApplication {

	public static void main(String[] args) {
		SpringApplication.run(FaturaOcrApplication.class, args);
	}

}
