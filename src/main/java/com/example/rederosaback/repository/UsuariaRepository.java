package com.example.rederosaback.repository;


import com.example.rederosaback.model.Usuaria;

import org.springframework.data.jpa.repository.JpaRepository;



public interface UsuariaRepository
        extends JpaRepository<Usuaria,Long>{



    Usuaria findByEmail(String email);



}