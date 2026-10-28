package com.example.rederosaback.repository;


import com.example.rederosaback.model.Usuaria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;



public interface UsuariaRepository
        extends JpaRepository<Usuaria,Long>{



    Usuaria findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuaria u where u.id = :id")
    Optional<Usuaria> buscarParaAcompanhamento(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuaria u where u.id = :id")
    Optional<Usuaria> buscarParaMensagem(@Param("id") Long id);



}
