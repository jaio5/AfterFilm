# AfterFilm — guía para trabajar aquí

Aplicación web para gestionar y descubrir películas, series y libros: catálogo,
reseñas, perfiles sociales, chat, panel de administración e integraciones con
TMDB, Google Books y Ollama. Es un **Trabajo de Fin de Grado**, con su memoria en
`docs/MEMORIA_TFG_AfterFilm.md`.

Spring Boot 3.2.10 sobre Java 17, Maven, Spring Security + JWT, JPA/Hibernate
sobre PostgreSQL, Thymeleaf para las vistas y Caffeine para caché.

**Este fichero es el mapa, no la documentación.** El proyecto tiene casi 8.000
líneas en `docs/`; aquí solo está lo que hace falta para no perderse en ellas.

## Antes de tocar nada: dos trampas comprobadas

**1. El README manda ejecutar `./mvnw.cmd`, y eso es el wrapper de Windows.** En
WSL o Linux falla. El bueno es `./mvnw`, que ya tiene permiso de ejecución.

**2. No hay Java instalado en WSL.** El único JDK del equipo está en Windows
(`/mnt/c/Program Files/Java/jdk-17`) y llega al PATH por el interop, así que
`java` desde WSL o no aparece o es el binario de Windows. Antes de compilar
aquí hay que instalarlo:

```bash
sudo apt install -y openjdk-17-jdk
```

O trabajar el proyecto desde Windows, que es de donde salen los comandos del
README.

## Comandos

```bash
./mvnw spring-boot:run   # arranca; puerto en src/main/resources/application.properties
./mvnw test              # tests, con perfil H2 en memoria
./mvnw clean package     # empaqueta
docker compose up --build
```

Hace falta PostgreSQL 16+ (o Docker) con una base `pelisapp` creada, y un token
de TMDB si se van a importar películas o series.

## Estructura

Arquitectura en capas clásica de Spring, bajo `src/main/java/alicanteweb/pelisapp/`:

| Busco... | Está en |
| --- | --- |
| APIs REST y controladores de administración | `controller/` |
| Controladores de vistas HTML | `controller/web/` |
| Lógica de negocio | `service/` |
| Entidades JPA | `entity/` |
| Acceso a datos | `repository/` |
| Objetos de transferencia | `dto/` |
| Seguridad, sesiones y JWT | `security/` |
| Cliente e integración con TMDB | `tmdb/` |
| Configuración de Spring | `config/` |
| Vistas Thymeleaf | `src/main/resources/templates/` |
| CSS, JS e imágenes | `src/main/resources/static/` |
| Scripts SQL | `src/main/resources/sql/` |
| Variables y perfiles | `application.properties`, `application-prod.properties` |

## La documentación, y qué contesta cada fichero

Hay 18 documentos en `docs/` y **no todos valen lo mismo**. Estos son los que se
consultan:

| Fichero | Contesta |
| --- | --- |
| `docs/INDEX.md` | El índice oficial, con más detalle que esta tabla |
| `docs/ARCHITECTURE.md` | Patrones, capas y organización de paquetes |
| `docs/DATABASE.md` | Modelo de datos y esquemas |
| `docs/DEVELOPER.md` | Estándares, patrones y prácticas al escribir código |
| `docs/CONFIGURATION.md` | Variables de entorno y perfiles |
| `docs/INSTALLATION.md` | Puesta en marcha paso a paso |
| `docs/DEPLOYMENT.md` | Despliegue y producción |
| `docs/API_REST_ACTUALIZADA.md` | Endpoints y ejemplos |
| `docs/TESTING.md` | Estrategia de pruebas |
| `docs/MEMORIA_TFG_AfterFilm.md` | La memoria del TFG |

El resto —`FINAL_IMPLEMENTATION_SUMMARY.md`, `REFACTORING_SUMMARY.md`,
`MOVIE_DOWNLOAD_SOLUTION.md`, `MOVIE_MANAGEMENT_SOLUTION.md`,
`MODERATION_SYSTEM_IMPROVED.md`, `SYSTEM_HEALTH_IMPLEMENTATION.md`— son
resúmenes de trabajos concretos ya hechos. Sirven de historia, no de referencia:
**no des por vigente lo que digan sin comprobarlo contra el código.**

## Estado real de las pruebas

Hay **un único fichero de test**, `AfterFilmApplicationTests.java`, que es el
`contextLoads` que genera Spring Initializr. `docs/TESTING.md` describe una
estrategia que el código no tiene todavía.

Consecuencia práctica: **aquí `./mvnw test` en verde no significa que algo
funcione.** Cualquier cambio de comportamiento hay que comprobarlo arrancando la
aplicación, y si se añaden tests, el perfil de H2 en memoria ya está preparado
para no depender de PostgreSQL ni de servicios externos.

## Cómo se escribe aquí

Español en documentación y commits. Java estándar de Spring: inyección por
constructor, Lombok para los accesores, DTO en las fronteras y la lógica en
`service/`, nunca en el controlador.

## Lo que este proyecto no hace

- No hay migraciones automáticas tipo Flyway o Liquibase: el esquema sale de JPA
  y de los scripts de `src/main/resources/sql/`.
- La moderación con Ollama **necesita Ollama corriendo**; sin él cae a un
  fallback local, así que un comportamiento distinto entre entornos puede ser
  eso y no un fallo.
