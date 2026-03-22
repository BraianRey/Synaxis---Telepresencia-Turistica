# Resumen de cambios agregados al repositorio

**Documento generado:** sábado **21 de marzo de 2025**, **16:30** (hora local de referencia)

---

## Backend (`backend-management-service`)

### Dependencias (`pom.xml`)

- **Spring Security** para BCrypt y configuración de seguridad HTTP.
- Mantiene **Spring Data JPA**, **Validation**, **Web**, **PostgreSQL**, **H2** (runtime para desarrollo/pruebas).

### Configuración

- **`application.yaml`**: conexión a **PostgreSQL** (`localhost:5432`, base `tourpresence`, usuario `synexis`), Hibernate `ddl-auto: update`, puerto **8080**.
- **`src/test/resources/application.yaml`**: **H2 en memoria** para tests automatizados (sin PostgreSQL local).

### Modelos (`com.synexis.management_service.models`)

- **`UserBase`** (`@MappedSuperclass`): campos alineados con la tabla lógica **`User`** en `Documentos/SQLBD.sql` — `name`, `email`, `password_hash`, `status`, `language`, `created_at`, `terms_accepted`, `user_role` (columna física; equivale a `rol` en el SQL lógico), `pic_directory` (equivale a `picDirectory`).
- **`Client`**: extiende `UserBase`; tabla `clients` (sin columnas extra respecto a `User` en el modelo lógico).
- **`Partner`**: extiende `UserBase`; tabla `partners` + **`area_id`**, **`availability_status`** (tabla lógica `Partner` en SQLBD.sql).
- Enums: `UserStatus`, `UserLanguage`, `UserRole` (`client` / `partner`), `PartnerAvailabilityStatus` (valores en minúsculas, coherentes con los ENUM del SQL / SQLBD.sql).

### Persistencia

- **`ClientRepository`** y **`PartnerRepository`**: un repositorio por tabla/entidad; consultas por email insensible a mayúsculas.

### DTOs

- **`RegisterClientRequest`** / **`RegisterClientResponse`**: `name`, `termsAccepted`, `language` opcional (por defecto `es`), `picDirectory` opcional.
- **`RegisterPartnerRequest`** / **`RegisterPartnerResponse`**: lo mismo + **`areaId`** obligatorio; respuesta incluye `availabilityStatus`.

### Servicios

- **`AuthService`**: solo **autenticación** a nivel de contraseña — `encodePassword`, `passwordMatches` (BCrypt); el registro no va aquí.
- **`ClientService`**: alta de clientes; email único solo dentro de `clients`.
- **`PartnerService`**: alta de socios; email único solo dentro de `partners`. La misma persona puede tener el mismo email en **ambas** tablas (cliente y socio).

### API REST

- **`UserController`**: `POST /register/client` y `POST /register/partner` → **201**.
- **`Controller`** (raíz del paquete): `GET /ping` — comprobación de salud (`status`, `timestamp`).

### Seguridad y errores

- **`SecurityConfig`**: rutas públicas `/ping`, `/register/client`, `/register/partner`; CSRF deshabilitado para API; bean **`BCryptPasswordEncoder`**.
- **`EmailAlreadyExistsException`** y **`RestExceptionHandler`**: **409** / **400** según corresponda.

### Pruebas

- **`ClientRepositoryTest`**, **`PartnerRepositoryTest`**: persistencia por tabla.
- **`ManagementServiceApplicationTests`**: carga de contexto.

### Recursos en disco

- Carpeta **`picProfile/`** con **`.gitkeep`** para versionar el directorio de imágenes de perfil (la BD solo guarda la ruta).

---

## Raíz del proyecto (`Synaxis---Telepresencia-Turistica`)

### Scripts (Windows)

- **`start-database.bat`**: recordatorio en consola para configurar **PostgreSQL local** (pgAdmin / servicio Windows); credenciales alineadas con `application.yaml`. No usa Docker.
- **`start-backend.bat`**: `mvn spring-boot:run` en `backend-management-service`.

### Postman

- **`postman/Synaxis-Management-Service.postman_collection.json`**: `GET /ping`, `POST /register/client`, `POST /register/partner`, validación y duplicado; variable `baseUrl`.

### Base de datos

- **PostgreSQL instalado localmente** (recomendado gestionar con **pgAdmin** o DBeaver). No hay `docker-compose` en el repositorio.

---

## Carpeta `Documentos` (fuera del árbol principal del repo Git de Synaxis)

Si también versionas cambios allí en otro remoto o carpeta:

- Actualización del **estándar de codificación** (JDK 21, Spring Boot 4.x alineado al backend, commits Conventional Commits en **inglés**).

*(Incluye solo lo que hayas confirmado en tu commit.)*

---

## Notas para el equipo

- El registro **no sube archivos**: solo guarda la **ruta** de la imagen; la subida real sería un endpoint o flujo aparte.
- Las apps Android (`mobile-client` / `mobile-partner`) pueden seguir con contratos distintos al backend hasta que se alineen `ApiService` y DTOs en Kotlin.

---

*Fin del resumen.*
