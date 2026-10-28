package com.example.rederosaback.controller;

import com.example.rederosaback.model.Usuaria;
import com.example.rederosaback.service.AcompanhamentoService;
import com.example.rederosaback.service.SessaoAcessoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;

@RestController
@RequestMapping("/api/acompanhamentos")
@CrossOrigin(origins = "*")
public class AcompanhamentoController {
    private final AcompanhamentoService service;
    private final SessaoAcessoService sessoes;
    private final String publicBaseUrl;

    public AcompanhamentoController(AcompanhamentoService service, SessaoAcessoService sessoes,
                                    @Value("${rederosa.public-base-url:}") String publicBaseUrl) {
        this.service = service; this.sessoes = sessoes; this.publicBaseUrl = publicBaseUrl;
    }

    @ModelAttribute
    public void semCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
    }

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public AcompanhamentoService.Privado iniciar(@RequestHeader(value = "Authorization", required = false) String auth,
                                                 @RequestBody Inicio dados) {
        Usuaria usuario = sessoes.autenticar(auth);
        return service.privado(service.iniciar(usuario, dados.minutos(), dados.telefone()), baseUrl());
    }

    @GetMapping("/ativo")
    public Ativo ativo(@RequestHeader(value = "Authorization", required = false) String auth) {
        return new Ativo(service.privado(service.ativo(sessoes.autenticar(auth).getId()), baseUrl()));
    }

    @GetMapping("/historico")
    public List<AcompanhamentoService.Privado> historico(@RequestHeader(value = "Authorization", required = false) String auth) {
        return service.historico(sessoes.autenticar(auth).getId()).stream().map(a -> service.privado(a, baseUrl())).toList();
    }

    @PostMapping("/{id}/confirmar")
    public AcompanhamentoService.Privado confirmar(@PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return service.privado(service.confirmar(id, sessoes.autenticar(auth).getId()), baseUrl());
    }

    @PostMapping("/{id}/estender")
    public AcompanhamentoService.Privado estender(@PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Inicio dados) {
        return service.privado(service.estender(id, sessoes.autenticar(auth).getId(), dados.minutos()), baseUrl());
    }

    @PostMapping("/{id}/cancelar")
    public AcompanhamentoService.Privado cancelar(@PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return service.privado(service.cancelar(id, sessoes.autenticar(auth).getId()), baseUrl());
    }

    @GetMapping("/publico/{token}")
    public AcompanhamentoService.Publico publico(@PathVariable("token") String token) { return service.acompanhar(token); }

    @PutMapping("/{id}/localizacao")
    public AcompanhamentoService.Privado localizar(@PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody AcompanhamentoService.Posicao dados) {
        return service.privado(service.localizar(id, sessoes.autenticar(auth).getId(), dados), baseUrl());
    }

    @PostMapping("/{id}/localizacao/pausar")
    public AcompanhamentoService.Privado pausarLocalizacao(@PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return service.privado(service.pausarLocalizacao(id, sessoes.autenticar(auth).getId()), baseUrl());
    }

    private String baseUrl() {
        return publicBaseUrl.isBlank() ? ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() : publicBaseUrl;
    }

    public record Inicio(Integer minutos, String telefone) { }
    public record Ativo(AcompanhamentoService.Privado acompanhamento) { }
}
