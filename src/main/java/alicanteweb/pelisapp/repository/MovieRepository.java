package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie,Long> {
    Optional<Movie> findByTmdbId(Long tmdbId);
    Optional<Movie> findByTitle(String title);
    Page<Movie> findAll(Pageable pageable);
    Page<Movie> findByCategories_Name(String name, Pageable pageable);
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * Encuentra una película por ID cargando explícitamente actores y directores
     */
    @Query("SELECT DISTINCT m FROM Movie m " +
           "LEFT JOIN FETCH m.actors " +
           "LEFT JOIN FETCH m.directors " +
           "LEFT JOIN FETCH m.categories " +
           "WHERE m.id = :id")
    Optional<Movie> findByIdWithCastAndDirectors(@Param("id") Long id);

    @Query("SELECT m FROM Movie m JOIN m.reviews r " +
           "WHERE r.createdAt >= :startOfMonth " +
           "GROUP BY m.id " +
           "ORDER BY AVG(r.stars) DESC, COUNT(r.id) DESC")
    List<Movie> findTopRatedThisMonth(@Param("startOfMonth") Instant startOfMonth, Pageable pageable);

    @Query("SELECT m.id, COALESCE(AVG(CAST(r.stars AS double)), null), COUNT(r.id) " +
           "FROM Movie m LEFT JOIN m.reviews r " +
           "WHERE m.id IN :ids GROUP BY m.id")
    List<Object[]> findRatingStatsByIds(@Param("ids") List<Long> ids);
}
