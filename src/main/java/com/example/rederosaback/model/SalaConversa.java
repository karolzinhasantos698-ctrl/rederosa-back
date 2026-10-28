package com.example.rederosaback.model;

import jakarta.persistence.*;

@Entity
public class SalaConversa {
    @Id @Column(length = 40)
    private String id;
    @Column(nullable = false, length = 100)
    private String nome;
    @Column(nullable = false)
    private String descricao;
    @Column(nullable = false)
    private int ordem;

    protected SalaConversa() { }
    public SalaConversa(String id, String nome, String descricao, int ordem) {
        this.id = id; this.nome = nome; this.descricao = descricao; this.ordem = ordem;
    }
    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
}
