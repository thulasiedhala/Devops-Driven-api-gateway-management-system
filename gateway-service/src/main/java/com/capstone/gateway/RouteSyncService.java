package com.capstone.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

/** Pulls the route table from the management service and applies it to the running gateway. */
@Slf4j
@Service
public class RouteSyncService {
    public record RouteDto(String routeId, String path, String uri, int stripPrefix, int rateLimitPerMin) {}

    private final WebClient client;
    private final String internalKey;
    private final RouteDefinitionWriter writer;
    private final ApplicationEventPublisher publisher;

    private List<RouteDto> last = null;
    private Set<String> applied = new HashSet<>();

    public RouteSyncService(WebClient.Builder builder,
                            @Value("${app.management-url}") String managementUrl,
                            @Value("${app.internal-key}") String internalKey,
                            RouteDefinitionWriter writer,
                            ApplicationEventPublisher publisher) {
        this.client = builder.baseUrl(managementUrl).build();
        this.internalKey = internalKey;
        this.writer = writer;
        this.publisher = publisher;
    }

    @Scheduled(initialDelay = 10_000, fixedDelayString = "${app.route-sync-ms}")
    public void sync() {
        try {
            List<RouteDto> remote = client.get().uri("/internal/routes")
                    .header("X-Internal-Key", internalKey)
                    .retrieve().bodyToFlux(RouteDto.class).collectList().block();
            if (remote == null || remote.equals(last)) return;

            Set<String> ids = new HashSet<>();
            remote.forEach(r -> ids.add(r.routeId()));
            for (String old : applied) {
                if (!ids.contains(old)) writer.delete(Mono.just(old)).onErrorResume(e -> Mono.empty()).block();
            }
            for (RouteDto r : remote) writer.save(Mono.just(toDefinition(r))).block();

            applied = ids;
            last = remote;
            publisher.publishEvent(new RefreshRoutesEvent(this));
            log.info("Route table synced: {} dynamic route(s)", remote.size());
        } catch (Exception e) {
            log.warn("Route sync failed (will retry): {}", e.getMessage());
        }
    }

    private RouteDefinition toDefinition(RouteDto r) {
        RouteDefinition def = new RouteDefinition();
        def.setId(r.routeId());
        def.setUri(URI.create(r.uri()));
        def.setPredicates(List.of(new PredicateDefinition("Path=" + r.path())));
        def.setFilters(List.of(new FilterDefinition("StripPrefix=" + r.stripPrefix())));
        def.setMetadata(Map.of("rateLimit", r.rateLimitPerMin()));
        return def;
    }
}
