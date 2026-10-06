package com.capstone.management;

import com.capstone.management.model.AppUser;
import com.capstone.management.model.GatewayRoute;
import com.capstone.management.repo.RouteRepository;
import com.capstone.management.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
        if (routes.count() == 0) {
            GatewayRoute r = new GatewayRoute();
            r.setRouteId("product-service");
            r.setPath("/products/**");
            r.setUri("http://sample-backend:8082");
            r.setRateLimitPerMin(20);
            routes.save(r);
        }
    }
}
