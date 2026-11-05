package com.guardianescolar.api.modules.notifications.domain;

import com.guardianescolar.api.modules.security.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "notification_settings")
@Getter
@Setter
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "delay_alert")
    private Boolean delayAlert = true;

    @Column(name = "route_change_alert")
    private Boolean routeChangeAlert = true;

    @Column(name = "arrival_alert")
    private Boolean arrivalAlert = true;

    @Column(name = "inactivity_alert")
    private Boolean inactivityAlert = true;

    @Column(name = "push_channel")
    private Boolean pushChannel = true;

    @Column(name = "email_channel")
    private Boolean emailChannel = true;


    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

}
