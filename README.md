# GPS Guardian Escolar Backend

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
- Notificaciones por correo y Expo Push.
- Panel administrativo.
- Auditoria y registro de errores.
- Configuracion Spring Boot por perfiles.
- Correo configurable mediante SMTP de Gmail o API HTTPS de Brevo.
- Dockerfile y docker-compose standalone.
- `.env.example` sin secretos reales.
- Pruebas automatizadas existentes.

## Dependencia de base de datos

La API no crea el esquema PostgreSQL por sí sola. El esquema, roles, índices y datos iniciales
se administran en `DB-Structure-Guardian-Escolar` mediante Liquibase. En un entorno local, el
orden soportado es:

1. Iniciar PostgreSQL.
2. Ejecutar Liquibase con el changelog de la base de datos.
3. Iniciar esta API con `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` apuntando a esa base.

El compose recomendado del repositorio `Docker-Infra-Guardian-Escolar` automatiza ese orden y
espera a que Liquibase termine antes de iniciar el backend.

## Requisitos y arranque

- Java 17.
- Maven Wrapper incluido (`mvnw` o `mvnw.cmd`).
- PostgreSQL 15 con el esquema aplicado por Liquibase.
- Variables de entorno definidas a partir de `.env.example`; `JWT_SECRET` debe contener al
  menos 32 bytes aleatorios y es obligatorio. La configuración no tiene un valor de respaldo.

Para ejecutar solo el backend con PostgreSQL disponible:

```bash
bash ./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Validación

Para una comprobación rápida:
```bash
bash ./mvnw test
```

El resultado y el número de pruebas pueden variar a medida que se agregan regresiones. La
validación mínima es `BUILD SUCCESS` sin fallos ni errores; el Dockerfile ejecuta estas pruebas
durante el empaquetado.
La puerta de calidad completa es `BUILD SUCCESS` y cero fallos con:

```bash
bash ./mvnw --batch-mode clean verify
```

La cantidad de pruebas puede cambiar cuando se agregan regresiones; no se debe fijar un número
histórico. Para consultar el resumen real de Maven en Windows:

```powershell
Get-ChildItem target\surefire-reports\*.txt | Get-Content | Select-String "Tests run"
```

Si el entorno local no tiene Docker/PostgreSQL disponible, ejecuta al menos la fase de compilación
y documenta esa limitación; no presentes una ejecución parcial como prueba completa.

## Imagen Docker

La puerta de calidad es `bash ./mvnw --batch-mode clean verify` o el workflow de CI. No uses el
resultado de construir una imagen Docker como sustituto de esa validación: las opciones Maven del
Dockerfile pueden cambiar según el flujo de despliegue.

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

## Direcciones de origen y destino de rutas

Las propiedades opcionales `originAddress` y `destinationAddress` se guardan en las columnas
`routes.origin_address` y `routes.destination_address`. Antes de desplegar este cambio, aplica
la migración `004l-ddl-add-route-addresses` del repositorio DB-Structure; sin esa migración,
Hibernate no encontrará las columnas al leer o guardar rutas.

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

Para Render Free se puede usar la API HTTPS de Brevo, sin depender de SMTP:

```env
APP_MAIL_ENABLED=true
MAIL_PROVIDER=brevo
APP_MAIL_FROM=gpsguardianescolar@gmail.com
APP_MAIL_FROM_NAME=GPS Guardian Escolar
BREVO_API_KEY=<clave-secreta-de-brevo>
BREVO_API_URL=https://api.brevo.com/v3/smtp/email
```

`BREVO_API_KEY` solo debe configurarse en las variables privadas del servicio. El remitente
debe estar verificado en Brevo; la cuenta y los límites de envío deben validarse antes de
usar el proveedor con destinatarios reales.

La selección acepta únicamente `smtp` y `brevo`. Un proveedor no seleccionado no necesita su
clave para que la aplicación arranque; la clave se valida solamente cuando se intenta enviar
un correo con ese proveedor.

La selección de proveedor conserva los flujos actuales de verificación de correo,
recuperación de contraseña, confirmación de dispositivo, doble factor y notificaciones. No
se incluyen claves en el código ni en `.env.example`.

