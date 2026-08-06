package com.example.rederosaback.controller;


import com.example.rederosaback.model.Usuaria;
import com.example.rederosaback.repository.UsuariaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/usuarias")
@CrossOrigin(origins = "*")
public class UsuariaController {


    @Autowired
    private UsuariaRepository repository;



    @PostMapping("/cadastro")
    public Usuaria cadastrar(@RequestBody Usuaria usuaria){

        return repository.save(usuaria);

    }



    @GetMapping("/teste")
    public String teste(){

        return "Rede Rosa funcionando 🌸";

    }
    @PutMapping("/{id}")
    public Usuaria atualizar(
            @PathVariable Long id,
            @RequestBody Usuaria dados
    ){

        Usuaria usuaria = repository.findById(id).orElse(null);


        if(usuaria != null){

            usuaria.setNome(dados.getNome());

            usuaria.setEmail(dados.getEmail());


            return repository.save(usuaria);

        }


        return null;

    }
}