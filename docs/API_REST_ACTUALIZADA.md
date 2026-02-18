# Documentación completa de la API REST de PelisApp (actualizada)

Esta documentación describe todos los endpoints públicos y de administración de la API REST de PelisApp. Está orientada a desarrolladores que deseen crear aplicaciones cliente (por ejemplo, en Kotlin) y explica cómo consumir la API, distinguir tipos de usuario, manejar autenticación, roles, paginación, errores y lógica de negocio.

---

## 1. Autenticación y flujo de usuario

### Registro
- **POST** `/api/auth/register`
- **Body:**
```json
{
  "username": "nuevoUsuario",
  "email": "correo@dominio.com",
  "password": "claveSegura"
}
```
- **Response:**
```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "user": {
    "id": 1,
    "username": "nuevoUsuario",
    "email": "correo@dominio.com",
    "displayName": "nuevoUsuario",
    "criticLevel": 0,
    "roles": ["ROLE_USER"]
  }
}
```

### Login
- **POST** `/api/auth/login`
- **Body:**
```json
{
  "username": "usuario1",
  "password": "clave"
}
```
- **Response:** igual que registro. El campo `roles` puede contener, por ejemplo, `["ROLE_USER"]` o `["ROLE_ADMIN"]`.

### Refresh Token
- **POST** `/api/auth/refresh`
- **Body:**
```json
{ "refreshToken": "..." }
```
- **Response:** igual que login.

### ¿Cómo distinguir un usuario admin de uno normal?
- El campo `roles` en la respuesta de login/registro/refresh y en `/api/users/me` indica los roles del usuario.
- Si el array incluye `ROLE_ADMIN`, el usuario es administrador. Si solo incluye `ROLE_USER`, es un usuario normal.
- Ejemplo de usuario admin:
```json
{
  "id": 2,
  "username": "admin",
  "email": "admin@email.com",
  "displayName": "Administrador",
  "criticLevel": 0,
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```
- En tu app Kotlin, tras el login, revisa el array `roles` para decidir a qué pantalla derivar al usuario.

---

## 2. Endpoints principales

### Usuarios

#### Obtener datos del usuario autenticado
- **GET** `/api/users/me`
- **Headers:** `Authorization: Bearer <token>`
- **Response:**
```json
{
  "id": 1,
  "username": "usuario1",
  "email": "usuario1@email.com",
  "displayName": "usuario1",
  "criticLevel": 0,
  "roles": ["ROLE_USER"]
}
```

#### Obtener reviews del usuario autenticado (paginado)
- **GET** `/api/users/me/reviews?page=0&size=10`
- **Headers:** `Authorization: Bearer <token>`
- **Response:**
```json
{
  "content": [ { "id": 1, "movieId": 2, "text": "Muy buena", ... }, ... ],
  "totalElements": 20,
  "totalPages": 2,
  "number": 0
}
```

#### Buscar usuario por email o username (admin)
- **GET** `/api/admin/users/search/email?value=correo`
- **GET** `/api/admin/users/search/username?value=nombre`
- **Headers:** `Authorization: Bearer <token>` (admin)

---

### Películas

> **Nota:** La API expone los datos de películas almacenados en la base de datos propia, no directamente desde TMDB. El backend puede sincronizarse con TMDB, pero los endpoints REST trabajan sobre la base de datos local.

