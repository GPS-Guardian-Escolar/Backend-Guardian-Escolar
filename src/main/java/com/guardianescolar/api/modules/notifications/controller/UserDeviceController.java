package com.guardianescolar.api.modules.notifications.controller;

import com.guardianescolar.api.modules.notifications.dto.NotificationDtos;
import com.guardianescolar.api.modules.notifications.service.UserDeviceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @GetMapping
    public List<NotificationDtos.DeviceResponse> listarActuales() {
        return userDeviceService.listarActuales();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationDtos.DeviceResponse register(
            @Valid @RequestBody NotificationDtos.DeviceRequest request) {
        return userDeviceService.register(request);
    }

    @DeleteMapping("/{dispositivoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable UUID dispositivoId) {
        userDeviceService.desactivar(dispositivoId);
    }
}
