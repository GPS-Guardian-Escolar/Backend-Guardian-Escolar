package com.guardianescolar.api.modules.admin.controller;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;

import com.guardianescolar.api.modules.admin.dto.AdminDtos;
import com.guardianescolar.api.modules.admin.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Administrative dashboards, alerts and audit data")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Solicitud inválida o parámetros no válidos"),
    @ApiResponse(responseCode = "401", description = "Se requiere autenticación"),
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
})
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Consultar panel administrativo")
    @GetMapping("/dashboard")
    public AdminDtos.DashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @Operation(summary = "Listar alertas recientes")
    @GetMapping("/alerts")
    public List<AdminDtos.AlertSummaryResponse> recentAlerts(
            @RequestParam(required = false) Integer limit) {
        return adminService.recentAlerts(limit);
    }

    @Operation(summary = "Consultar auditoría reciente")
    @GetMapping("/audit")
    public List<AdminDtos.AuditResponse> recentAudit(
            @RequestParam(required = false) Integer limit) {
        return adminService.recentAudit(limit);
    }

    @Operation(summary = "Consultar errores recientes")
    @GetMapping("/errors")
    public List<AdminDtos.ErrorResponse> recentErrors(
            @RequestParam(required = false) Integer limit) {
        return adminService.recentErrors(limit);
    }
}
