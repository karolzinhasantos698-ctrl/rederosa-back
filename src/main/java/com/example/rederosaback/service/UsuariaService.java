package com.example.rederosaback.service;


import com.example.rederosaback.model.Usuaria;
import com.example.rederosaback.repository.UsuariaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class UsuariaService {


    @Autowired
    private UsuariaRepository repository;



    public Usuaria salvar(Usuaria usuaria){

        return repository.save(usuaria);

    }


    public Usuaria buscarPorEmail(String email){

        return repository.findByEmail(email);

    }

}