package com.guardianescolar.api.modules.maps.controller;

import com.guardianescolar.api.modules.maps.dto.MapsDtos;
import com.guardianescolar.api.modules.maps.service.MapsRateLimiter;
import com.guardianescolar.api.modules.maps.service.MapsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/maps")
@PreAuthorize("hasAuthority('LOCATION_VIEW') or hasRole('ADMIN')")
public class MapsController {

    private final MapsService mapsService;
    private final MapsRateLimiter rateLimiter;

    public MapsController(MapsService mapsService, MapsRateLimiter rateLimiter) {
        this.mapsService = mapsService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/geocode")
    @Operation(summary = "Geocodificar una dirección",
            description = "Consulta Google Geocoding desde el backend sin exponer la llave de servidor.")
    @ApiResponse(responseCode = "200", description = "Dirección geocodificada")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida o sin resultados")
    @ApiResponse(responseCode = "429", description = "Límite local o cuota de Google excedida")
    @ApiResponse(responseCode = "503", description = "Google Maps no disponible o solicitud rechazada")
    public MapsDtos.GeocodeResponse geocode(@Valid @RequestBody MapsDtos.GeocodeRequest request) {
        rateLimiter.check("geocode");
        return mapsService.geocode(request);
    }

    @PostMapping("/reverse-geocode")
    @Operation(summary = "Obtener dirección desde coordenadas")
    @ApiResponse(responseCode = "200", description = "Coordenadas geocodificadas")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida o sin resultados")
    @ApiResponse(responseCode = "429", description = "Límite local o cuota de Google excedida")
    @ApiResponse(responseCode = "503", description = "Google Maps no disponible o solicitud rechazada")
    public MapsDtos.GeocodeResponse reverseGeocode(@Valid @RequestBody MapsDtos.ReverseGeocodeRequest request) {
        rateLimiter.check("reverse-geocode");
        return mapsService.reverseGeocode(request);
    }

    @PostMapping("/directions")
    @Operation(summary = "Calcular ruta con Google Directions")
    @ApiResponse(responseCode = "200", description = "Ruta calculada")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida o sin resultados")
    @ApiResponse(responseCode = "429", description = "Límite local o cuota de Google excedida")
    @ApiResponse(responseCode = "503", description = "Google Maps no disponible o solicitud rechazada")
    public MapsDtos.DirectionsResponse directions(@Valid @RequestBody MapsDtos.DirectionsRequest request) {
        rateLimiter.check("directions");
        return mapsService.directions(request);
    }

    @GetMapping("/places/autocomplete")
    @Operation(summary = "Autocompletar lugares con Google Places")
    @ApiResponse(responseCode = "200", description = "Predicciones de lugares")
    @ApiResponse(responseCode = "400", description = "Entrada inválida")
    @ApiResponse(responseCode = "429", description = "Límite local o cuota de Google excedida")
    @ApiResponse(responseCode = "503", description = "Google Maps no disponible o solicitud rechazada")
    public MapsDtos.PlaceAutocompleteResponse autocomplete(
            @RequestParam @NotBlank @Size(max = 200) String input) {
        rateLimiter.check("places-autocomplete");
        return mapsService.autocomplete(input);
    }
}
