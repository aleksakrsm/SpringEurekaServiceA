package com.example.servicea.controller;

import com.example.servicea.client.AlbumsRestClient;
import com.example.servicea.client.ServiceBClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@RestController
@Slf4j
public class ServiceARestController {

    public static final AtomicInteger counter = new AtomicInteger(0);

    private final String serviceId;
    private final int servicePort;
    private final AlbumsRestClient albumsRestClient;
    private final ServiceBClient serviceBClient;
    private final CircuitBreaker circuitBreaker;


    /**
     * @param serviceId             this service identifier used by discovery service
     * @param servicePort           server port
     * @param circuitBreakerFactory Circuit Breaker Factory based on implementation imported as library (Currently resilience4J)
     * @param albumsRestClient      rest client used to consume external api
     */
    public ServiceARestController(@Value("${service.id}") String serviceId,
                                  @Value("${server.port}") int servicePort,
                                  CircuitBreakerFactory circuitBreakerFactory,
                                  AlbumsRestClient albumsRestClient, ServiceBClient serviceBClient) {
        this.serviceId = serviceId;
        this.servicePort = servicePort;
        this.albumsRestClient = albumsRestClient;
        this.serviceBClient = serviceBClient;
        circuitBreaker = circuitBreakerFactory.create(serviceId);
    }

    @GetMapping("/helloWorld")
    public String helloWorld() {
        log.info("Hello world from Service A!");
        return "Hello world from Service A! My name is " + serviceId;
    }


    @GetMapping("/albums/no-circuit-breaker")
    public AlbumsResponse returnAlbums() {
        log.info("Invoking 'GET /albums' method on service {}:{}", serviceId, servicePort);
        try {
            List<AlbumsRestClient.Album> albums = albumsRestClient.getAlbums();
            log.info("Retrieved {} items from external API", albums.size());
            return new AlbumsResponse(serviceId + ":" + servicePort, albums);
        } catch (Exception e) {
            log.error("Error while executing request to external API. Cause: {}", e.getMessage());
            throw e;
        }
    }


    /**
     * @return list of albums
     */
    @GetMapping("/albums/with-circuit-breaker")
    public ResponseEntity<?> returnAlbumsWithCircuitBreaker() {
        log.info("Invoking 'GET /albums' with circuit breaker method on service {}:{}", serviceId, servicePort);
        try {
            /* U slučaju da koristimo neku reaktivnu varijantu api klijenta (koja je bazirana na webflux WebClient implementaciji)
             * koristili bismo ReactiveCircuitBreaker varijantu za koju je potrebno uvesti drugačiju biblioteku*/
            List<AlbumsRestClient.Album> albums = circuitBreaker.run(albumsRestClient::getAlbums,
                    throwable -> Collections.emptyList());
            log.info("Retrieved {} items from external API with circuit breaker", albums.size());
            return ResponseEntity.ok(new AlbumsResponse(serviceId + ":" + servicePort, albums));
        } catch (Exception e) {
            log.error("Error while executing request to external API with circuit breaker. Cause: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("Cause: ", e.getMessage()));
        }
    }

    @GetMapping("/svc-b/cb")
    public ResponseEntity<?> testServiceBWithDelay(@RequestParam boolean delay) {
        log.info("Invoking 'GET /svc-b/cb'. Counter: {},  using circuit breaker method on service {}:{}; {}",
                counter.incrementAndGet(), serviceId, servicePort, delay ? "delayed" : "non-delayed");
        try {

            Supplier<String> toExecute = delay ? serviceBClient::testCbWithDelay : serviceBClient::testCbNoDelay;
            String result = circuitBreaker.run(toExecute,
                    throwable -> "Error on service B call. Cause: " + throwable.getMessage());
            log.info("Retrieved '{}' from external Service B API with circuit breaker on attempt {}", result, counter.get());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error while executing request to external Service B API with circuit breaker. Counter: {}. Cause: {}",
                    counter.get(), e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("Cause: ", e.getMessage()));
        }
    }

    public record AlbumsResponse(String service, List<AlbumsRestClient.Album> albums) {
    }

}
