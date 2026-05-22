# 2025-2026

# Tecnico Superior en Desarrollo de Aplicaciones Multiplataforma

# Proyecto de Desarrollo de Aplicaciones Multiplataforma

# AfterFilm: plataforma web para catalogo, valoracion y gestion de contenido audiovisual

**Alumno:** [Javier Barceló García]  
**Tutor:** [Víctor Palomar]  
**Centro:** [FP Fempa]  
**Curso:** 2025-2026  
**Fecha:** Mayo de 2026

---

# INDICE

1. Introduccion  
2. Tecnologias usadas. Conceptos  
3. Funcionamiento  
4. Descripcion grafica de la aplicacion  
5. Inicio de la App  
6. Presupuesto  
7. Glosario  
8. Referencias  
9. Anexos

---

# 1. Introduccion

AfterFilm es una aplicacion web desarrollada como proyecto final del ciclo de Desarrollo de Aplicaciones Multiplataforma. El objetivo principal del proyecto es construir una plataforma completa para consultar, organizar, valorar y administrar contenido audiovisual, principalmente peliculas y series, incorporando ademas funcionalidades sociales y herramientas de administracion.

La aplicacion permite a los usuarios registrarse, iniciar sesion, explorar un catalogo de peliculas, series y libros, consultar fichas detalladas, publicar resenas, puntuar contenido, seguir a otros usuarios y participar en una experiencia social centrada en recomendaciones y valoraciones. Desde el punto de vista administrativo, el sistema permite gestionar usuarios, importar contenido desde APIs externas, revisar resenas, moderar contenido y comprobar el estado de los servicios conectados.

El proyecto se ha planteado como una aplicacion realista, no solo como una demostracion aislada. Por ello se han incluido aspectos habituales en aplicaciones profesionales: autenticacion segura, roles de usuario, persistencia en base de datos relacional, integracion con servicios externos, separacion por capas, validacion, manejo de errores, carga de imagenes, almacenamiento local, API REST, vistas web y pruebas automatizadas.

## 1.1 Justificacion del proyecto

Las plataformas de catalogacion audiovisual son un caso de uso muy adecuado para un proyecto de DAM porque combinan varias areas importantes del desarrollo de software:

- Gestion de usuarios y seguridad.
- Consumo de APIs externas.
- Persistencia de datos complejos.
- Relaciones entre entidades.
- Interfaz web dinamica.
- Administracion de contenido.
- Arquitectura backend escalable.
- Pruebas y documentacion tecnica.

AfterFilm permite aplicar estos conocimientos en un dominio conocido por cualquier usuario: peliculas, series, libros, valoraciones y recomendaciones. Esto facilita comprender el funcionamiento de la aplicacion y, al mismo tiempo, permite trabajar con un modelo de datos suficientemente amplio.

## 1.2 Objetivos generales

El objetivo general del proyecto es desarrollar una aplicacion web funcional que permita gestionar un catalogo multimedia y la actividad de los usuarios alrededor de ese contenido.

Los objetivos principales son:

- Crear una aplicacion basada en Spring Boot y Java 17.
- Implementar un sistema de autenticacion con usuarios, roles y confirmacion de email.
- Permitir la consulta de peliculas, series y libros.
- Integrar la API de TMDB para importar informacion de peliculas y series.
- Integrar Google Books para importar informacion de libros.
- Crear un sistema de resenas, puntuaciones y likes.
- Desarrollar funcionalidades sociales como perfiles, seguimiento y chat.
- Construir un panel de administracion para gestionar usuarios y contenido.
- Incorporar moderacion automatica mediante IA local con Ollama.
- Almacenar imagenes localmente para mejorar rendimiento y disponibilidad.
- Proporcionar una API REST para consumir datos desde otros clientes, como una futura aplicacion movil.
- Documentar la arquitectura, configuracion, base de datos y pruebas.

## 1.3 Alcance

El alcance del proyecto incluye tanto la parte publica de usuario como la parte privada de administracion.

La parte publica permite:

- Navegar por el catalogo de peliculas.
- Navegar por el catalogo de series.
- Navegar por el catalogo de libros.
- Ver detalles de cada elemento.
- Consultar reparto, directores, imagenes y descripcion.
- Registrarse e iniciar sesion.
- Escribir resenas y puntuar contenido.
- Dar like a resenas.
- Consultar perfiles de otros usuarios.
- Seguir y dejar de seguir usuarios.
- Usar un sistema de chat entre usuarios.

