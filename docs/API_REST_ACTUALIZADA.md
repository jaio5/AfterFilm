# API REST ACTUALIZADA - PelisApp

## Autenticación

### Registro
**POST** `/api/auth/register`
- **Body:**
```json
{
  "username": "usuario1",
  "email": "usuario1@email.com",
  "password": "123456",
  "displayName": "Usuario Uno"
}
```
- **Response:**
```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresAt": 1700000000,
  "refreshToken": "...",
  "user": {
    "id": 1,
    "username": "usuario1",
    "displayName": "Usuario Uno",
    "criticLevel": 1,
    "roles": ["USER"]
  }
}
```

### Login
**POST** `/api/auth/login`
- **Body:**
```json
{
  "username": "usuario1",
  "password": "123456"
}
```
- **Response:** igual que registro.

### Refresh Token
**POST** `/api/auth/refresh`
- **Body:**
```json
{
  "refreshToken": "..."
}
```
- **Response:** igual que login.

## Usuario actual

### Perfil
**GET** `/api/users/me`
- **Header:** `Authorization: Bearer <accessToken>`
- **Response:**
```json
{
  "id": 1,
  "username": "usuario1",
  "displayName": "Usuario Uno",
  "criticLevel": 1,
  "roles": ["USER"]
}
```

### Reviews del usuario
**GET** `/api/users/me/reviews?page=0&size=10`
- **Header:** `Authorization: Bearer <accessToken>`
- **Response:**
```json
{
  "content": [ ...reviews... ],
  "pageable": { ... },
  "totalPages": 1,
  "totalElements": 2
}
```

## Películas

### Listado
**GET** `/api/movies?page=0&size=12`
- **Response:**
```json
{
  "content": [
    {
      "id": 123,
      "title": "Matrix",
      "description": "Película de ciencia ficción...",
      "posterPath": "/data/movies/movie_123.jpg",
      "posterLocalPath": "movie_123.jpg",
      "releaseDate": "1999-03-31",
      "runtimeMinutes": 136,
      "tmdbId": 603,
      "categories": ["Acción", "Ciencia ficción"]
    }
  ],
  "pageable": { ... },
  "totalPages": 10,
  "totalElements": 120
}
```

### Por categoría
**GET** `/api/movies/by-category?category=Acción&page=0&size=12`
- **Response:** igual que listado.

### Detalles
**GET** `/api/movies/{id}/details`
- **Response:**
```json
{
  "id": 123,
  "tmdbId": 603,
  "title": "Matrix",
  "overview": "Película de ciencia ficción...",
  "posterPath": "/data/movies/movie_123.jpg",
  "releaseDate": "1999-03-31",
  "cast": ["Keanu Reeves", "Laurence Fishburne"],
  "castMembers": [
    {
      "tmdbId": 12345,
      "name": "Keanu Reeves",
      "character": "Neo",
      "profilePath": "/data/images/profiles/actor_12345.jpg",
      "profileLocalPath": "actor_12345.jpg",
      "profileUrl": "http://localhost:8080/data/images/profiles/actor_12345.jpg"
    }
  ],
  "directors": [
    {
      "tmdbId": 54321,
      "name": "Lana Wachowski",
      "job": "Director",
      "department": "Directing",
      "profilePath": "/data/images/profiles/director_54321.jpg",
      "profileLocalPath": "director_54321.jpg",
      "profileUrl": "http://localhost:8080/data/images/profiles/director_54321.jpg"
    }
  ],
  "comments": [
    {
      "id": 1,
      "author": "usuario1",
      "text": "Me encantó la película",
      "rating": 5
    }
  ]
}
```

## Reviews

### Crear review
**POST** `/api/reviews`
- **Header:** `Authorization: Bearer <accessToken>`
- **Body:**
```json
{
  "userId": 1,
  "movieId": 123,
  "text": "Me encantó la película, muy recomendable",
  "stars": 4
}
```
- **Response:**
```json
{
  "id": 10,
  "user": { "id": 1, "username": "usuario1" },
  "movie": { "id": 123, "title": "Matrix" },
  "text": "Me encantó la película, muy recomendable",
  "stars": 4,
  "createdAt": "2026-02-18T12:34:56.789Z",
  "likesCount": 0
}
```

### Listar reviews por película
**GET** `/api/reviews/movie/{movieId}`
- **Response:**
```json
[
  {
    "id": 10,
    "user": { "id": 1, "username": "usuario1" },
    "movie": { "id": 123, "title": "Matrix" },
    "text": "Me encantó la película, muy recomendable",
    "stars": 4,
    "createdAt": "2026-02-18T12:34:56.789Z",
    "likesCount": 0
  }
]
```

### Like a review
**POST** `/api/reviews/{id}/like?userId=1`
- **Header:** `Authorization: Bearer <accessToken>`
- **Response:** 200 OK (sin body).

## Modelos de datos (DTO)

### UserDTO
- `id`: Long
- `username`: String
- `displayName`: String
- `criticLevel`: Integer
- `roles`: List<String>

### MovieListDTO
- `id`: Long
- `title`: String
- `description`: String
- `posterPath`: String
- `posterLocalPath`: String
- `releaseDate`: String
- `runtimeMinutes`: Integer
- `tmdbId`: Long
- `categories`: List<String>

### MovieDetailsDTO
- `id`: Long
- `tmdbId`: Long
- `title`: String
- `overview`: String
- `posterPath`: String
- `releaseDate`: String
- `cast`: List<String>
- `castMembers`: List<CastDTO>
- `directors`: List<CrewDTO>
- `comments`: List<CommentDTO>

### CastDTO
- `tmdbId`: Long
- `name`: String
- `character`: String
- `profilePath`: String
- `profileLocalPath`: String
- `profileUrl`: String

### CrewDTO
- `tmdbId`: Long
- `name`: String
- `job`: String
- `department`: String
- `profilePath`: String
- `profileLocalPath`: String
- `profileUrl`: String

### ReviewDTO
- `id`: Long
- `user`: SimpleUserDTO
- `movie`: SimpleMovieDTO
- `text`: String
- `stars`: Integer
- `createdAt`: String
- `likesCount`: Long

### ReviewCreateRequest
- `userId`: Long
- `movieId`: Long
- `text`: String
- `stars`: Integer

### Ejemplo de imagen de película
- Campo `posterPath`: `"/data/movies/movie_123.jpg"`
- Para mostrar la carátula en la app, usar la URL completa: `"http://localhost:8080/data/movies/movie_123.jpg"`

## Errores
- Formato estándar:
```json
{
  "error": "Mensaje de error"
}
```
- Códigos HTTP: 400, 401, 404, 500 según el caso.

## Notas
- Todos los endpoints que requieren autenticación deben enviar el header `Authorization: Bearer <accessToken>`.
- Los endpoints públicos permiten listar películas, detalles, reviews, comentarios, reparto y equipo.
- Las imágenes de carátulas y perfiles se exponen como rutas relativas, pero se deben consumir como URLs completas desde la app Android.
