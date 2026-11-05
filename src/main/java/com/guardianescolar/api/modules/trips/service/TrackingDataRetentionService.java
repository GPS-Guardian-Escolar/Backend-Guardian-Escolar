package com.guardianescolar.api.modules.trips.service;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enforces data minimization for completed trips; open trips are always retained. */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingDataRetentionService {

    static final int COORDINATE_RETENTION_DAYS = 7;
    static final int TRIP_HISTORY_RETENTION_DAYS = 30;
    static final int NOTIFICATION_RETENTION_DAYS = 3;

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.retention.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${app.retention.cron:0 15 3 * * *}", zone = "UTC")
    @Transactional
    public void purgeExpiredTrackingData() {
        if (!enabled) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime notificationCutoff = now.minusDays(NOTIFICATION_RETENTION_DAYS);
        OffsetDateTime coordinateCutoff = now.minusDays(COORDINATE_RETENTION_DAYS);
        OffsetDateTime tripCutoff = now.minusDays(TRIP_HISTORY_RETENTION_DAYS);

        int receipts = jdbcTemplate.update("""
                DELETE FROM notification_receipts r
                USING notifications n
                LEFT JOIN trips t ON t.id = n.trip_id
                WHERE r.notification_id = n.id
                  AND (n.event_at < ? OR (t.trip_ended_at IS NOT NULL AND t.trip_ended_at < ?))
                """, notificationCutoff, tripCutoff);
        int notifications = jdbcTemplate.update("""
                DELETE FROM notifications n
                WHERE n.event_at < ?
                   OR n.trip_id IN (
                       SELECT t.id FROM trips t
                       WHERE t.trip_ended_at IS NOT NULL AND t.trip_ended_at < ?
                   )
                """, notificationCutoff, tripCutoff);
        int coordinates = jdbcTemplate.update("""
                DELETE FROM coordinates c
                USING trips t
                WHERE c.trip_id = t.id
                  AND t.trip_ended_at IS NOT NULL
                  AND t.trip_ended_at < ?
                """, coordinateCutoff);
        int trips = jdbcTemplate.update("""
                DELETE FROM trips
                WHERE status IN ('COMPLETED', 'CANCELED')
                  AND trip_ended_at IS NOT NULL
                  AND trip_ended_at < ?
                """, tripCutoff);

        log.info("Tracking retention completed: receipts={}, notifications={}, coordinates={}, trips={}",
                receipts, notifications, coordinates, trips);
    }
}
