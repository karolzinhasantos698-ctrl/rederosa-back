package com.example.rederosaback.repository;

import com.example.rederosaback.model.DenunciaComunidade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DenunciaComunidadeRepository extends JpaRepository<DenunciaComunidade, Long> {
    boolean existsByMensagemIdAndDenuncianteId(Long mensagemId, Long denuncianteId);
}
