package com.example.rederosaback.controller;


import com.example.rederosaback.model.Usuaria;
import com.example.rederosaback.repository.UsuariaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/login")
@CrossOrigin(origins = "*")
public class LoginController {


    @Autowired
    private UsuariaRepository repository;



    @PostMapping
    public Usuaria login(@RequestBody Usuaria dados){


        System.out.println("Tentativa login: " + dados.getEmail());


        Usuaria usuaria =
                repository.findByEmail(dados.getEmail());



        if(usuaria == null){

            System.out.println("Usuária não encontrada");

            return null;

        }



        if(dados.getSenha() == null){

            return null;

        }



        if(usuaria.getSenha().equals(dados.getSenha())){


            System.out.println("Login OK id: " + usuaria.getId());

            return usuaria;


        }



        System.out.println("Senha incorreta");


        return null;


    }


}