La parte administrativa permite:

- Consultar usuarios registrados.
- Banear, desbanear o eliminar usuarios.
- Confirmar emails de usuarios.
- Importar peliculas desde TMDB.
- Ejecutar cargas masivas de contenido.
- Importar series y libros.
- Reparar o recargar imagenes.
- Revisar resenas y moderaciones.
- Consultar el estado del sistema: base de datos, TMDB, Ollama, email y servidor.

## 1.4 Metodologia de trabajo

El desarrollo se ha realizado de forma incremental. Primero se construyo la base del proyecto con Spring Boot, la conexion a base de datos y las entidades principales. Posteriormente se implementaron los servicios de negocio, los controladores web, los endpoints REST y las vistas. Finalmente se incorporaron funcionalidades avanzadas como administracion, carga masiva, moderacion, chat, gestion de imagenes y pruebas.

Se ha seguido una arquitectura por capas para mantener el codigo organizado:

- Capa de presentacion: controladores y plantillas Thymeleaf.
- Capa de aplicacion: servicios con la logica de negocio.
- Capa de dominio: entidades, DTOs y reglas principales.
- Capa de persistencia: repositorios JPA.
- Capa de infraestructura: integraciones con TMDB, Google Books, Ollama, email y almacenamiento local.

---

# 2. Tecnologias usadas. Conceptos

## 2.1 Java 17

Java 17 es el lenguaje principal del proyecto. Se ha utilizado por ser una version LTS estable, compatible con Spring Boot 3 y adecuada para aplicaciones empresariales. Java permite trabajar con orientacion a objetos, tipado fuerte, excepciones, colecciones, streams y un ecosistema muy amplio de librerias.

En AfterFilm se utiliza Java para:

- Definir entidades JPA.
- Crear servicios de negocio.
- Implementar controladores web y REST.
- Gestionar seguridad.
- Consumir APIs externas.
- Validar datos.
- Manejar errores.

## 2.2 Spring Boot 3.2.10

Spring Boot es el framework principal de la aplicacion. Facilita la creacion de aplicaciones Java modernas reduciendo configuracion manual y proporcionando integracion con seguridad, persistencia, web, validacion, cache y testing.

En el proyecto se utilizan varios modulos:

- Spring Web: controladores MVC y REST.
- Spring Data JPA: acceso a base de datos.
- Spring Security: autenticacion y autorizacion.
- Spring Mail: envio de correos.
- Spring Validation: validacion de formularios y DTOs.
- Spring Cache: cache con Caffeine.
- Spring Boot Test: pruebas automatizadas.

## 2.3 Maven

Maven se usa como herramienta de construccion y gestion de dependencias. El archivo `pom.xml` define las librerias necesarias, la version de Java, los plugins de compilacion y el empaquetado del proyecto.

Comandos principales:

```bash
./mvnw.cmd test
./mvnw.cmd spring-boot:run
./mvnw.cmd clean package
```

## 2.4 MySQL

MySQL es el sistema gestor de base de datos relacional usado en desarrollo y produccion local. La aplicacion guarda en MySQL usuarios, peliculas, series, libros, resenas, roles, likes, relaciones sociales, mensajes, logros y datos de moderacion.

La configuracion principal se encuentra en `src/main/resources/application.properties`.

## 2.5 JPA e Hibernate

JPA permite trabajar con objetos Java en lugar de escribir SQL manual para cada operacion. Hibernate es la implementacion ORM usada por Spring Boot.

En el proyecto se usan entidades como:

- `User`
- `Movie`
- `TvShow`
- `Book`
- `Review`
- `Actor`
- `Director`
- `Role`
- `Following`
- `Message`
- `CommentModeration`

Estas entidades se mapean a tablas relacionales mediante anotaciones como `@Entity`, `@Table`, `@Id`, `@ManyToOne`, `@ManyToMany` y `@OneToMany`.

## 2.6 Spring Security y JWT

Spring Security se encarga de proteger rutas, controlar el acceso segun roles y autenticar usuarios. Ademas, el proyecto incorpora JWT para permitir autenticacion basada en tokens.

El flujo general es:

1. El usuario envia credenciales.
2. El backend valida usuario y contrasena.
3. Se genera un access token y un refresh token.
4. El cliente envia el token en las peticiones protegidas.
5. El filtro JWT valida el token y establece la autenticacion.

