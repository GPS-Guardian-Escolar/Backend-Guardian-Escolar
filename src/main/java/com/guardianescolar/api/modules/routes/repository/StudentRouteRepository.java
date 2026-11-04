package com.guardianescolar.api.modules.routes.repository;

import com.guardianescolar.api.modules.routes.domain.StudentRoute;
import com.guardianescolar.api.modules.routes.domain.StudentRouteId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRouteRepository extends JpaRepository<StudentRoute, StudentRouteId> {

    boolean existsByIdStudentIdAndIdRouteIdAndIsActiveTrue(UUID studentId, UUID routeId);

    List<StudentRoute> findByIdStudentIdAndIsActiveTrue(UUID studentId);

    java.util.Optional<StudentRoute> findFirstByIdStudentIdAndIsActiveTrueAndIsPreferredTrue(UUID studentId);
}