#### Listar películas (paginado)
- **GET** `/api/movies?page=0&size=12`
- **Response:**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Matrix",
      "overview": "...",
      "posterUrl": "/api/movies/1/poster",
      "releaseDate": "1999-03-31",
      "categories": ["Acción", "Ciencia Ficción"]
    }
  ],
  "totalElements": 100,
  "totalPages": 9,
  "number": 0
}
```

#### Detalles de una película
- **GET** `/api/movies/{id}/details`
- **Response:**
```json
{
  "id": 1,
  "title": "Matrix",
  "overview": "...",
  "posterUrl": "/api/movies/1/poster",
  "releaseDate": "1999-03-31",
  "categories": ["Acción", "Ciencia Ficción"],
  "cast": [
    { "id": 10, "name": "Keanu Reeves", "character": "Neo", "profileUrl": "/api/actors/10/profile" }
  ],
  "directors": [
    { "id": 20, "name": "Lana Wachowski", "profileUrl": "/api/directors/20/profile" }
  ]
}
```

#### Obtener carátula/poster de una película
- **GET** `/api/movies/{id}/poster`
- **Response:** Imagen JPEG/PNG (header `Content-Type: image/jpeg`)

#### Archivos de una película
- **GET** `/api/movies/{id}/files`
- **Response:**
```json
{
  "movieId": 1,
  "files": [
    { "name": "matrix.mp4", "size": 123456789, "downloadUrl": "/movies/download/1/matrix.mp4", "streamUrl": "/movies/stream/1/matrix.mp4" }
  ],
  "totalFiles": 1
}
```

#### Películas por categoría
- **GET** `/api/movies/by-category?category=accion&page=0&size=12`

---

### Reviews

#### Crear review
- **POST** `/api/reviews`
- **Body:**
```json
{
  "userId": 1,
  "movieId": 2,
  "text": "Muy buena",
  "stars": 5
}
```
- **Response:**
```json
{
  "id": 10,
  "userId": 1,
  "movieId": 2,
  "text": "Muy buena",
  "stars": 5,
  "createdAt": "2026-02-16T12:00:00Z"
}
```

#### Like a una review
- **POST** `/api/reviews/{id}/like?userId=1`

---

### Salud del sistema

#### Estado general
- **GET** `/api/system/health`
- **Response:**
```json
{
  "database": { "connected": true, "message": "Base de datos conectada" },
  "tmdb": { "connected": true }
}
```

#### Estado de un servicio
- **GET** `/api/system/health/{service}`

---

## 3. Endpoints de administración (solo ADMIN)

- Todos bajo `/api/admin/*` y requieren `Authorization: Bearer <token>` de admin.
- Ejemplos:
  - Confirmar email: `POST /api/admin/users/{userId}/confirm-email`
  - Banear usuario: `POST /api/admin/users/{userId}/ban`
  - Eliminar usuario: `POST /api/admin/users/{userId}/delete`
  - Descargar película de TMDB: `POST /api/admin/tmdb/load-movie/{tmdbId}`
  - Carga masiva de películas populares: `POST /api/admin/tmdb/bulk-load?page=1`
  - Recargar posters: `POST /api/admin/images/reload`
  - Moderación: `/api/admin/moderation/*`

---

## 4. Autenticación y roles

- Los endpoints protegidos requieren el header `Authorization: Bearer <token>`.
- El token es un JWT. Se obtiene en login/registro y se refresca con `/api/auth/refresh`.
- El campo `roles` del usuario indica si es admin (`ROLE_ADMIN`) o usuario normal (`ROLE_USER`).
- Los endpoints `/api/admin/*` solo aceptan tokens de admin.

---

## 5. Paginación y filtros

- Los endpoints que devuelven listas usan paginación Spring: parámetros `page` y `size`.
- El objeto de respuesta incluye `content`, `totalElements`, `totalPages`, `number`.
- Algunos endpoints permiten filtrar por texto, categoría, usuario, etc.

---

## 6. CRUD y lógica de negocio

- Crear, editar, borrar y listar ítems (películas, reviews, usuarios) según permisos y roles.
- Validaciones: campos obligatorios en cada operación (ver modelos de datos).
- Relación usuario-ítems: por ejemplo, `/api/users/me/reviews` para "mis reviews".

---

## 7. Reviews y likes

- Crear review: `/api/reviews` (ver ejemplo arriba).
- Like a review: `/api/reviews/{id}/like?userId=...`.
- Las reviews se asocian a usuario y película por sus IDs.

---

## 8. Archivos y multimedia

- Subida/descarga/visualización de archivos asociados a películas vía `/api/movies/{id}/files`.
- Formato: JSON con URLs de descarga y streaming.
- Las imágenes de carátulas de películas se obtienen vía `/api/movies/{id}/poster`.
- Las imágenes de reparto/directores vía `/api/actors/{id}/profile` y `/api/directors/{id}/profile`.

---

## 9. Errores y estados

- Errores: códigos HTTP estándar (400, 401, 403, 404, 500).
- Cuerpo de error: `{ "success": false, "message": "..." }` o `{ "error": "..." }`.

---

## 10. Otros endpoints

- Estado del sistema: `/api/system/health` y `/api/system/health/{service}`.
- Preferencias, notificaciones, etc.: consultar documentación específica si aplica.

---

## 11. Ejemplo de flujo completo en Kotlin (pseudocódigo)

```kotlin
val loginResp = post("/api/auth/login", LoginRequest(...))
val token = loginResp.accessToken
val user = loginResp.user
if (user.roles.contains("ROLE_ADMIN")) {
    // Ir a pantalla de admin
} else {
    // Ir a pantalla de usuario normal
}
// Usar token en siguientes peticiones:
val peliculas = get("/api/movies?page=0&size=12", headers = mapOf("Authorization" to "Bearer $token"))
```

---

## 12. Recomendaciones para clientes Kotlin

- Usar librerías como Ktor o Retrofit para consumir la API.
- Serializar/deserializar JSON con kotlinx.serialization o Moshi.
- Añadir el header `Authorization: Bearer <token>` tras login.
- Tras el login, revisa el campo `roles` del usuario para saber si es admin (`ROLE_ADMIN`) o usuario normal (`ROLE_USER`).
- Manejar errores HTTP y estructura de error JSON.

---

Para dudas o detalles adicionales, consulta el código fuente o contacta con el equipo de backend.