Los roles principales son:

- USER
- MODERATOR
- ADMIN
- SUPERADMIN

## 2.7 Thymeleaf

Thymeleaf se usa para generar vistas HTML desde el servidor. Permite pasar datos desde los controladores a las plantillas y renderizar paginas dinamicas.

Pantallas principales:

- Inicio.
- Login.
- Registro.
- Detalle de pelicula.
- Detalle de serie.
- Detalle de libro.
- Perfil.
- Usuarios.
- Feed.
- Chat.
- Panel de administracion.

## 2.8 HTML, CSS y JavaScript

La interfaz de usuario se construye con HTML, CSS y JavaScript. Los archivos estaticos se encuentran en `src/main/resources/static`, mientras que las vistas se encuentran en `src/main/resources/templates`.

JavaScript se utiliza para interacciones dinamicas, llamadas a endpoints REST, actualizacion de estados y operaciones administrativas.

## 2.9 TMDB API

TMDB, The Movie Database, es una API externa que proporciona informacion de peliculas y series. AfterFilm la utiliza para importar:

- Titulos.
- Sinopsis.
- Fechas de estreno.
- Generos.
- Posters.
- Reparto.
- Directores.
- Series populares.
- Peliculas populares y mejor valoradas.

La integracion se realiza mediante un cliente HTTP y servicios especializados como `TMDBMovieLoaderService`, `TMDBSeriesLoaderService` y `TMDBBulkLoaderService`.

## 2.10 Google Books API

Google Books se utiliza para importar informacion de libros. El sistema permite buscar libros, guardar sus metadatos y mostrarlos dentro del catalogo de la aplicacion.

## 2.11 Ollama e IA de moderacion

Ollama permite ejecutar modelos de IA de forma local. En AfterFilm se utiliza para analizar contenido generado por usuarios y ayudar a detectar texto toxico o inapropiado.

El sistema de moderacion combina:

- Analisis automatico.
- Umbrales configurables.
- Registro de resultados.
- Revision manual desde el panel de administracion.
- Fallback local si el servicio externo no esta disponible.

## 2.12 Caffeine Cache

Caffeine se usa como sistema de cache en memoria. Mejora el rendimiento reduciendo llamadas repetidas a servicios externos o consultas repetitivas.

## 2.13 H2 para pruebas

Para las pruebas automatizadas se ha incorporado H2 como base de datos en memoria. Esto permite ejecutar `mvn test` sin depender de una instalacion MySQL local, credenciales concretas o servicios externos.

El perfil de test se define en `src/test/resources/application-test.properties`.

---

# 3. Funcionamiento

## 3.1 Vision general del sistema

AfterFilm funciona como una aplicacion cliente-servidor web. El usuario accede mediante navegador a las paginas HTML generadas por el servidor. El backend procesa las peticiones, consulta la base de datos, llama a servicios externos cuando es necesario y devuelve vistas HTML o respuestas JSON.

El flujo basico es:

1. El usuario accede a la pagina principal.
2. El sistema muestra contenido del catalogo.
3. El usuario consulta una pelicula, serie o libro.
4. Si esta autenticado puede publicar resenas o interactuar con otros usuarios.
5. Los administradores pueden acceder al panel de control.
6. Desde el panel se gestionan usuarios, contenido, importaciones y moderacion.

## 3.2 Arquitectura de capas

La arquitectura se organiza en paquetes:

```text
src/main/java/alicanteweb/pelisapp/
  controller/     Controladores web y REST
  controller/web/ Controladores de vistas publicas
  service/        Logica de negocio
  repository/     Repositorios Spring Data JPA
  entity/         Entidades del dominio
  dto/            Objetos de transferencia de datos
  security/       Seguridad y JWT
  config/         Configuracion de Spring
  exception/      Manejo de errores
  tmdb/           Integracion con TMDB
  constants/      Constantes de aplicacion
```

Esta estructura permite separar responsabilidades. Por ejemplo, un controlador no accede directamente a la base de datos, sino que llama a un servicio, y el servicio usa repositorios.

## 3.3 Registro y autenticacion

El usuario puede registrarse desde la pantalla de registro. Durante este proceso se validan los datos y se crea una cuenta en base de datos. El sistema puede enviar un correo de confirmacion si el email esta habilitado.

