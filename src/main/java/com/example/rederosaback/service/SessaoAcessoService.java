package com.example.rederosaback.service;
import com.example.rederosaback.model.*;
import com.example.rederosaback.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Duration;

@Service
public class SessaoAcessoService {
    private final SessaoAcessoRepository sessoes;
    private final UsuariaRepository usuarias;
    private final Clock clock;
    public SessaoAcessoService(SessaoAcessoRepository sessoes, UsuariaRepository usuarias, Clock clock) {
        this.sessoes = sessoes; this.usuarias = usuarias; this.clock = clock;
    }
    public String criar(Usuaria usuaria) {
        String token = TokenSeguro.gerar();
        sessoes.save(new SessaoAcesso(TokenSeguro.hash(token), usuaria.getId(), clock.instant().plus(Duration.ofDays(7))));
        return token;
    }
    public Usuaria autenticar(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw semSessao();
        String token = authorization.substring(7).trim();
        if (!token.matches("[A-Za-z0-9_-]{43}")) throw semSessao();
        SessaoAcesso sessao = sessoes.findByTokenHashAndExpiraEmAfter(TokenSeguro.hash(token), clock.instant()).orElseThrow(this::semSessao);
        return usuarias.findById(sessao.getUsuarioId()).orElseThrow(this::semSessao);
    }
    private ResponseStatusException semSessao() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Entre novamente na sua conta para continuar.");
    }
}
