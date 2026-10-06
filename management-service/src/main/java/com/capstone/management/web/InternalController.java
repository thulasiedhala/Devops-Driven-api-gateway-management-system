package com.capstone.management.web;

import com.capstone.management.repo.RouteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Called only by the gateway (shared secret) to pull the active route table. */
@RestController
@RequestMapping("/internal")
public class InternalController {
    public record RouteDto(String routeId, String path, String uri, int stripPrefix, int rateLimitPerMin) {}

    private final RouteRepository repo;
    private final String internalKey;

    public InternalController(RouteRepository repo, @Value("${app.internal-key}") String internalKey) {
        this.repo = repo; this.internalKey = internalKey;
    }

    @GetMapping("/routes")
    public List<RouteDto> routes(@RequestHeader(value = "X-Internal-Key", required = false) String key) {
        if (!internalKey.equals(key)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return repo.findByEnabledTrue().stream()
                .map(r -> new RouteDto(r.getRouteId(), r.getPath(), r.getUri(), r.getStripPrefix(), r.getRateLimitPerMin()))
                .toList();
    }
}