El inicio de sesion permite acceder a las funcionalidades privadas. La aplicacion soporta sesiones web y endpoints REST con JWT, por lo que puede ser consumida tanto desde navegador como desde una aplicacion movil.

Endpoints relacionados:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/auth/confirm-email`

## 3.4 Catalogo de peliculas

El catalogo de peliculas permite listar contenido paginado, buscar por texto, filtrar por categoria y consultar fichas detalladas.

Cada ficha puede contener:

- Titulo.
- Descripcion.
- Fecha de estreno.
- Duracion.
- Poster.
- Generos.
- Reparto.
- Directores.
- Resenas de usuarios.
- Puntuacion.

Endpoints destacados:

- `GET /api/movies`
- `GET /api/movies/{id}/details`
- `GET /api/movies/search`
- `GET /api/movies/by-category`
- `GET /pelicula/{id}`

## 3.5 Catalogo de series

Las series se gestionan de forma similar a las peliculas. Se pueden listar, buscar, filtrar por genero y consultar detalles.

Endpoints destacados:

- `GET /api/series`
- `GET /api/series/{id}`
- `GET /api/series/search`
- `GET /api/series/by-genre`
- `GET /serie/{id}`

## 3.6 Catalogo de libros

El proyecto tambien incluye una seccion de libros con integracion con Google Books. Permite listar libros, buscar por texto, filtrar por categoria y acceder al detalle.

Endpoints destacados:

- `GET /api/books`
- `GET /api/books/{id}`
- `GET /api/books/search`
- `GET /libros`
- `GET /libro/{id}`

## 3.7 Resenas, puntuaciones y likes

Los usuarios autenticados pueden crear resenas sobre peliculas, series o libros. Las resenas permiten guardar texto, puntuacion, fecha de creacion y usuario autor. Otros usuarios pueden dar like a las resenas.

Endpoints relacionados:

- `POST /api/reviews`
- `POST /api/reviews/{id}/like`
- `POST /api/reviews/series/{seriesId}`
- `POST /api/reviews/books/{bookId}`
- `GET /api/reviews/movie/{movieId}`

## 3.8 Funcionalidades sociales

AfterFilm incluye un modulo social para fomentar la interaccion entre usuarios:

- Busqueda de usuarios.
- Perfil publico.
- Seguidores y seguidos.
- Feed social.
- Chat privado.
- Conversaciones destacadas.
- Mensajes no leidos.

Endpoints destacados:

- `GET /api/social/users/search`
- `GET /api/social/users/{username}`
- `POST /api/social/users/{username}/follow`
- `POST /api/social/users/{username}/unfollow`
- `GET /api/chat/conversations`
- `GET /api/chat/messages/{username}`
- `POST /api/chat/messages/{username}`

## 3.9 Panel de administracion

El panel de administracion centraliza las tareas de gestion. Esta protegido por roles y permite operar sobre usuarios, peliculas, series, libros, moderacion e integraciones.

Funciones principales:

- Ver usuarios registrados.
- Buscar usuarios por email o nombre.
- Confirmar email.
- Banear o desbanear.
- Eliminar usuarios.
- Importar peliculas por identificador TMDB.
- Ejecutar carga masiva.
- Importar series.
- Importar libros.
- Reparar posters.
- Revisar resenas.
- Aprobar o rechazar moderaciones.
- Consultar estado del sistema.

Rutas web principales:

- `/admin`
- `/admin/users`
- `/admin/movies`
- `/admin/series`
- `/admin/books`
- `/admin/moderation`
- `/admin/bulk-loader`
- `/admin/email-config`

El modulo `/admin/books` incluye acciones de mantenimiento para reparar portadas importadas desde Google Books cuando alguna referencia queda invalida.

## 3.10 Carga de contenido desde APIs externas

Una de las funciones mas importantes del proyecto es la importacion de contenido desde servicios externos.

TMDB se utiliza para:

- Cargar una pelicula concreta.
- Cargar peliculas populares.
- Cargar peliculas mejor valoradas.
- Cargar series populares.
- Cargar series mejor valoradas.
- Descargar posters.
- Descargar imagenes de actores y directores.

Google Books se utiliza para:

- Buscar libros.
- Importar libros concretos.
- Crear catalogo inicial.

## 3.11 Gestion de imagenes

Las imagenes importadas desde APIs externas se almacenan localmente en el directorio `data/images`. Esto evita depender siempre de la URL externa y mejora la estabilidad de la aplicacion.

Tipos de imagenes:

- Posters de peliculas.
- Posters de series.
- Perfiles de actores.
- Perfiles de directores.

El sistema define rutas de almacenamiento y rutas publicas para servir las imagenes desde el navegador.

## 3.12 Descarga y streaming de peliculas

El proyecto incluye un controlador para gestionar archivos de pelicula almacenados en `data/movies`. Soporta:

- Listado de archivos disponibles.
- Descarga directa.
- Streaming progresivo.
- Cabeceras HTTP Range.
- Validacion anti path traversal.
- Formatos como MP4, MKV, AVI, MOV, WEBM y FLV.

Endpoints:

- `GET /api/movies/{id}/files`
- `GET /movies/download/{movieId}/{fileName}`
- `GET /movies/stream/{movieId}/{fileName}`

## 3.13 Moderacion de contenido

El sistema de moderacion analiza el contenido de las resenas para detectar mensajes inapropiados. La moderacion se apoya en:

- `ModerationService`
- `ModeratingAI`
- `OllamaClient`
- `ContentAnalyzer`
- `CommentModeration`

El administrador puede revisar contenido pendiente, aprobarlo o rechazarlo. Esto permite combinar automatizacion con control humano.

## 3.14 Monitorizacion del sistema

AfterFilm incluye endpoints para comprobar el estado de:

- Base de datos.
- TMDB.
- Ollama.
- Email.
- Servidor.

Endpoints:

- `GET /api/health`
- `GET /api/system-health`
- `GET /api/system/health`
- `GET /api/system/health/{service}`
- `GET /api/admin/system/health`

## 3.15 Base de datos

La base de datos sigue un modelo relacional. Las tablas principales son:

- `usuario`
- `movie`
- `tv_shows`
- `books`
- `review`
- `role`
- `actor`
- `director`
- `category`
- `review_like`
- `following`
- `messages`
- `comment_moderation`
- `user_content_list`
- `archivement`

Relaciones destacadas:

- Un usuario puede escribir muchas resenas.
- Una pelicula puede recibir muchas resenas.
- Un usuario puede seguir a otros usuarios.
- Una pelicula puede tener muchos actores.
- Una pelicula puede tener varios directores.
- Una resena puede recibir muchos likes.
- Un usuario puede tener varios roles.

## 3.16 Pruebas

El proyecto incluye pruebas automatizadas con JUnit y Spring Boot Test. La prueba principal verifica que el contexto de Spring arranca correctamente.

Para que las pruebas sean reproducibles, se ha configurado un perfil `test` con:

- Base de datos H2 en memoria.
- Email desactivado.
- Cargas automaticas desactivadas.
- Moderacion externa desactivada.
- Rutas de almacenamiento dentro de `target/test-data`.

Comando de ejecucion:

```bash
./mvnw.cmd test
```

Resultado verificado:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

# 4. Descripcion grafica de la aplicacion

Esta seccion describe las pantallas principales de AfterFilm. En la version final de la memoria se recomienda insertar capturas reales de cada pantalla.

## 4.1 Pantalla de inicio

La pantalla de inicio muestra el acceso al catalogo principal y sirve como punto de entrada a la aplicacion. Desde ella el usuario puede navegar a peliculas, series, libros, login, registro y otras secciones disponibles.

**Captura recomendada:** pagina `/`.

## 4.2 Catalogo de peliculas

El catalogo de peliculas presenta una lista paginada con posters, titulos y datos principales. Permite consultar contenido de forma visual y acceder al detalle de cada pelicula.

**Captura recomendada:** listado de peliculas o pantalla principal con peliculas.

## 4.3 Detalle de pelicula

La ficha de pelicula muestra informacion completa:

- Poster.
- Titulo.
- Sinopsis.
- Fecha de estreno.
- Reparto.
- Directores.
- Categorias.
- Resenas.
- Puntuacion.

**Captura recomendada:** pagina `/pelicula/{id}`.

## 4.4 Catalogo y detalle de series

La seccion de series permite explorar contenido televisivo importado desde TMDB. La estructura es similar a peliculas, pero adaptada a series.

**Capturas recomendadas:** `/series` y `/serie/{id}`.

## 4.5 Catalogo y detalle de libros

La seccion de libros permite consultar obras importadas desde Google Books. Cada libro incluye metadatos, imagen de portada y descripcion.

**Capturas recomendadas:** `/libros` y `/libro/{id}`.

## 4.6 Registro e inicio de sesion

Las pantallas de autenticacion permiten crear una cuenta y acceder al sistema. El registro incluye validacion de datos y el sistema puede confirmar cuentas por email.

**Capturas recomendadas:** `/register` y `/login`.

## 4.7 Perfil de usuario

El perfil muestra la informacion del usuario, sus resenas y actividad. Forma parte del modulo social de la aplicacion.

**Captura recomendada:** `/perfil`.

## 4.8 Busqueda de usuarios y perfil publico

El sistema social permite buscar otros usuarios, consultar sus perfiles y seguirlos.

**Capturas recomendadas:** `/usuarios` y `/usuario/{username}`.

## 4.9 Feed social

El feed permite visualizar actividad de usuarios seguidos o contenido social relevante.

**Captura recomendada:** `/feed`.

## 4.10 Chat

El chat permite mantener conversaciones privadas entre usuarios, consultar mensajes y gestionar conversaciones.

**Captura recomendada:** `/chat`.

## 4.11 Panel de administracion

El panel de administracion muestra accesos a las herramientas internas:

- Usuarios.
- Peliculas.
- Series.
- Libros.
- Moderacion.
- Carga masiva.
- Email.
- Estado del sistema.

**Captura recomendada:** `/admin`.

## 4.12 Gestion de usuarios

La pantalla de usuarios permite al administrador consultar, buscar y modificar el estado de usuarios.

**Captura recomendada:** `/admin/users`.

## 4.13 Carga masiva

La pantalla de carga masiva permite importar contenido desde TMDB con presets y consultar el progreso.

**Captura recomendada:** `/admin/bulk-loader`.

## 4.14 Moderacion

La pantalla de moderacion permite revisar contenido detectado por el sistema automatico y tomar decisiones manuales.

**Captura recomendada:** `/admin/moderation`.

## 4.15 Estado del sistema

La aplicacion incluye pantallas y endpoints para comprobar conexiones con base de datos, TMDB, Ollama, email y servidor.

**Captura recomendada:** panel de salud en administracion.

---

# 5. Inicio de la App

## 5.1 Requisitos previos

Para ejecutar el proyecto se necesitan:

- Java 17 o superior.
- Maven o el wrapper incluido (`mvnw.cmd` en Windows).
- MySQL 8.0 o superior.
- Conexion a Internet para importar datos desde TMDB y Google Books.
- Token de TMDB.
- Ollama opcional para moderacion con IA.
- Cuenta SMTP opcional para envio de emails.

## 5.2 Configuracion de base de datos

Crear una base de datos MySQL:

```sql
CREATE DATABASE AfterFilm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Configurar credenciales en variables de entorno o en `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/AfterFilm
spring.datasource.username=root
spring.datasource.password=tu_password
```

