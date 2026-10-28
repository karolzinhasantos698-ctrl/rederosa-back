package com.example.rederosaback.controller;

import com.example.rederosaback.service.ComunidadeService;
import com.example.rederosaback.service.SessaoAcessoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comunidade")
@CrossOrigin(origins = "*")
public class ComunidadeController {
    private final ComunidadeService comunidade;
    private final SessaoAcessoService sessoes;
    public ComunidadeController(ComunidadeService comunidade, SessaoAcessoService sessoes) {
        this.comunidade = comunidade; this.sessoes = sessoes;
    }
    @ModelAttribute
    public void semCache(HttpServletResponse resposta) { resposta.setHeader("Cache-Control", "no-store"); }

    @GetMapping("/salas")
    public List<ComunidadeService.SalaResposta> salas() { return comunidade.salas(); }

    @GetMapping("/salas/{sala}/mensagens")
    public ComunidadeService.PaginaMensagens listar(@PathVariable("sala") String sala,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(value = "apos", required = false) Long apos,
            @RequestParam(value = "antes", required = false) Long antes,
            @RequestParam(value = "limite", defaultValue = "50") int limite) {
        return comunidade.listar(sala, sessoes.autenticar(auth).getId(), apos, antes, limite);
    }

    @PostMapping("/salas/{sala}/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    public ComunidadeService.MensagemResposta enviar(@PathVariable("sala") String sala,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody ComunidadeService.NovaMensagem dados) {
        return comunidade.enviar(sala, sessoes.autenticar(auth).getId(), dados);
    }

    @PostMapping("/salas/{sala}/mensagens/{mensagem}/denuncias")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> denunciar(@PathVariable("sala") String sala, @PathVariable("mensagem") Long mensagem,
            @RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Denuncia dados) {
        comunidade.denunciar(sala, mensagem, sessoes.autenticar(auth).getId(), dados.motivo());
        return Map.of("mensagem", "Denúncia registrada.");
    }

    public record Denuncia(String motivo) { }
}
