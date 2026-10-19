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
- Correo configurable mediante SMTP de Gmail o API HTTPS de Resend.
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

## Proveedores de correo

El proveedor se selecciona con `MAIL_PROVIDER` y por defecto es `smtp`, por lo que la
configuracion local existente con Gmail se conserva. Para Docker local:

```env
APP_MAIL_ENABLED=true
MAIL_PROVIDER=smtp
APP_MAIL_FROM=gpsguardianescolar@gmail.com
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=gpsguardianescolar@gmail.com
MAIL_PASSWORD=<contrasena-de-aplicacion-de-gmail>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

Para Render Free se puede usar la API HTTPS de Resend, sin exponer una clave SMTP:

```env
APP_MAIL_ENABLED=true
MAIL_PROVIDER=resend
APP_MAIL_FROM=onboarding@resend.dev
RESEND_API_KEY=<clave-secreta-de-resend>
```

`RESEND_API_KEY` solo debe configurarse en las variables privadas del servicio. El remitente
`onboarding@resend.dev` es válido para pruebas con las restricciones de Resend y no debe
tratarse como un remitente de producción para destinatarios arbitrarios. Para producción se
debe verificar un dominio en Resend y usar una dirección autorizada en `APP_MAIL_FROM`.

La selección de proveedor conserva los flujos actuales de verificación de correo,
recuperación de contraseña, confirmación de dispositivo, doble factor y notificaciones. No
se incluyen claves en el código ni en `.env.example`.
