package com.example.rederosaback.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = @Index(name = "idx_acompanhamento_usuario", columnList = "usuario_id,estado"))
public class Acompanhamento {
    public enum Estado { EM_ANDAMENTO, CONFIRMADO, CANCELADO }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
    @Column(nullable = false)
    private String nome;
    @Column(length = 16)
    private String telefone;
    @Column(nullable = false, unique = true, length = 43)
    private String tokenPublico;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Estado estado;
    @Column(nullable = false)
    private Instant inicio;
    @Column(nullable = false)
    private Instant prazo;
    private Instant encerradoEm;
    private Double latitude;
    private Double longitude;
    private Double precisao;
    private Instant localizacaoColetadaEm;
    private Instant localizacaoRecebidaEm;
    private Instant localizacaoPausadaEm;
    private boolean compartilhandoLocalizacao;
    @Version
    private Long versao;

    protected Acompanhamento() { }
    public Acompanhamento(Long usuarioId, String nome, String telefone, String tokenPublico, Instant inicio, Instant prazo) {
        this.usuarioId = usuarioId; this.nome = nome; this.telefone = telefone;
        this.tokenPublico = tokenPublico; this.inicio = inicio; this.prazo = prazo;
        this.estado = Estado.EM_ANDAMENTO;
    }
    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getTokenPublico() { return tokenPublico; }
    public Estado getEstado() { return estado; }
    public Instant getInicio() { return inicio; }
    public Instant getPrazo() { return prazo; }
    public Instant getEncerradoEm() { return encerradoEm; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getPrecisao() { return precisao; }
    public Instant getLocalizacaoColetadaEm() { return localizacaoColetadaEm; }
    public Instant getLocalizacaoRecebidaEm() { return localizacaoRecebidaEm; }
    public Instant getLocalizacaoPausadaEm() { return localizacaoPausadaEm; }
    public boolean isCompartilhandoLocalizacao() { return compartilhandoLocalizacao; }
    public void atualizarLocalizacao(Double latitude, Double longitude, Double precisao, Instant coletadaEm, Instant recebidaEm) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.precisao = precisao;
        this.localizacaoColetadaEm = coletadaEm;
        this.localizacaoRecebidaEm = recebidaEm;
        this.compartilhandoLocalizacao = true;
    }
    public void pausarLocalizacao(Instant agora) {
        this.compartilhandoLocalizacao = false;
        this.localizacaoPausadaEm = agora;
    }
    public void estenderAte(Instant prazo) { this.prazo = prazo; }
    public void encerrar(Estado estado, Instant agora) {
        this.estado = estado;
        this.encerradoEm = agora;
        this.compartilhandoLocalizacao = false;
    }
}
