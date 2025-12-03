package com.example.demo;

import com.example.demo.security.JwtTokenUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.Clock;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TestProject01Application {

	public static void main(String[] args) {
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
