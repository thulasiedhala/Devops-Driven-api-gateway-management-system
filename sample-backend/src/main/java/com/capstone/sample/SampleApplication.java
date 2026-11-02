package com.capstone.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Demo downstream service that sits behind the gateway. Each endpoint matches one seeded route. */
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

    @GetMapping("/orders")
    public Map<String, Object> orders(@RequestHeader(value = "X-User", defaultValue = "unknown") String user) {
        return Map.of("servedFor", user,
                "orders", List.of(
                        Map.of("id", 101, "item", "Laptop", "status", "SHIPPED"),
                        Map.of("id", 102, "item", "Keyboard", "status", "PROCESSING")));
    }

    @GetMapping("/users")
    public Map<String, Object> users(@RequestHeader(value = "X-User", defaultValue = "unknown") String user) {
        return Map.of("servedFor", user,
                "users", List.of(
                        Map.of("id", 1, "name", "Asha", "role", "ADMIN"),
                        Map.of("id", 2, "name", "Ravi", "role", "USER")));
    }

    @GetMapping("/payments")
    public Map<String, Object> payments(@RequestHeader(value = "X-User", defaultValue = "unknown") String user) {
        return Map.of("servedFor", user,
                "payments", List.of(
                        Map.of("id", 9001, "orderId", 101, "amount", 55000, "status", "PAID"),
                        Map.of("id", 9002, "orderId", 102, "amount", 1500, "status", "PENDING")));
    }

    @GetMapping("/inventory")
    public Map<String, Object> inventory(@RequestHeader(value = "X-User", defaultValue = "unknown") String user) {
        return Map.of("servedFor", user,
                "stock", List.of(
                        Map.of("product", "Laptop", "available", 14),
                        Map.of("product", "Keyboard", "available", 120),
                        Map.of("product", "Monitor", "available", 31)));
    }
}
