package com.capstone.management.web;

import com.capstone.management.model.GatewayRoute;
import com.capstone.management.repo.RouteRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final RouteRepository repo;

    public RouteController(RouteRepository repo) { this.repo = repo; }

    @GetMapping
    public List<GatewayRoute> list() { return repo.findAll(); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public GatewayRoute create(@Valid @RequestBody GatewayRoute r) {
        r.setId(null);
        return repo.save(r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GatewayRoute update(@PathVariable Long id, @Valid @RequestBody GatewayRoute r) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        r.setId(id);
        return repo.save(r);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { repo.deleteById(id); }
}
