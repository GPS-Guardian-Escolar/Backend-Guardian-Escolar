package com.guardianescolar.api.modules.routes.repository;

import com.guardianescolar.api.modules.routes.domain.Stop;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository extends JpaRepository<Stop, UUID> {

    List<Stop> findByRouteIdOrderByStopOrderAsc(UUID routeId);

    void deleteByRouteId(UUID routeId);
}
