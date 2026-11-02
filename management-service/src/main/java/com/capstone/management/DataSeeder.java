package com.capstone.management;

import com.capstone.management.model.AppUser;
import com.capstone.management.model.GatewayRoute;
import com.capstone.management.repo.RouteRepository;
import com.capstone.management.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the demo admin user and a starter set of routes. Existing data is never overwritten. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final RouteRepository routes;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, RouteRepository routes, PasswordEncoder encoder) {
        this.users = users; this.routes = routes; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.findByUsername("admin").isEmpty()) {
            AppUser a = new AppUser();
            a.setUsername("admin");
            a.setPasswordHash(encoder.encode("admin123"));
            a.setRole("ADMIN");
            users.save(a);
        }
        seed("product-service",   "/products/**",  20);
        seed("order-service",     "/orders/**",    30);
        seed("user-service",      "/users/**",     50);
        seed("payment-service",   "/payments/**",  15);
        seed("inventory-service", "/inventory/**", 40);
    }

    private void seed(String routeId, String path, int limitPerMin) {
        if (routes.existsByRouteId(routeId)) return;
        GatewayRoute r = new GatewayRoute();
        r.setRouteId(routeId);
        r.setPath(path);
        r.setUri("http://sample-backend:8082");
        r.setRateLimitPerMin(limitPerMin);
        routes.save(r);
    }
}