## 5.3 Configuracion de TMDB

Configurar token o API key:

```properties
app.tmdb.base-url=https://api.themoviedb.org/3
app.tmdb.api-key=TU_API_KEY
app.tmdb.bearer-token=TU_BEARER_TOKEN
```

## 5.4 Configuracion de email

El email se puede activar o desactivar:

```properties
app.email.enabled=true
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu_correo@gmail.com
spring.mail.password=tu_password_de_aplicacion
```

Para desarrollo o pruebas:

```properties
app.email.enabled=false
```

## 5.5 Configuracion de Ollama

Para usar moderacion con IA local:

```properties
app.moderation.ollama.url=http://localhost:11434
app.moderation.ollama.model=llama3
app.moderation.ollama.enabled=true
```

## 5.6 Ejecucion

Desde la raiz del proyecto:

```bash
./mvnw.cmd spring-boot:run
```

La aplicacion queda disponible normalmente en:

```text
http://localhost:8082
```

## 5.7 Usuario administrador inicial

El sistema crea o actualiza un usuario administrador inicial. En la configuracion del proyecto aparece:

```properties
app.admin.initial-password=admin123
```

Datos habituales para acceso local:

```text
Usuario: admin
Password: admin123
```

## 5.8 Ejecucion de pruebas

```bash
./mvnw.cmd test
```

