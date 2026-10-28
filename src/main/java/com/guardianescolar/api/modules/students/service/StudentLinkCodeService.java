package com.guardianescolar.api.modules.students.service;

import com.guardianescolar.api.modules.students.domain.Student;
import com.guardianescolar.api.modules.students.repository.StudentRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentLinkCodeService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 16;
    private static final int MAX_ATTEMPTS = 5;
    private static final int EXPIRATION_MINUTES = 15;

    private final SecureRandom secureRandom = new SecureRandom();
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;

    public record IssuedCode(String value, OffsetDateTime expiresAt) {
    }

    @Transactional
    public IssuedCode issue(Student student) {
        String code = randomCode();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(EXPIRATION_MINUTES);
        student.setLinkCodeHash(passwordEncoder.encode(code));
        student.setLinkCodeExpiresAt(expiresAt);
        student.setLinkCodeUsedAt(null);
        student.setLinkCodeFailedAttempts(0);
        studentRepository.save(student);
        return new IssuedCode(code, expiresAt);
    }

    @Transactional
    public void consume(Student student, String code) {
        validate(student, code);
        student.setLinkCodeUsedAt(OffsetDateTime.now());
        studentRepository.save(student);
    }

    @Transactional
    public void verify(Student student, String code) {
        validate(student, code);
    }

    private void validate(Student student, String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase();
        OffsetDateTime now = OffsetDateTime.now();
        int attempts = student.getLinkCodeFailedAttempts() == null ? 0 : student.getLinkCodeFailedAttempts();

        if (student.getLinkCodeHash() == null
                || student.getLinkCodeUsedAt() != null
                || student.getLinkCodeExpiresAt() == null
                || !student.getLinkCodeExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Invalid or expired link code");
        }
        if (attempts >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Link code attempts exceeded");
        }
        if (!passwordEncoder.matches(code, student.getLinkCodeHash())) {
            student.setLinkCodeFailedAttempts(attempts + 1);
            studentRepository.incrementLinkCodeFailedAttempts(student.getId());
            studentRepository.save(student);
            throw new IllegalArgumentException("Invalid link code");
        }
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            code.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
