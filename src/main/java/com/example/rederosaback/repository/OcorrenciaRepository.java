package com.example.rederosaback.repository;


import com.example.rederosaback.model.Ocorrencia;

import org.springframework.data.jpa.repository.JpaRepository;



public interface OcorrenciaRepository
        extends JpaRepository<Ocorrencia, Long> {


}