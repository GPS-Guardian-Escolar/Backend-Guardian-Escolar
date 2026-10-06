# Módulo maps

Este módulo actúa como proxy seguro entre los clientes y Google Maps. La llave de servidor se usa únicamente en backend y debe estar restringida por IP en Google Cloud Console.

## Estrategia híbrida

- `GeoMathService` sigue siendo la fuente local para cálculos en tiempo real, como distancia y detección de desvíos. Esto evita costo por evento GPS y reduce latencia.
- Google Maps se usa para planificación inicial de ruta, ETA con tráfico, geocoding, reverse geocoding y autocompletado de lugares.
- Los eventos WebSocket existentes en `/topic/trips/{id}/coordinates` no cambian. Cuando se agregue Navigation SDK, sus eventos deben publicarse como eventos de dominio o pasar por `TripAlertService` sin reemplazar el flujo actual.

## Caché

- Geocoding, reverse geocoding y places autocomplete usan TTL largo configurado en `google.maps.cache.geocoding-ttl`.
- Directions usa TTL corto configurado en `google.maps.cache.directions-ttl`, porque el tráfico puede cambiar rápido.
- La interfaz `MapsCacheService` permite migrar a Redis sin cambiar controladores ni servicios de negocio.

## Costos

Cada miss de caché consume cuota de Google Maps. Mantener límites por usuario y alertas de presupuesto evita sorpresas en facturación.
