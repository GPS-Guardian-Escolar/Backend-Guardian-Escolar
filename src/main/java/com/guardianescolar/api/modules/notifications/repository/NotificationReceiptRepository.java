package com.guardianescolar.api.modules.notifications.repository;

import com.guardianescolar.api.modules.notifications.domain.NotificationReceipt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationReceiptRepository extends JpaRepository<NotificationReceipt, UUID> {

    List<NotificationReceipt> findByUserEmailIgnoreCaseOrderByReceivedAtDesc(String email);

    Optional<NotificationReceipt> findByIdAndUserEmailIgnoreCase(UUID id, String email);

    List<NotificationReceipt> findByUserEmailIgnoreCaseAndWasReadFalse(String email);
}
