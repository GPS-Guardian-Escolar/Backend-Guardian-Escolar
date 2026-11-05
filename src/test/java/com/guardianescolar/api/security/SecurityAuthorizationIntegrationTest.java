package com.guardianescolar.api.security;

import com.guardianescolar.api.modules.audit.service.ErrorLogService;
import com.guardianescolar.api.modules.audit.service.AuditService;
import com.guardianescolar.api.modules.students.service.StudentService;
import com.guardianescolar.api.shared.exception.ApiExceptionHandler;
import com.guardianescolar.api.shared.security.JwtAuthenticationEntryPoint;
import com.guardianescolar.api.shared.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.guardianescolar.api.modules.students.controller.StudentController.class)
@Import({ApiExceptionHandler.class, SecurityAuthorizationIntegrationTest.TestSecurityConfiguration.class})
@TestPropertySource(properties = "guardian.cors.allowed-origins=http://localhost")
class SecurityAuthorizationIntegrationTest {

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> { })
                    .exceptionHandling(errors -> errors.authenticationEntryPoint(
                            new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.GET, "/api/students").authenticated()
                            .requestMatchers(HttpMethod.GET, "/api/students/*/guardians").hasAuthority("STUDENT_READ")
                            .requestMatchers(HttpMethod.POST, "/api/students").hasAuthority("STUDENT_CREATE")
                            .anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @MockBean
    private ErrorLogService errorLogService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "STUDENT_READ")
    void authenticatedUserWithoutCreatePermissionReturns403() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "STUDENT_READ")
    void accessToAnotherStudentIsRejectedWith403() throws Exception {
        UUID studentId = UUID.randomUUID();
        doThrow(new AccessDeniedException("You do not have access to this student"))
                .when(studentService).unlinkDevice(studentId);

        mockMvc.perform(delete("/api/students/{studentId}/device", studentId))
                .andExpect(status().isForbidden());
    }
}
