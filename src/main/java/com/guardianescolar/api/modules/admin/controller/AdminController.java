package com.guardianescolar.api.modules.admin.controller;

import com.guardianescolar.api.modules.admin.dto.AdminDtos;
import com.guardianescolar.api.modules.admin.service.AdminService;
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
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public AdminDtos.DashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/alerts")
    public List<AdminDtos.AlertSummaryResponse> recentAlerts(
            @RequestParam(required = false) Integer limit) {
        return adminService.recentAlerts(limit);
    }

    @GetMapping("/audit")
    public List<AdminDtos.AuditResponse> recentAudit(
            @RequestParam(required = false) Integer limit) {
        return adminService.recentAudit(limit);
    }

    @GetMapping("/errors")
    public List<AdminDtos.ErrorResponse> recentErrors(
            @RequestParam(required = false) Integer limit) {
        return adminService.recentErrors(limit);
    }
}
