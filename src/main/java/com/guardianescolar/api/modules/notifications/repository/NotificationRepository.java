package com.guardianescolar.api.modules.notifications.repository;

import com.guardianescolar.api.modules.notifications.domain.Notification;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByOrderByEventDateTimeDesc(Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(n) > 0 THEN TRUE ELSE FALSE END
            FROM Notification n, NotificationReceipt r
            WHERE r.notification = n
              AND n.trip.id = :tripId
              AND n.eventType = :eventType
              AND n.eventDateTime > :after
              AND r.user.id = :userId
            """)
    boolean existsRecentNotificationForUser(
            @Param("tripId") UUID tripId,
            @Param("eventType") String eventType,
            @Param("after") OffsetDateTime after,
            @Param("userId") UUID userId);
}
