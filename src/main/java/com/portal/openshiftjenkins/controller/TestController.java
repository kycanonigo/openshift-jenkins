package com.portal.openshiftjenkins.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

	@GetMapping("/")
	public String root() {
		return "ROOT - AKO SI JOHN BRYAN";
	}
	
	@GetMapping("/hello")
	public String ping() {
		return "HELLO - AKO SI KYLE CANONIGO";
	}
}
