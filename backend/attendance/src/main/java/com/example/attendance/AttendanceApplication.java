package com.example.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class AttendanceApplication {
	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(AppClock.ZONE));
		SpringApplication.run(AttendanceApplication.class, args);
	}
}
