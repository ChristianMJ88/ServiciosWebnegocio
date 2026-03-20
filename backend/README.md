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

2. Ejecuta el backend:

```bash
mvn spring-boot:run
```

## Endpoints iniciales

- `GET /actuator/health`
- `GET /api/v1/publico/sucursales`
- `GET /api/v1/publico/servicios`
- `GET /api/v1/publico/disponibilidad/franjas`
- `POST /api/v1/publico/citas`
- `POST /api/v1/auth/iniciar-sesion`
- `POST /api/v1/auth/registrar-cliente`

## Estado actual

Esta base deja:

- estructura modular en espanol
- seguridad JWT preparada
- configuracion de Docker y MySQL
- migracion inicial de base de datos
- controladores y servicios base para comenzar implementacion

Lo siguiente recomendado es implementar:

1. persistencia JPA para sucursales, servicios y usuarios
2. autenticacion real con usuarios y roles en base de datos
3. calculo real de disponibilidad
4. creacion transaccional de citas con validacion de conflicto

