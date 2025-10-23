package com.example.demo;

import com.example.demo.security.JwtTokenUtil;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class TestProject01Application {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
		dotenv.entries().forEach((e)->System.setProperty(e.getKey(),e.getValue()));
		SpringApplication.run(TestProject01Application.class, args);
	}

	@Bean
	public Clock systemClock(){
		return Clock.systemDefaultZone();
	}

	@Bean
	public JwtTokenUtil jwtTokenUtil(Clock clock) {
		return new JwtTokenUtil(clock);
	}
}
