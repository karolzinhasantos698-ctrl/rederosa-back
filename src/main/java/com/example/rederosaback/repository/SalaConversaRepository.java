package com.example.rederosaback.repository;

import com.example.rederosaback.model.SalaConversa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SalaConversaRepository extends JpaRepository<SalaConversa, String> {
    List<SalaConversa> findAllByOrderByOrdemAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SalaConversa s where s.id = :id")
    Optional<SalaConversa> buscarParaEnviar(@Param("id") String id);
}
