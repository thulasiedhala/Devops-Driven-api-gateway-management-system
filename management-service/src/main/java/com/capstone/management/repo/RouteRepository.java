package com.capstone.management.repo;

import com.capstone.management.model.GatewayRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RouteRepository extends JpaRepository<GatewayRoute, Long> {
    List<GatewayRoute> findByEnabledTrue();
}
