package com.guardianescolar.api.modules.security.repository;

import com.guardianescolar.api.modules.security.domain.SessionStatus;
import com.guardianescolar.api.modules.security.domain.UserSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    List<UserSession> findByUserIdAndSessionStatusAndEmailConfirmedTrueAndDeviceIdentifierIsNotNull(
            UUID userId,
            SessionStatus sessionStatus);

    boolean existsByUserIdAndDeviceIdentifierAndSessionStatusAndEmailConfirmedTrue(
            UUID userId,
            String deviceIdentifier,
            SessionStatus sessionStatus);

    Optional<UserSession> findFirstByUserIdAndDeviceIdentifierAndEmailConfirmedFalseOrderByCreatedAtDesc(
            UUID userId,
            String deviceIdentifier);

    Optional<UserSession> findFirstByDeviceConfirmationToken(String deviceConfirmationToken);

    boolean existsByTokenAndSessionStatus(String token, SessionStatus sessionStatus);

    List<UserSession> findByUserIdAndSessionStatus(UUID userId, SessionStatus sessionStatus);
}
