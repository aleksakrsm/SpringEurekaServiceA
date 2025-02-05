package com.example.servicea.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ServiceARestController {

    @Value("${service.id}")
    private String serviceId;

    @GetMapping("/helloWorld")
    public String helloWorld() {
        log.info("Hello world from Service A!");
        return "Hello world from Service A! My name is " + serviceId;
    }

}
