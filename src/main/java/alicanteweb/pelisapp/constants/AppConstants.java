package alicanteweb.pelisapp.constants;

/**
 * Constantes para la aplicación PelisApp.
 * Centraliza valores mágicos y mejora la mantenibilidad del código.
 */
public final class AppConstants {

    private AppConstants() {
    }

    // Constantes de paginación
    public static final int DEFAULT_PAGE_SIZE = 15;
    public static final int MAX_PAGE_SIZE = 48;
    public static final int MIN_PAGE_SIZE = 1;
    public static final int DEFAULT_PAGE_NUMBER = 0;

    // Constantes de roles
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUPERADMIN = "ROLE_SUPERADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_MODERATOR = "ROLE_MODERATOR";
    public static final String ROLE_CRITIC = "ROLE_CRITIC";
    public static final String ROLE_TOP_CRITIC = "ROLE_TOP_CRITIC";

    // Constantes de logros/achievements
    public static final String ACHIEVEMENT_FIRST_REVIEW = "FIRST_REVIEW";
    public static final String ACHIEVEMENT_REVIEWER_10 = "REVIEWER_10";
    public static final String ACHIEVEMENT_REVIEWER_50 = "REVIEWER_50";
    public static final String ACHIEVEMENT_CRITIC_100 = "CRITIC_100";
    public static final String ACHIEVEMENT_FIRST_LIKE = "FIRST_LIKE";
    public static final String ACHIEVEMENT_POPULAR_25 = "POPULAR_25";
    public static final String ACHIEVEMENT_INFLUENCER_100 = "INFLUENCER_100";
    public static final String ACHIEVEMENT_VIRAL_REVIEW = "VIRAL_REVIEW";
    public static final String ACHIEVEMENT_POPULAR_10_FOLLOWERS = "POPULAR_10_FOLLOWERS";
    public static final String ACHIEVEMENT_CELEBRITY_50_FOLLOWERS = "CELEBRITY_50_FOLLOWERS";

    // Constantes de reviews y ratings
    public static final int MIN_STARS_RATING = 1;
    public static final int MAX_STARS_RATING = 5;
    public static final int VIRAL_REVIEW_LIKES_THRESHOLD = 20;
    public static final double CRITIC_ROLE_AVG_LIKES_THRESHOLD = 3.0;
    public static final double TOP_CRITIC_ROLE_AVG_LIKES_THRESHOLD = 5.0;

    // Constantes de moderación
    public static final double DEFAULT_TOXICITY_THRESHOLD = 0.7;
    public static final double DEFAULT_REVIEW_THRESHOLD = 0.5;
    public static final double AI_REJECTION_THRESHOLD = 0.2;

    // Constantes de URLs y rutas
    public static final String LOGIN_REDIRECT_PATH = "redirect:/login";
    public static final String PROFILE_TEMPLATE_PATH = "usuario/profile";
    public static final String INDEX_TEMPLATE_PATH = "index";
    public static final String ERROR_TEMPLATE_PATH = "error";

    // Constantes de archivos e imágenes
    public static final String DEFAULT_IMAGES_PATH = "./data/images";
    public static final String POSTERS_SUBFOLDER = "posters";
    public static final String BACKDROPS_SUBFOLDER = "backdrops";
    public static final String PROFILES_SUBFOLDER = "profiles";
    public static final String DEFAULT_SERVE_BASE = "/images";

    // Constantes de TMDB
    public static final String TMDB_DEFAULT_BASE_URL = "https://api.themoviedb.org/3";
    public static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p";
    public static final String TMDB_POSTER_SIZE_W500 = "w500";
    public static final String TMDB_BACKDROP_SIZE_W780 = "w780";

    // Constantes de caché
    public static final String CACHE_MOVIES_PAGE_BY_GENRE = "moviesPageByGenre";
    public static final String CACHE_ALL_CATEGORIES = "allCategories";
    public static final String CACHE_MOVIE_DETAILS = "movieDetails";

    // Constantes de límites de texto
    public static final int MAX_REVIEW_TEXT_LENGTH = 2000;
    public static final int MIN_REVIEW_TEXT_LENGTH = 10;
    public static final int MAX_USERNAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 200;
    public static final int MAX_MOVIE_TITLE_LENGTH = 255;

    // Mensajes de error comunes
    public static final String ERROR_USER_NOT_FOUND = "Usuario no encontrado";
    public static final String ERROR_MOVIE_NOT_FOUND = "Película no encontrada";
    public static final String ERROR_REVIEW_NOT_FOUND = "Reseña no encontrada";
    public static final String ERROR_INVALID_RATING = "La puntuación debe estar entre 1 y 5";
    public static final String ERROR_REVIEW_TOO_TOXIC = "Reseña rechazada por moderación";

    // Constantes adicionales para TMDB API
    public static final String TMDB_POPULAR_ENDPOINT = "/movie/popular";
    public static final String TMDB_TOP_RATED_ENDPOINT = "/movie/top_rated";
    public static final String TMDB_LANGUAGE_ES = "es-ES";
    public static final String TMDB_AUTHORIZATION_HEADER = "Authorization";
    public static final String TMDB_BEARER_PREFIX = "Bearer ";
    public static final String TMDB_APPEND_CREDITS = "credits";

    // Response keys de TMDB
    public static final String TMDB_RESULTS_KEY = "results";
    public static final String TMDB_ID_KEY = "id";
    public static final String TMDB_TITLE_KEY = "title";
    public static final String TMDB_OVERVIEW_KEY = "overview";
    public static final String TMDB_POSTER_PATH_KEY = "poster_path";
    public static final String TMDB_RELEASE_DATE_KEY = "release_date";
    public static final String TMDB_RUNTIME_KEY = "runtime";
    public static final String TMDB_GENRES_KEY = "genres";
    public static final String TMDB_NAME_KEY = "name";
    public static final String TMDB_CREDITS_KEY = "credits";
    public static final String TMDB_CAST_KEY = "cast";
    public static final String TMDB_CREW_KEY = "crew";
    public static final String TMDB_JOB_KEY = "job";
    public static final String TMDB_PROFILE_PATH_KEY = "profile_path";
    public static final String TMDB_DIRECTOR_JOB = "Director";

    // Constantes de flujo de carga
    public static final int MAX_ACTORS_PER_MOVIE = 10;
    public static final long MINIMUM_MOVIES_FOR_STARTUP = 10L;
    public static final int DEFAULT_PAGES_TO_LOAD = 5;
    public static final long MINIMUM_SERIES_FOR_STARTUP = 10L;
    public static final int DEFAULT_SERIES_PAGES_TO_LOAD = 3;
    public static final int DELAY_BETWEEN_REQUESTS_MS = 250;
    public static final int DELAY_BETWEEN_PAGES_MS = 300;
    public static final int MAX_PAGES_LIMIT = 500;
    public static final int REQUEST_TIMEOUT_SECONDS = 10;

    // Prefijos para nombres de archivos
    public static final String MOVIE_FILE_PREFIX = "movie_";
    public static final String ACTOR_FILE_PREFIX = "actor_";
    public static final String DIRECTOR_FILE_PREFIX = "director_";


    // Marcadores de logging compatibles con cualquier consola.
    public static final String LOG_SEPARATOR = "------------------------------------------------------------";
    public static final String LOG_SUCCESS_EMOJI = "[OK]";
    public static final String LOG_ERROR_EMOJI = "[ERROR]";
    public static final String LOG_WARNING_EMOJI = "[WARN]";
    public static final String LOG_INFO_EMOJI = "[INFO]";
    public static final String LOG_FIRE_EMOJI = "[START]";
}
