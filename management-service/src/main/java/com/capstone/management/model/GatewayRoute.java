package com.capstone.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(name = "gateway_routes")
public class GatewayRoute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(unique = true, nullable = false)
    private String routeId;          // e.g. product-service
    @NotBlank
    private String path;             // e.g. /products/**
    @NotBlank
    private String uri;              // e.g. http://sample-backend:8082
    private int stripPrefix = 0;
    private int rateLimitPerMin = 100;
    private boolean enabled = true;
}
