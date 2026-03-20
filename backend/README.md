# Backend de Agenda

Base inicial del backend Spring Boot para evolucionar la plataforma de agenda de servicios.

## Stack

- Java 21
- Spring Boot
- Spring Security con JWT
- Spring Data JPA
- Flyway
- MySQL
- Docker

## Estructura

```text
backend/
  src/main/java/com/techprotech/agenda
    compartido/
    seguridad/
    modulos/
      autenticacion/
      sucursales/
      servicios/
      disponibilidad/
      citas/
  src/main/resources/
    application.yml
    db/migration/
```

## Arranque local

1. Levanta MySQL:

```bash
docker compose up -d mysql
```

MySQL queda expuesto localmente en `localhost:3302` para no chocar con instalaciones que ya usan `3306`.

La imagen local recomendada es `mysql:8.0` para evitar el warning de compatibilidad que Flyway muestra con `8.4`.

2. Ejecuta el backend:

```bash
mvn spring-boot:run
```

Spring Boot usa configuracion en:

- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

No hay `application.properties` porque en este backend se esta usando formato YAML.

Si ya habias levantado Docker con una version distinta de MySQL, puede que necesites recrear el volumen:

```bash
docker compose down -v
docker compose up -d mysql
```

## Endpoints iniciales

- `GET /actuator/health`
- `GET /api/v1/publico/sucursales`
- `GET /api/v1/publico/servicios`
- `GET /api/v1/publico/disponibilidad/franjas`
- `POST /api/v1/publico/citas`
- `POST /api/v1/auth/iniciar-sesion`
- `POST /api/v1/auth/registrar-cliente`
- `POST /api/v1/auth/refrescar-token`
- `POST /api/v1/auth/cerrar-sesion`

## Produccion

1. Copia el ejemplo de variables:

```bash
cp .env.example .env
```

2. Ajusta secretos, base de datos y origenes CORS.

3. Levanta el backend:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Notas:

- En `prod`, Hibernate queda en `validate`.
- El frontend Angular ya esta preparado para trabajar en modo `backend-first`.
- Si necesitas una transicion temporal, puedes activar `allowLegacyFallback` en `src/environments`.
