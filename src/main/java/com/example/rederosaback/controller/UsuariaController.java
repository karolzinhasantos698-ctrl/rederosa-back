package com.example.rederosaback.controller;


import com.example.rederosaback.model.Usuaria;
import com.example.rederosaback.repository.UsuariaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


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
    public ResponseEntity<?> atualizar(
            @PathVariable("id") Long id,
            @RequestBody Usuaria dados
    ){

        Usuaria usuaria = repository.findById(id).orElse(null);


        if (usuaria == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("mensagem", "Conta não encontrada. Entre novamente."));
        }

        String nome = dados.getNome() == null ? "" : dados.getNome().trim();
        String email = dados.getEmail() == null ? "" : dados.getEmail().trim();

        if (nome.isEmpty() || nome.length() > 255 || email.length() > 255
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensagem", "Informe seu nome e um e-mail válido."));
        }

        Usuaria contaComEmail = repository.findByEmail(email);
        if (contaComEmail != null && !id.equals(contaComEmail.getId())) {
            return ResponseEntity.status(409)
                    .body(Map.of("mensagem", "Este e-mail já está sendo usado por outra conta."));
        }

        usuaria.setNome(nome);
        usuaria.setEmail(email);
        return ResponseEntity.ok(repository.save(usuaria));

    }
}