El perfil de pruebas usa H2 y no requiere MySQL, email ni servicios externos.

---

# 6. Presupuesto

El presupuesto se divide en recursos humanos, hardware, software y servicios externos. Al tratarse de un proyecto academico, muchas herramientas utilizadas son gratuitas o de codigo abierto, pero se estima el coste profesional equivalente.

## 6.1 Recursos humanos

| Tarea | Horas estimadas | Coste/hora | Total |
|---|---:|---:|---:|
| Analisis de requisitos | 20 h | 20 EUR | 400 EUR |
| Diseno de arquitectura | 25 h | 20 EUR | 500 EUR |
| Diseno de base de datos | 20 h | 20 EUR | 400 EUR |
| Desarrollo backend | 90 h | 20 EUR | 1.800 EUR |
| Desarrollo frontend con Thymeleaf | 55 h | 20 EUR | 1.100 EUR |
| Integracion con APIs externas | 35 h | 20 EUR | 700 EUR |
| Seguridad y autenticacion | 25 h | 20 EUR | 500 EUR |
| Panel de administracion | 40 h | 20 EUR | 800 EUR |
| Moderacion con IA | 20 h | 20 EUR | 400 EUR |
| Pruebas y depuracion | 30 h | 20 EUR | 600 EUR |
| Documentacion y memoria | 25 h | 20 EUR | 500 EUR |
| **Total** | **385 h** |  | **7.700 EUR** |

