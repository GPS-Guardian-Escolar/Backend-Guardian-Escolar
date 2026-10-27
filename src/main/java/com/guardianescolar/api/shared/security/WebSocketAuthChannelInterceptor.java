package com.guardianescolar.api.shared.security;

import lombok.RequiredArgsConstructor;
import com.guardianescolar.api.modules.trips.repository.TripRepository;
import com.guardianescolar.api.modules.security.domain.User;
import com.guardianescolar.api.modules.security.repository.UserRepository;
import com.guardianescolar.api.modules.students.repository.StudentGuardianRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT != accessor.getCommand()) {
            if (StompCommand.SUBSCRIBE == accessor.getCommand()) {
                authorizeTripSubscription(accessor);
            }
            return message;
        }

        String token = extractToken(accessor);
        UserDetails userDetails = userDetailsService.loadUserByUsername(jwtService.extractUsername(token));
        if (!jwtService.isTokenValid(token, userDetails)) {
            throw new AccessDeniedException("Token WebSocket inválido");
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
        accessor.setUser(authentication);
        return message;
    }

    private void authorizeTripSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        String prefix = "/topic/trips/";
        String suffix = "/coordinates";
        if (destination == null || !destination.startsWith(prefix) || !destination.endsWith(suffix)) {
            return;
        }
        if (!(accessor.getUser() instanceof Authentication authentication)) {
            throw new AccessDeniedException("Autenticación WebSocket requerida");
        }
        String value = destination.substring(prefix.length(), destination.length() - suffix.length());
        UUID tripId;
        try {
            tripId = UUID.fromString(value);
        } catch (IllegalArgumentException error) {
            throw new AccessDeniedException("Viaje WebSocket inválido");
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ADMIN".equals(authority.getAuthority()));
        boolean owner = tripRepository.existsByIdAndStudentUserEmailIgnoreCaseAndDeletedAtIsNull(
                tripId, authentication.getName());
        boolean shared = false;
        if (!admin && !owner) {
            User current = userRepository.findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException("Usuario WebSocket inválido"));
            shared = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                    .map(trip -> studentGuardianRepository.existsByStudentIdAndUserIdAndStatusAndDeletedAtIsNull(
                            trip.getStudent().getId(), current.getId(), "ACTIVE"))
                    .orElse(false);
        }
        if (!admin && !owner && !shared) {
            throw new AccessDeniedException("No tienes acceso a este viaje");
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");
        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            authorizationHeaders = accessor.getNativeHeader("authorization");
        }

        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            throw new AccessDeniedException("Authorization es obligatorio para WebSocket");
        }

        String authorization = authorizationHeaders.get(0);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("Authorization WebSocket debe usar Bearer token");
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
