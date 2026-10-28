package com.example.rederosaback.repository;

import com.example.rederosaback.model.MensagemComunidade;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MensagemComunidadeRepository extends JpaRepository<MensagemComunidade, Long> {
    List<MensagemComunidade> findBySalaIdOrderByIdDesc(String sala, Pageable pagina);
    List<MensagemComunidade> findBySalaIdAndIdGreaterThanOrderByIdAsc(String sala, Long apos, Pageable pagina);
    List<MensagemComunidade> findBySalaIdAndIdLessThanOrderByIdDesc(String sala, Long antes, Pageable pagina);
    Optional<MensagemComunidade> findByAutoraIdAndChaveEnvio(Long autoraId, String chaveEnvio);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MensagemComunidade m where m.id = :id and m.sala.id = :sala")
    Optional<MensagemComunidade> buscarParaDenuncia(@Param("id") Long id, @Param("sala") String sala);
}
