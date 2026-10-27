package com.guardianescolar.api.modules.routes.domain;

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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "routes")
@Getter
@Setter
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "route_name", nullable = false, length = 100)
    private String routeName;

    @Column(name = "description")
    private String description;

    @Column(name = "origin_latitude", precision = 9, scale = 6)
    private BigDecimal originLatitude;

    @Column(name = "origin_longitude", precision = 9, scale = 6)
    private BigDecimal originLongitude;

    @Column(name = "destination_latitude", precision = 9, scale = 6)
    private BigDecimal destinationLatitude;

    @Column(name = "destination_longitude", precision = 9, scale = 6)
    private BigDecimal destinationLongitude;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

}
