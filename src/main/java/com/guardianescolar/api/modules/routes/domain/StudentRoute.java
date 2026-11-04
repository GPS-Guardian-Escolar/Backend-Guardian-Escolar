package com.guardianescolar.api.modules.routes.domain;

import com.guardianescolar.api.modules.students.domain.Student;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "student_routes")
@Getter
@Setter
public class StudentRoute {

    @EmbeddedId
    private StudentRouteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("routeId")
    @JoinColumn(name = "route_id")
    private Route route;

    @Column(name = "is_preferred", nullable = false)
    private Boolean isPreferred = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    public StudentRoute() {
    }

    public StudentRoute(Student student, Route route) {
        this.student = student;
        this.route = route;
        this.id = new StudentRouteId(student.getId(), route.getId());
    }

}
