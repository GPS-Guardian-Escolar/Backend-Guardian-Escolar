package com.guardianescolar.api.modules.trips.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class TrackingDataRetentionServiceTest {

    @Test
    void purgeDeletesReceiptsAndDependentsBeforeExpiredTrips() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);
        TrackingDataRetentionService service = new TrackingDataRetentionService(jdbc);
        ReflectionTestUtils.setField(service, "enabled", true);

        service.purgeExpiredTrackingData();

        InOrder order = inOrder(jdbc);
        order.verify(jdbc).update(contains("DELETE FROM notification_receipts"), any(Object[].class));
        order.verify(jdbc).update(contains("DELETE FROM notifications"), any(Object[].class));
        order.verify(jdbc).update(contains("DELETE FROM coordinates"), any(Object[].class));
        order.verify(jdbc).update(contains("DELETE FROM trips"), any(Object[].class));
        assertEquals(7, TrackingDataRetentionService.COORDINATE_RETENTION_DAYS);
        assertEquals(30, TrackingDataRetentionService.TRIP_HISTORY_RETENTION_DAYS);
        assertEquals(3, TrackingDataRetentionService.NOTIFICATION_RETENTION_DAYS);
    }

    @Test
    void disabledRetentionDoesNotExecuteDeletes() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        TrackingDataRetentionService service = new TrackingDataRetentionService(jdbc);
        ReflectionTestUtils.setField(service, "enabled", false);

        service.purgeExpiredTrackingData();

        org.mockito.Mockito.verifyNoInteractions(jdbc);
    }
}
