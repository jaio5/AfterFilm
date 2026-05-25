# AfterFilm

AfterFilm es una aplicacion web para gestionar y descubrir peliculas, series y libros. Incluye catalogo, resenas, puntuaciones, perfiles sociales, chat, panel de administracion e integraciones con APIs externas.

## Funcionalidades

- Catalogo de peliculas con busqueda, categorias y detalle.
- Catalogo de series importadas desde TMDB.
- Catalogo de libros importados desde Google Books.
- Registro, login, roles y confirmacion de email.
- Resenas, puntuaciones y likes.
- Perfiles de usuario, seguimiento, feed social y chat.
- Panel de administracion para usuarios, contenido, importaciones y moderacion.
- Moderacion de contenido con Ollama y fallback local.
- Descarga local de imagenes de posters, actores y directores.
- Endpoints REST para integracion con clientes externos.

## Stack

- Java 17
- Spring Boot 3.2.10
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- H2 para tests
- Thymeleaf
- Maven
- Caffeine Cache
- TMDB API
- Google Books API
- Ollama

## Estructura

```text
src/main/java/alicanteweb/pelisapp/
  config/         Configuracion de Spring
  controller/     APIs REST y controladores administrativos
  controller/web/ Controladores de vistas HTML
  dto/            Objetos de transferencia
  entity/         Entidades JPA
  repository/     Repositorios JPA
  security/       Seguridad, sesiones y JWT
  service/        Logica de negocio
  tmdb/           Cliente y configuracion TMDB

src/main/resources/
  templates/      Vistas Thymeleaf
  static/         CSS, JS e imagenes estaticas
  sql/            Scripts SQL
```

## Ejecucion local

Requisitos:

- Java 17+
- PostgreSQL 16+ o Docker
- Token de TMDB si se van a importar peliculas o series

Crear base de datos:

```sql
CREATE DATABASE pelisapp;
```

Ejecutar:

```bash
./mvnw.cmd spring-boot:run
```

Con Docker:

```bash
docker compose up --build
```

Por defecto la aplicacion usa el puerto configurado en `src/main/resources/application.properties`.

## Tests

Los tests usan perfil `test` con H2 en memoria para no depender de PostgreSQL ni de servicios externos.

```bash
./mvnw.cmd test
```

## Documentacion

La documentacion tecnica esta en `docs/`.

La memoria del TFG generada esta en:

- `docs/MEMORIA_TFG_PelisApp.md`
- `docs/MEMORIA_TFG_PelisApp.docx`
