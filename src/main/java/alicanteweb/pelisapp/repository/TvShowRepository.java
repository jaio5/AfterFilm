package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.TvShow;
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
public interface TvShowRepository extends JpaRepository<TvShow, Long> {
    Optional<TvShow> findByTmdbId(Long tmdbId);
    List<TvShow> findByTitleContainingIgnoreCase(String title);
    Page<TvShow> findAll(Pageable pageable);

    @Query("SELECT ts FROM TvShow ts JOIN ts.reviews r " +
           "WHERE r.createdAt >= :startOfMonth " +
           "GROUP BY ts.id " +
           "ORDER BY AVG(r.stars) DESC, COUNT(r.id) DESC")
    List<TvShow> findTopRatedThisMonth(@Param("startOfMonth") Instant startOfMonth, Pageable pageable);

    Page<TvShow> findByGenresContainingIgnoreCase(String genre, Pageable pageable);

    @Query("SELECT ts.genres FROM TvShow ts WHERE ts.genres IS NOT NULL AND ts.genres <> ''")
    List<String> findAllGenreStrings();

    @Query("SELECT ts.id, COALESCE(AVG(CAST(r.stars AS double)), null), COUNT(r.id) " +
           "FROM TvShow ts LEFT JOIN ts.reviews r " +
           "WHERE ts.id IN :ids GROUP BY ts.id")
    List<Object[]> findRatingStatsByIds(@Param("ids") List<Long> ids);
}
