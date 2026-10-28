package com.example.rederosaback.service;

import com.example.rederosaback.model.*;
import com.example.rederosaback.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class ComunidadeService {
    private final SalaConversaRepository salas;
    private final MensagemComunidadeRepository mensagens;
    private final DenunciaComunidadeRepository denuncias;
    private final UsuariaRepository usuarias;
    private final Clock clock;

    public ComunidadeService(SalaConversaRepository salas, MensagemComunidadeRepository mensagens,
                             DenunciaComunidadeRepository denuncias, UsuariaRepository usuarias, Clock clock) {
        this.salas = salas; this.mensagens = mensagens; this.denuncias = denuncias;
        this.usuarias = usuarias; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SalaResposta> salas() {
        return salas.findAllByOrderByOrdemAsc().stream()
                .map(s -> new SalaResposta(s.getId(), s.getNome(), s.getDescricao())).toList();
    }

    @Transactional(readOnly = true)
    public PaginaMensagens listar(String sala, Long usuariaId, Long apos, Long antes, int limite) {
        if (!salas.existsById(sala)) throw naoEncontrada();
        if (limite < 1 || limite > 100 || (apos != null && apos < 0) || (antes != null && antes <= 0)
                || (apos != null && antes != null)) {
            throw erro(HttpStatus.BAD_REQUEST, "Os parâmetros de consulta da conversa não são válidos.");
        }
        var pagina = PageRequest.of(0, limite + 1);
        List<MensagemComunidade> resultado = new ArrayList<>(apos != null
                ? mensagens.findBySalaIdAndIdGreaterThanOrderByIdAsc(sala, apos, pagina)
                : antes != null ? mensagens.findBySalaIdAndIdLessThanOrderByIdDesc(sala, antes, pagina)
                : mensagens.findBySalaIdOrderByIdDesc(sala, pagina));
        boolean excedeu = resultado.size() > limite;
        if (excedeu) resultado.remove(resultado.size() - 1);
        if (apos == null) Collections.reverse(resultado);
        List<MensagemResposta> itens = resultado.stream().map(m -> resposta(m, usuariaId)).toList();
        return new PaginaMensagens(itens, apos == null && excedeu, apos != null && excedeu,
                itens.isEmpty() ? null : itens.get(0).id(), itens.isEmpty() ? null : itens.get(itens.size()-1).id(), clock.instant());
    }

    public MensagemResposta enviar(String salaId, Long usuariaId, NovaMensagem dados) {
        if (dados == null || dados.texto() == null || dados.texto().isBlank() || dados.texto().length() > 2000
                || dados.texto().indexOf('\0') >= 0) {
            throw erro(HttpStatus.BAD_REQUEST, "Escreva uma mensagem de até 2.000 caracteres.");
        }
        try {
            if (dados.chaveEnvio() == null || !UUID.fromString(dados.chaveEnvio()).toString().equals(dados.chaveEnvio())) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            throw erro(HttpStatus.BAD_REQUEST, "Não foi possível identificar o envio. Atualize a página e tente novamente.");
        }
        String texto = dados.texto().strip();
        // O bloqueio da sala mantém a ordem de confirmação dos IDs, evitando perder
        // mensagens com IDs menores que terminaram de gravar depois de outra consulta.
        SalaConversa sala = salas.buscarParaEnviar(salaId).orElseThrow(this::naoEncontrada);
        Usuaria autora = usuarias.buscarParaMensagem(usuariaId)
                .orElseThrow(() -> erro(HttpStatus.UNAUTHORIZED, "Entre novamente na sua conta para conversar."));
        Optional<MensagemComunidade> anterior = mensagens.findByAutoraIdAndChaveEnvio(usuariaId, dados.chaveEnvio());
        if (anterior.isPresent()) {
            MensagemComunidade m = anterior.get();
            if (!m.getSala().getId().equals(salaId) || !m.getTexto().equals(texto)) {
                throw erro(HttpStatus.CONFLICT, "Este envio já foi usado em outra mensagem. Atualize a conversa.");
            }
            return resposta(m, usuariaId);
        }
        String nome = autora.getNome() == null || autora.getNome().isBlank() ? "Usuária" : autora.getNome().strip().split("\\s+")[0];
        MensagemComunidade criada = mensagens.save(new MensagemComunidade(sala, usuariaId, nome, texto, dados.chaveEnvio(), clock.instant()));
        return resposta(criada, usuariaId);
    }

    public void denunciar(String sala, Long mensagemId, Long denuncianteId, String motivo) {
        if (motivo == null || motivo.isBlank() || motivo.length() > 500) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe o motivo da denúncia em até 500 caracteres.");
        }
        MensagemComunidade m = mensagens.buscarParaDenuncia(mensagemId, sala)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Mensagem não encontrada nesta sala."));
        if (m.getAutoraId().equals(denuncianteId)) throw erro(HttpStatus.BAD_REQUEST, "Você não pode denunciar sua própria mensagem.");
        if (!denuncias.existsByMensagemIdAndDenuncianteId(mensagemId, denuncianteId)) {
            denuncias.save(new DenunciaComunidade(m, denuncianteId, motivo.strip(), clock.instant()));
        }
    }

    private MensagemResposta resposta(MensagemComunidade m, Long usuariaId) {
        return new MensagemResposta(m.getId(), m.getSala().getId(), m.getAutoraNome(), m.getTexto(), m.getCriadaEm(), m.getAutoraId().equals(usuariaId));
    }
    private ResponseStatusException naoEncontrada() { return erro(HttpStatus.NOT_FOUND, "Sala de conversa não encontrada."); }
    private ResponseStatusException erro(HttpStatus status, String mensagem) { return new ResponseStatusException(status, mensagem); }

    public record SalaResposta(String id, String nome, String descricao) { }
    public record NovaMensagem(String texto, String chaveEnvio) { }
    public record MensagemResposta(Long id, String sala, String autora, String texto, Instant criadaEm, boolean propria) { }
    public record PaginaMensagens(List<MensagemResposta> mensagens, boolean temAnteriores, boolean temMais,
                                 Long primeiroId, Long ultimoId, Instant agora) { }
}
