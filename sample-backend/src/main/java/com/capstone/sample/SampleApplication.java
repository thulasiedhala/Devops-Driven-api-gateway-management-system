package com.capstone.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Demo downstream service that sits behind the gateway. */
@SpringBootApplication
@RestController
public class SampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    @GetMapping("/products")
    public Map<String, Object> products(@RequestHeader(value = "X-User", defaultValue = "unknown") String user) {
        return Map.of("servedFor", user,
                "products", List.of(
                        Map.of("id", 1, "name", "Laptop", "price", 55000),
                        Map.of("id", 2, "name", "Keyboard", "price", 1500),
                        Map.of("id", 3, "name", "Monitor", "price", 12000)));
    }
}
