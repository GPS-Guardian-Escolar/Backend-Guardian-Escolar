package com.guardianescolar.api.modules.students.service;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StudentLinkCodeService {

    private static final int LINK_CODE_LENGTH = 10;

    public String codeFor(UUID studentId) {
        return studentId.toString().replace("-", "").substring(0, LINK_CODE_LENGTH).toUpperCase();
    }

    public boolean matches(UUID studentId, String code) {
        return codeFor(studentId).equalsIgnoreCase(code == null ? "" : code.trim());
    }
}
