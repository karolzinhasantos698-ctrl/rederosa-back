package com.example.rederosaback.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_denuncia_mensagem_autora", columnNames = {"mensagem_id", "denunciante_id"}))
public class DenunciaComunidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "mensagem_id", nullable = false)
    private MensagemComunidade mensagem;
    @Column(name = "denunciante_id", nullable = false)
    private Long denuncianteId;
    @Column(nullable = false, length = 500)
    private String motivo;
    @Column(nullable = false)
    private Instant criadaEm;

    protected DenunciaComunidade() { }
    public DenunciaComunidade(MensagemComunidade mensagem, Long denuncianteId, String motivo, Instant criadaEm) {
        this.mensagem = mensagem; this.denuncianteId = denuncianteId;
        this.motivo = motivo; this.criadaEm = criadaEm;
    }
    public Long getId() { return id; }
}
