package com.example.rederosaback.controller;


import com.example.rederosaback.model.Usuaria;
import com.example.rederosaback.repository.UsuariaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/recuperar-senha")
@CrossOrigin(origins="*")
public class RecuperarSenhaController {



    @Autowired
    private UsuariaRepository repository;





    @PostMapping
    public String recuperar(
            @RequestBody Usuaria dados
    ){


        Usuaria usuaria =
                repository.findByEmail(dados.getEmail());



        if(usuaria == null){


            return "E-mail não encontrado";


        }




        return "E-mail confirmado! Agora você pode criar uma nova senha 🌸";



    }

    @PutMapping
    public String trocarSenha(
            @RequestBody Usuaria dados
    ){


        Usuaria usuaria =
                repository.findByEmail(dados.getEmail());



        if(usuaria == null){

            return "Usuária não encontrada";

        }



        usuaria.setSenha(dados.getSenha());


        repository.save(usuaria);



        return "Senha alterada com sucesso 🌸";

    }

}