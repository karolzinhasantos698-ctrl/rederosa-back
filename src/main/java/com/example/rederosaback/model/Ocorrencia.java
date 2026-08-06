package com.example.rederosaback.model;


import jakarta.persistence.*;



@Entity
@Table(name="ocorrencias")
public class Ocorrencia {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String titulo;


    @Column(length = 1000)
    private String descricao;


    private String status;





    public Long getId() {
        return id;
    }



    public String getTitulo() {
        return titulo;
    }



    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }




    public String getDescricao() {
        return descricao;
    }




    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }




    public String getStatus() {
        return status;
    }




    public void setStatus(String status) {
        this.status = status;
    }


}