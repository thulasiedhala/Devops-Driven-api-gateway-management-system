package com.capstone.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Fixed-window rate limiter: N requests per minute per (route, client IP). */
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final int defaultLimit;

    public RateLimitGlobalFilter(@Value("${app.default-rate-limit-per-min}") int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) return chain.filter(exchange);

        int limit = defaultLimit;
        Object meta = route.getMetadata().get("rateLimit");
        if (meta instanceof Number n) limit = n.intValue();

        var addr = exchange.getRequest().getRemoteAddress();
        String ip = addr == null ? "unknown" : addr.getAddress().getHostAddress();
        int count = counters.computeIfAbsent(route.getId() + ":" + ip, k -> new AtomicInteger()).incrementAndGet();

        if (count > limit) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Scheduled(fixedRate = 60_000)
    void resetWindow() { counters.clear(); }

    @Override
    public int getOrder() { return -50; }
}
