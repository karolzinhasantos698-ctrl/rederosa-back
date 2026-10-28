package com.example.rederosaback.controller;

import com.example.rederosaback.model.Usuaria;
import com.example.rederosaback.repository.UsuariaRepository;
import com.example.rederosaback.service.SessaoAcessoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/login")
@CrossOrigin(origins = "*")
public class LoginController {
    private final UsuariaRepository repository;
    private final SessaoAcessoService sessoes;

    public LoginController(UsuariaRepository repository, SessaoAcessoService sessoes) {
        this.repository = repository;
        this.sessoes = sessoes;
    }

    @PostMapping
    public ResponseEntity<?> login(@RequestBody Usuaria dados) {
        Usuaria usuaria = dados.getEmail() == null ? null : repository.findByEmail(dados.getEmail().trim());
        if (usuaria == null || dados.getSenha() == null || !dados.getSenha().equals(usuaria.getSenha())) {
            return ResponseEntity.status(401).body(Map.of("mensagem", "E-mail ou senha incorretos."));
        }
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(new LoginResposta(usuaria.getId(), usuaria.getNome(), usuaria.getEmail(), sessoes.criar(usuaria)));
    }

    public record LoginResposta(Long id, String nome, String email, String token) { }
}
