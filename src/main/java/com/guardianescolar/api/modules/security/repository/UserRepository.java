package com.guardianescolar.api.modules.security.repository;

import com.guardianescolar.api.modules.security.domain.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByEmailVerificationToken(String emailVerificationToken);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    Optional<User> findByTwoFactorToken(String twoFactorToken);

    boolean existsByEmailIgnoreCase(String email);

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM users
                    WHERE deleted_at IS NULL
                      AND phone IS NOT NULL
                      AND regexp_replace(phone, '\\D', '', 'g') = :phoneDigits
                    """,
            nativeQuery = true)
    long countActiveByPhoneDigits(@Param("phoneDigits") String phoneDigits);
}
