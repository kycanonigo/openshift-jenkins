package com.portal.openshiftjenkins.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

	@GetMapping("/")
	public String root() {
		return "Hello. JB here. TESTTESTTESTTESTTEST";
	}
	
	@GetMapping("/hello")
	public String ping() {
		return "Hello. Kyle here";
	}
}
