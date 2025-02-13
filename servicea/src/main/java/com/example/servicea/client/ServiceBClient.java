package com.example.servicea.client;

import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(
        url = "/test/cb",
        accept = MediaType.APPLICATION_JSON_VALUE)
public interface ServiceBClient {


    @GetExchange(url = "/with-delay")
    String testCbWithDelay();

    @GetExchange(url = "/no-delay")
    String testCbNoDelay();
}
