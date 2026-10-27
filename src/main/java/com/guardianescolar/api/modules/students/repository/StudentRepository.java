package com.guardianescolar.api.modules.students.repository;

import com.guardianescolar.api.modules.students.domain.Student;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    List<Student> findByDeletedAtIsNullOrderByFullNameAsc();

    List<Student> findByUserEmailIgnoreCaseAndDeletedAtIsNullOrderByFullNameAsc(String email);

    Optional<Student> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByUserIdAndFullNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String fullName);

    boolean existsByUserIdAndFullNameIgnoreCaseAndDeletedAtIsNullAndIdNot(UUID userId, String fullName, UUID id);
}
