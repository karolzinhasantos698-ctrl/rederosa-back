package com.example.rederosaback.model;


import jakarta.persistence.*;



@Entity
@Table(name="contatos_sos")
public class Contato {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String nome;


    private String telefone;



    @ManyToOne
    @JoinColumn(name="usuaria_id")
    private Usuaria usuaria;





    public Long getId() {
        return id;
    }


    public String getNome() {
        return nome;
    }


    public void setNome(String nome) {
        this.nome = nome;
    }



    public String getTelefone() {
        return telefone;
    }


    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }



    public Usuaria getUsuaria() {
        return usuaria;
    }


    public void setUsuaria(Usuaria usuaria) {
        this.usuaria = usuaria;
    }


}