package com.example.rederosaback.repository;
import com.example.rederosaback.model.SessaoAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface SessaoAcessoRepository extends JpaRepository<SessaoAcesso, Long> {
    Optional<SessaoAcesso> findByTokenHashAndExpiraEmAfter(String tokenHash, Instant agora);
}
