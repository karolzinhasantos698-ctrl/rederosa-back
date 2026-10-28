package com.example.rederosaback.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class SessaoAcesso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false)
    private Long usuarioId;
    @Column(nullable = false)
    private Instant expiraEm;

    protected SessaoAcesso() { }
    public SessaoAcesso(String tokenHash, Long usuarioId, Instant expiraEm) {
        this.tokenHash = tokenHash;
        this.usuarioId = usuarioId;
        this.expiraEm = expiraEm;
    }
    public Long getUsuarioId() { return usuarioId; }
    public Instant getExpiraEm() { return expiraEm; }
}
