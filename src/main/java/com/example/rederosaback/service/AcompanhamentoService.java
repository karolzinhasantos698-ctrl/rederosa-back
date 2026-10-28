package com.example.rederosaback.service;

import com.example.rederosaback.model.Acompanhamento;
import com.example.rederosaback.model.Usuaria;
import com.example.rederosaback.repository.AcompanhamentoRepository;
import com.example.rederosaback.repository.UsuariaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class AcompanhamentoService {
    private final AcompanhamentoRepository acompanhamentos;
    private final UsuariaRepository usuarias;
    private final Clock clock;

    public AcompanhamentoService(AcompanhamentoRepository acompanhamentos, UsuariaRepository usuarias, Clock clock) {
        this.acompanhamentos = acompanhamentos;
        this.usuarias = usuarias;
        this.clock = clock;
    }

    public Acompanhamento iniciar(Usuaria usuario, Integer minutos, String telefone) {
        validarMinutos(minutos);
        String numero = normalizarTelefone(telefone);
        // Serializa inícios concorrentes da mesma conta, inclusive em abas diferentes.
        Usuaria conta = usuarias.buscarParaAcompanhamento(usuario.getId())
                .orElseThrow(() -> erro(HttpStatus.UNAUTHORIZED, "Entre novamente na sua conta."));
        if (ativo(conta.getId()) != null) {
            throw erro(HttpStatus.CONFLICT, "Você já tem um acompanhamento. Confirme ou encerre antes de iniciar outro.");
        }
        if (conta.getNome() == null || conta.getNome().isBlank()) {
            throw erro(HttpStatus.BAD_REQUEST, "Preencha seu nome no Perfil antes de iniciar.");
        }
        Instant agora = clock.instant();
        return acompanhamentos.save(new Acompanhamento(conta.getId(), conta.getNome().trim(), numero,
                TokenSeguro.gerar(), agora, agora.plus(Duration.ofMinutes(minutos))));
    }

    @Transactional(readOnly = true)
    public Acompanhamento ativo(Long usuarioId) {
        return acompanhamentos.findFirstByUsuarioIdAndEstadoOrderByInicioDesc(usuarioId, Acompanhamento.Estado.EM_ANDAMENTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Acompanhamento> historico(Long usuarioId) {
        return acompanhamentos.findTop20ByUsuarioIdOrderByInicioDesc(usuarioId);
    }

    public Acompanhamento confirmar(Long id, Long usuarioId) {
        Acompanhamento a = buscarDono(id, usuarioId);
        if (a.getEstado() == Acompanhamento.Estado.CONFIRMADO) return a;
        exigirAberto(a);
        a.encerrar(Acompanhamento.Estado.CONFIRMADO, clock.instant());
        return acompanhamentos.save(a);
    }

    public Acompanhamento estender(Long id, Long usuarioId, Integer minutos) {
        validarMinutos(minutos);
        Acompanhamento a = buscarDono(id, usuarioId);
        exigirAberto(a);
        Instant base = clock.instant().isAfter(a.getPrazo()) ? clock.instant() : a.getPrazo();
        Instant novoPrazo = base.plus(Duration.ofMinutes(minutos));
        if (novoPrazo.isAfter(a.getInicio().plus(Duration.ofHours(24)))) {
            throw erro(HttpStatus.CONFLICT, "Este acompanhamento atingiu 24 horas. Encerre e inicie outro.");
        }
        a.estenderAte(novoPrazo);
        return acompanhamentos.save(a);
    }

    public Acompanhamento cancelar(Long id, Long usuarioId) {
        Acompanhamento a = buscarDono(id, usuarioId);
        if (a.getEstado() == Acompanhamento.Estado.CANCELADO) return a;
        exigirAberto(a);
        a.encerrar(Acompanhamento.Estado.CANCELADO, clock.instant());
        return acompanhamentos.save(a);
    }

    public Acompanhamento localizar(Long id, Long usuarioId, Posicao dados) {
        Acompanhamento a = buscarDono(id, usuarioId);
        exigirAberto(a);
        Instant agora = clock.instant();
        if (dados == null || dados.latitude() == null || dados.longitude() == null || dados.precisao() == null
                || !Double.isFinite(dados.latitude()) || !Double.isFinite(dados.longitude()) || !Double.isFinite(dados.precisao())
                || Math.abs(dados.latitude()) > 90 || Math.abs(dados.longitude()) > 180
                || dados.precisao() < 0 || dados.precisao() > 1_000_000 || dados.coletadaEm() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "A localização recebida não é válida. Tente ativar o GPS novamente.");
        }
        if (dados.coletadaEm().isAfter(agora.plusSeconds(120)) || dados.coletadaEm().isBefore(agora.minusSeconds(120))) {
            throw erro(HttpStatus.BAD_REQUEST, "A posição não é recente. Verifique o horário do aparelho e atualize o GPS.");
        }
        // Uma requisição atrasada nunca substitui uma posição mais nova.
        if (a.getLocalizacaoColetadaEm() != null && !dados.coletadaEm().isAfter(a.getLocalizacaoColetadaEm())) return a;
        if (a.getLocalizacaoPausadaEm() != null && !dados.coletadaEm().isAfter(a.getLocalizacaoPausadaEm())) return a;
        a.atualizarLocalizacao(dados.latitude(), dados.longitude(), dados.precisao(), dados.coletadaEm(), agora);
        return acompanhamentos.save(a);
    }

    public Acompanhamento pausarLocalizacao(Long id, Long usuarioId) {
        Acompanhamento a = buscarDono(id, usuarioId);
        if (a.getEstado() == Acompanhamento.Estado.EM_ANDAMENTO) {
            a.pausarLocalizacao(clock.instant());
            return acompanhamentos.save(a);
        }
        return a;
    }

    @Transactional(readOnly = true)
    public Publico acompanhar(String token) {
        if (!token.matches("[A-Za-z0-9_-]{43}")) throw erro(HttpStatus.NOT_FOUND, "Link não encontrado.");
        Acompanhamento a = acompanhamentos.findByTokenPublico(token)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Link não encontrado."));
        if (!clock.instant().isBefore(expiraEm(a))) {
            throw erro(HttpStatus.GONE, "Este link expirou. Peça um novo acompanhamento à usuária.");
        }
        // O link público nunca retorna e-mail, ID da conta ou credencial de acesso.
        return new Publico(a.getNome().split("\\s+")[0], a.getTelefone(), estado(a), a.getInicio(),
                a.getPrazo(), a.getEncerradoEm(), expiraEm(a), clock.instant(), localizacao(a));
    }

    public Privado privado(Acompanhamento a, String baseUrl) {
        if (a == null) return null;
        return new Privado(a.getId(), a.getNome(), a.getTelefone(), estado(a), a.getInicio(),
                a.getPrazo(), a.getEncerradoEm(), expiraEm(a), clock.instant(),
                baseUrl.replaceAll("/+$", "") + "/acompanhar/" + a.getTokenPublico(), localizacao(a));
    }

    private Localizacao localizacao(Acompanhamento a) {
        if (a.getLatitude() == null) return null;
        return new Localizacao(a.getLatitude(), a.getLongitude(), a.getPrecisao(), a.getLocalizacaoColetadaEm(),
                a.getLocalizacaoRecebidaEm(), a.getEstado() == Acompanhamento.Estado.EM_ANDAMENTO && a.isCompartilhandoLocalizacao());
    }

    public String estado(Acompanhamento a) {
        if (a.getEstado() != Acompanhamento.Estado.EM_ANDAMENTO) return a.getEstado().name();
        // O prazo é decidido pelo relógio do servidor, mesmo com todas as páginas fechadas.
        return clock.instant().isBefore(a.getPrazo()) ? "AGUARDANDO" : "ATRASADO";
    }

    private Instant expiraEm(Acompanhamento a) {
        Instant limite = a.getInicio().plus(Duration.ofHours(48));
        Instant fim = a.getEncerradoEm() == null ? a.getPrazo() : a.getEncerradoEm();
        Instant expiracao = fim.plus(Duration.ofHours(24));
        return expiracao.isBefore(limite) ? expiracao : limite;
    }

    private Acompanhamento buscarDono(Long id, Long usuarioId) {
        return acompanhamentos.buscarParaAlterar(id, usuarioId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Acompanhamento não encontrado na sua conta."));
    }

    private void exigirAberto(Acompanhamento a) {
        if (a.getEstado() != Acompanhamento.Estado.EM_ANDAMENTO) {
            throw erro(HttpStatus.CONFLICT, "Este acompanhamento já foi encerrado.");
        }
    }

    private void validarMinutos(Integer minutos) {
        if (minutos == null || !List.of(15, 30, 60).contains(minutos)) {
            throw erro(HttpStatus.BAD_REQUEST, "Escolha 15, 30 ou 60 minutos.");
        }
    }

    private String normalizarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) return null;
        if (!telefone.matches("[+0-9() .-]{8,30}")) throw erro(HttpStatus.BAD_REQUEST, "Informe um telefone válido com DDD.");
        String numero = telefone.replaceAll("\\D", "");
        if (!telefone.trim().startsWith("+") && (numero.length() == 10 || numero.length() == 11)) numero = "55" + numero;
        if (!numero.matches("[1-9][0-9]{9,14}")) throw erro(HttpStatus.BAD_REQUEST, "Informe um telefone válido com DDD.");
        return "+" + numero;
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) { return new ResponseStatusException(status, mensagem); }

    public record Privado(Long id, String nome, String telefone, String estado, Instant inicio, Instant prazo,
                          Instant encerradoEm, Instant expiraEm, Instant agora, String urlAcompanhamento, Localizacao localizacao) { }
    public record Publico(String nome, String telefone, String estado, Instant inicio, Instant prazo,
                          Instant encerradoEm, Instant expiraEm, Instant agora, Localizacao localizacao) { }
    public record Posicao(Double latitude, Double longitude, Double precisao, Instant coletadaEm) { }
    public record Localizacao(Double latitude, Double longitude, Double precisao, Instant coletadaEm,
                              Instant recebidaEm, boolean compartilhando) { }
}
