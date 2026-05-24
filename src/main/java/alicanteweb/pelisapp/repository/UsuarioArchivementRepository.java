package alicanteweb.pelisapp.repository;

import alicanteweb.pelisapp.entity.UsuarioArchivement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UsuarioArchivementRepository extends JpaRepository<UsuarioArchivement,Long> {

    @Modifying
    @Transactional
    void deleteByUser_Id(Long userId);
}
