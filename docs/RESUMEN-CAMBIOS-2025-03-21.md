# Resumen de cambios agregados al repositorio

**Documento generado:** sábado **21 de marzo de 2025**, **16:30** (hora local de referencia)

---

## Backend (`backend-management-service`)

### Dependencias (`pom.xml`)

- **Spring Security** para BCrypt y configuración de seguridad HTTP.
- Mantiene **Spring Data JPA**, **Validation**, **Web**, **PostgreSQL**, **H2** (runtime para desarrollo/pruebas).

### Configuración

- **`application.yaml`**: conexión a **PostgreSQL** (`localhost:5432`, base `tourpresence`, usuario `synexis`), Hibernate `ddl-auto: update`, puerto **8080**.
- **`src/test/resources/application.yaml`**: **H2 en memoria** para tests sin depender de Docker.

### Modelos (`com.synexis.management_service.models`)

- **`Role`**: enum `CLIENT`, `SOCIO` (persistido como texto en BD).
- **`User`**: entidad JPA tabla `users` — `id`, `email` (único), `password_hash` (BCrypt), `role`, `profile_picture_path` (opcional, ruta bajo `picProfile/`).

### Persistencia

- **`UserRepository`**: `JpaRepository<User, Long>` + `existsByEmailIgnoreCase`, `findByEmailIgnoreCase`.

### DTOs

- **`RegisterRequest`**: `email`, `password`, `role`, `profilePicturePath` opcional (validación Bean Validation).
- **`RegisterResponse`**: `id`, `email`, `role`, `profilePicturePath` (sin contraseña ni hash).

### Servicio

- **`AuthService`**: registro — normaliza email, evita duplicados, codifica contraseña, guarda usuario y devuelve respuesta.

### API REST

- **`AuthController`**: `POST /register` → **201** con cuerpo JSON de registro.
- **`Controller`** (raíz del paquete): `GET /ping` — comprobación de salud (`status`, `timestamp`).

### Seguridad y errores

- **`SecurityConfig`**: rutas públicas `/ping` y `/register`; resto requiere autenticación; CSRF deshabilitado para API; bean **`BCryptPasswordEncoder`**.
- **`EmailAlreadyExistsException`** y **`RestExceptionHandler`**: **409** si el email existe; **400** si falla la validación del cuerpo.

### Pruebas

- **`UserRepositoryTest`**: integración con contexto Spring, persistencia y lectura por email.
- **`ManagementServiceApplicationTests`**: carga de contexto.

### Recursos en disco

- Carpeta **`picProfile/`** con **`.gitkeep`** para versionar el directorio de imágenes de perfil (la BD solo guarda la ruta).

---

## Raíz del proyecto (`Synaxis---Telepresencia-Turistica`)

### Scripts (Windows)

- **`start-database.bat`**: ejecuta `docker compose up -d` para PostgreSQL; mensaje recordando que Postman/API usan el puerto **8080**.
- **`start-backend.bat`**: `mvn spring-boot:run` en `backend-management-service`.

### Postman

- **`postman/Synaxis-Management-Service.postman_collection.json`**: colección con `GET /ping`, `POST /register` (CLIENT, SOCIO, validación, duplicado), variable `baseUrl`, scripts de prueba básicos.

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