## 6.2 Hardware

| Recurso | Coste estimado |
|---|---:|
| Equipo de desarrollo | 900 EUR |
| Perifericos y monitor | 200 EUR |
| Consumo electrico estimado | 30 EUR |
| **Total hardware** | **1.130 EUR** |

## 6.3 Software

| Herramienta | Licencia | Coste |
|---|---|---:|
| Java 17 | Gratuita | 0 EUR |
| Spring Boot | Open Source | 0 EUR |
| Maven | Open Source | 0 EUR |
| MySQL Community | Gratuita | 0 EUR |
| IntelliJ IDEA Community / IDE equivalente | Gratuita | 0 EUR |
| Git | Open Source | 0 EUR |
| Ollama | Gratuita | 0 EUR |
| H2 Database | Open Source | 0 EUR |
| **Total software** |  | **0 EUR** |

## 6.4 Servicios externos

| Servicio | Uso | Coste |
|---|---|---:|
| TMDB API | Datos de peliculas y series | 0 EUR |
| Google Books API | Datos de libros | 0 EUR |
| Gmail SMTP | Envio de emails de desarrollo | 0 EUR |
| Hosting local | Desarrollo y pruebas | 0 EUR |
| **Total servicios** |  | **0 EUR** |

## 6.5 Coste total estimado

| Concepto | Total |
|---|---:|
| Recursos humanos | 7.700 EUR |
| Hardware | 1.130 EUR |
| Software | 0 EUR |
| Servicios externos | 0 EUR |
| **Total estimado** | **8.830 EUR** |

---

# 7. Glosario

**API:** Interfaz que permite que dos aplicaciones se comuniquen.  
**Backend:** Parte del sistema que se ejecuta en el servidor.  
**Base de datos relacional:** Sistema que organiza informacion en tablas relacionadas.  
**DTO:** Objeto utilizado para transferir datos entre capas o hacia el cliente.  
**Endpoint:** URL concreta de una API que realiza una operacion.  
**Frontend:** Parte visible de la aplicacion con la que interactua el usuario.  
**Hibernate:** Implementacion ORM que mapea objetos Java a tablas SQL.  
**IA:** Inteligencia artificial. En este proyecto se aplica a moderacion de contenido.  
**JPA:** Especificacion de Java para persistencia de datos.  
**JWT:** Token firmado usado para autenticacion.  
**MVC:** Patron Modelo-Vista-Controlador.  
**ORM:** Tecnica que relaciona objetos de programacion con tablas de base de datos.  
**REST:** Estilo de arquitectura para crear APIs HTTP.  
**Spring Boot:** Framework Java para crear aplicaciones web y servicios backend.  
**Thymeleaf:** Motor de plantillas para generar HTML desde Spring.  
**TMDB:** The Movie Database, API de informacion audiovisual.  
**Token:** Cadena de texto usada para representar autenticacion o autorizacion.  
**Usuario administrador:** Usuario con permisos para gestionar el sistema.  

---

# 8. Referencias

