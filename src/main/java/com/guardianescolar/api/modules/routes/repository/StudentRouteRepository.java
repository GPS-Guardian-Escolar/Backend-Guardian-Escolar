package com.guardianescolar.api.modules.routes.repository;

import com.guardianescolar.api.modules.routes.domain.StudentRoute;
import com.guardianescolar.api.modules.routes.domain.StudentRouteId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRouteRepository extends JpaRepository<StudentRoute, StudentRouteId> {

    boolean existsByIdStudentIdAndIdRouteIdAndIsActiveTrue(UUID studentId, UUID routeId);

    List<StudentRoute> findByIdStudentIdAndIsActiveTrue(UUID studentId);

    java.util.Optional<StudentRoute> findFirstByIdStudentIdAndIsActiveTrueAndIsPreferredTrue(UUID studentId);

    @Modifying
    @Query("update StudentRoute assignment set assignment.isPreferred = false "
            + "where assignment.id.studentId = :studentId and assignment.isActive = true")
    int clearPreferredRoute(@Param("studentId") UUID studentId);

    @Modifying
    @Query("update StudentRoute assignment set assignment.isPreferred = true "
            + "where assignment.id.studentId = :studentId and assignment.id.routeId = :routeId "
            + "and assignment.isActive = true")
    int markPreferredRoute(@Param("studentId") UUID studentId, @Param("routeId") UUID routeId);
}
