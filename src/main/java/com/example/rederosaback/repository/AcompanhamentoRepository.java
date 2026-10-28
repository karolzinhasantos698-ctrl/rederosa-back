package com.example.rederosaback.repository;
import com.example.rederosaback.model.Acompanhamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AcompanhamentoRepository extends JpaRepository<Acompanhamento, Long> {
    List<Acompanhamento> findTop20ByUsuarioIdOrderByInicioDesc(Long usuarioId);
    Optional<Acompanhamento> findFirstByUsuarioIdAndEstadoOrderByInicioDesc(Long usuarioId, Acompanhamento.Estado estado);
    Optional<Acompanhamento> findByTokenPublico(String token);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Acompanhamento a where a.id = :id and a.usuarioId = :usuarioId")
    Optional<Acompanhamento> buscarParaAlterar(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
