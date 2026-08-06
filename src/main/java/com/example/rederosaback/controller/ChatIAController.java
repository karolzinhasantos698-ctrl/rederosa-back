package com.example.rederosaback.controller;


import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*")
public class ChatIAController {


    @GetMapping
    public String teste(){

        return "Chat IA funcionando 🌸";

    }


    @PostMapping
    public String conversar(@RequestBody String mensagem){


        return "Rede Rosa IA 🌸 recebeu: " + mensagem;

    }

}