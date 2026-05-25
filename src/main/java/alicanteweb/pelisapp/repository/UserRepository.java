package alicanteweb.pelisapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import alicanteweb.pelisapp.entity.User;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // Métodos de búsqueda simples
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM usuario_roles WHERE usuario_id = :userId", nativeQuery = true)
    void deleteRoleLinks(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM usuario_etiquetas WHERE usuario_id = :userId", nativeQuery = true)
    void deleteTagLinks(@Param("userId") Long userId);
}
