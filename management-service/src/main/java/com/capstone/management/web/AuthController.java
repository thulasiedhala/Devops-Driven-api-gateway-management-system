package com.capstone.management.web;

import com.capstone.management.model.AppUser;
import com.capstone.management.repo.UserRepository;
import com.capstone.management.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Credentials(@NotBlank String username, @NotBlank String password) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody Credentials c) {
        if (users.findByUsername(c.username()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        AppUser u = new AppUser();
        u.setUsername(c.username());
        u.setPasswordHash(encoder.encode(c.password()));
        u.setRole("USER");
        users.save(u);
        return Map.of("message", "Registered");
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody Credentials c) {
        AppUser u = users.findByUsername(c.username())
                .filter(x -> encoder.matches(c.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return Map.of("token", jwt.generate(u.getUsername(), u.getRole()),
                      "username", u.getUsername(), "role", u.getRole());
    }
}
