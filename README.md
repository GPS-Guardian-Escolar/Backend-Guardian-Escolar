# HU-20 - Backend Implementado

## Objetivo

Consolidar el backend completo de GPS Guardian Escolar con todos los modulos implementados, configuracion productiva, pruebas basicas y soporte de despliegue.

## Cambios agregados

Esta HU representa el estado final implementado del backend, incluyendo:

- Autenticacion, JWT, verificacion de correo y doble factor.
- Gestion de usuarios, roles, permisos y sesiones.
- Gestion de estudiantes, acudientes, contactos y dispositivos.
- Rutas escolares y paradas.
- Trayectos GPS y coordenadas.
- Zonas seguras.
- Notificaciones, Expo Push y SMS configurable.
- Panel administrativo.
- Auditoria y registro de errores.
- Configuracion Spring Boot por perfiles.
- Dockerfile y docker-compose standalone.
- `.env.example` sin secretos reales.
- Pruebas automatizadas existentes.

## Validacion esperada

```bash
./mvnw test
```

Resultado esperado:

```text
Tests run: 3
Failures: 0
BUILD SUCCESS
```

## Google Maps Platform

El backend expone `/api/maps/**` como proxy autenticado para Google Maps. La clave debe vivir solo en variables de entorno o en `.env`; nunca debe escribirse en codigo Java ni devolverse al frontend.

Configura la variable antes de iniciar la aplicacion:

```bash
GOOGLE_MAPS_API_KEY=<tu_google_maps_api_key>
```

Ejemplo geocoding:

```http
POST /api/maps/geocode
Authorization: Bearer <jwt>
Content-Type: application/json

{ "address": "SENA Bogota" }
```

Ejemplo reverse geocoding:

```http
POST /api/maps/reverse-geocode
Authorization: Bearer <jwt>
Content-Type: application/json

{ "latitude": 4.6486, "longitude": -74.1006 }
```

Los endpoints requieren JWT y no exponen la API key al cliente.
