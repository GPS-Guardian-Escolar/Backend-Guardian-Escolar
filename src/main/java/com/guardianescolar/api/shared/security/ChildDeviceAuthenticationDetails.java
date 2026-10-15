package com.guardianescolar.api.shared.security;

import java.util.UUID;

public record ChildDeviceAuthenticationDetails(UUID studentId, String deviceIdentifier) {
}
