package com.portal.openshiftjenkins.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

	@GetMapping("/")
	public String root() {
		return "App is running! Hello root - from Kyle Sy";
	}
	
	@GetMapping("/hello")
	public String ping() {
		return "Openshift Pipe Line Testing Hello ping - from Kyle Sy";
	}
}
