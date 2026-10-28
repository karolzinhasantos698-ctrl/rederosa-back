package com.example.rederosaback.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = @Index(name = "idx_mensagem_sala_id", columnList = "sala_id,id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_mensagem_autora_envio", columnNames = {"autora_id", "chave_envio"}))
public class MensagemComunidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sala_id", nullable = false)
    private SalaConversa sala;
    @Column(name = "autora_id", nullable = false)
    private Long autoraId;
    @Column(nullable = false, length = 255)
    private String autoraNome;
    @Column(nullable = false, length = 2000)
    private String texto;
    @Column(name = "chave_envio", nullable = false, length = 36)
    private String chaveEnvio;
    @Column(nullable = false)
    private Instant criadaEm;

    protected MensagemComunidade() { }
    public MensagemComunidade(SalaConversa sala, Long autoraId, String autoraNome, String texto, String chaveEnvio, Instant criadaEm) {
        this.sala = sala; this.autoraId = autoraId; this.autoraNome = autoraNome;
        this.texto = texto; this.chaveEnvio = chaveEnvio; this.criadaEm = criadaEm;
    }
    public Long getId() { return id; }
    public SalaConversa getSala() { return sala; }
    public Long getAutoraId() { return autoraId; }
    public String getAutoraNome() { return autoraNome; }
    public String getTexto() { return texto; }
    public String getChaveEnvio() { return chaveEnvio; }
    public Instant getCriadaEm() { return criadaEm; }
}