- Documentacion oficial de Spring Boot: https://spring.io/projects/spring-boot
- Documentacion oficial de Spring Security: https://spring.io/projects/spring-security
- Documentacion oficial de Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Documentacion oficial de Thymeleaf: https://www.thymeleaf.org/documentation.html
- Documentacion oficial de Maven: https://maven.apache.org/guides/
- Documentacion oficial de MySQL: https://dev.mysql.com/doc/
- Documentacion de TMDB API: https://developer.themoviedb.org/docs
- Documentacion de Google Books API: https://developers.google.com/books
- Documentacion de Ollama: https://ollama.com
- Documentacion de Caffeine Cache: https://github.com/ben-manes/caffeine
- Documentacion de JJWT: https://github.com/jwtk/jjwt
- Repositorio y documentacion interna del proyecto AfterFilm: carpeta `docs/`

---

# 9. Anexos

## 9.1 Estructura del proyecto

```text
AfterFilm/
  src/
    main/
      java/alicanteweb/pelisapp/
        config/
        constants/
        controller/
        controller/web/
        dto/
        entity/
        exception/
        repository/
        security/
        service/
        tmdb/
        util/
      resources/
        static/
        templates/
        sql/
    test/
      java/
      resources/
  data/
    images/
    movies/
  docs/
  scripts/
  pom.xml
  README.md
```

## 9.2 Endpoints principales

### Autenticacion

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
GET  /api/auth/confirm-email
```

### Peliculas

```text
GET /api/movies
GET /api/movies/{id}/details
GET /api/movies/search
GET /api/movies/by-category
GET /pelicula/{id}
```

### Series

```text
GET /api/series
GET /api/series/{id}
GET /api/series/search
GET /api/series/by-genre
GET /serie/{id}
```

### Libros

```text
GET /api/books
GET /api/books/{id}
GET /api/books/search
GET /libros
GET /libro/{id}
```

### Resenas

```text
POST /api/reviews
POST /api/reviews/{id}/like
GET  /api/reviews/movie/{movieId}
POST /api/reviews/series/{seriesId}
POST /api/reviews/books/{bookId}
```

### Social y chat

```text
GET  /api/social/users/search
GET  /api/social/users/{username}
POST /api/social/users/{username}/follow
POST /api/social/users/{username}/unfollow
GET  /api/chat/conversations
GET  /api/chat/messages/{username}
POST /api/chat/messages/{username}
```

### Administracion

```text
GET  /admin
GET  /admin/users
GET  /admin/movies
GET  /admin/series
GET  /admin/books
GET  /admin/moderation
GET  /admin/bulk-loader
POST /api/admin/tmdb/load-movie/{tmdbId}
POST /api/admin/tmdb/bulk-load
GET  /api/admin/system/health
```

## 9.3 Dependencias principales

```text
Spring Boot Starter Web
Spring Boot Starter Security
Spring Boot Starter Data JPA
Spring Boot Starter Thymeleaf
Spring Boot Starter Mail
Spring Boot Starter Validation
Spring Boot Starter Cache
Spring Boot Starter Test
MySQL Connector/J
H2 Database
JJWT
Caffeine
Lombok
Jackson Datatype JSR310
```

## 9.4 Archivos de configuracion importantes

```text
pom.xml
src/main/resources/application.properties
src/test/resources/application-test.properties
src/main/java/alicanteweb/pelisapp/config/SecurityConfig.java
src/main/java/alicanteweb/pelisapp/config/WebConfig.java
src/main/java/alicanteweb/pelisapp/config/EmailConfig.java
```

## 9.5 Pruebas realizadas

Prueba de contexto de Spring:

```text
src/test/java/alicanteweb/pelisapp/AfterFilmApplicationTests.java
```

Comando:

```bash
./mvnw.cmd test
```

Resultado:

```text
BUILD SUCCESS
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

## 9.6 Posibles mejoras futuras

- Crear una aplicacion movil nativa que consuma la API REST.
- Ampliar la cobertura de tests unitarios e integracion.
- Incorporar paginacion y filtros avanzados en todas las secciones.
- Mejorar el sistema de recomendaciones.
- Anadir despliegue con Docker.
- Incorporar CI/CD con GitHub Actions.
- Mejorar accesibilidad y experiencia responsive.
- Anadir recuperacion de contrasena completa.
- Crear estadisticas avanzadas para administradores.
- Anadir notificaciones en tiempo real para chat y actividad social.

