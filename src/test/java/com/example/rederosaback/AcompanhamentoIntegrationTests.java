package com.example.rederosaback;

import com.example.rederosaback.model.Usuaria;
import com.example.rederosaback.repository.UsuariaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:acompanhamento;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Import(AcompanhamentoIntegrationTests.TempoConfig.class)
@Transactional
class AcompanhamentoIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuariaRepository usuarias;
    @Autowired TempoControlado tempo;
    private Usuaria conta;
    private String token;

    @BeforeEach
    void preparar() throws Exception {
        tempo.agora = Instant.parse("2026-10-27T18:00:00Z");
        conta = novaConta("Maria Silva");
        token = login(conta).get("token").asText();
    }

    @Test
    void loginCriaSessaoSemExporSenha() throws Exception {
        JsonNode dados = login(conta);
        assertTrue(dados.get("token").asText().matches("[A-Za-z0-9_-]{43}"));
        assertFalse(dados.has("senha"));
        mvc.perform(get("/api/acompanhamentos/ativo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/acompanhamentos/ativo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acompanhamento").doesNotExist());
    }

    @Test
    void fluxoCompletoPersisteECompartilhaSomenteLeitura() throws Exception {
        JsonNode inicio = iniciar(15, "(11) 99999-1234");
        String publico = caminhoPublico(inicio);
        assertEquals("AGUARDANDO", inicio.get("estado").asText());
        assertEquals("2026-10-27T18:15:00Z", inicio.get("prazo").asText());
        mvc.perform(get(publico)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.telefone").value("+5511999991234"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/acompanhamentos/ativo").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.acompanhamento.id").value(inicio.get("id").asInt()));
        JsonNode estendido = acao(inicio, "estender", "{\"minutos\":15}");
        assertEquals("2026-10-27T18:30:00Z", estendido.get("prazo").asText());
        assertEquals(inicio.get("urlAcompanhamento"), estendido.get("urlAcompanhamento"));
        acao(inicio, "confirmar", null);
        mvc.perform(get(publico)).andExpect(jsonPath("$.estado").value("CONFIRMADO"));
        mvc.perform(get("/api/acompanhamentos/ativo").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.acompanhamento").doesNotExist());
        mvc.perform(get("/api/acompanhamentos/historico").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADO"));
    }

    @Test
    void prazoVenceNoServidorEProrrogacaoRecuperaAtraso() throws Exception {
        JsonNode inicio = iniciar(15, null);
        tempo.agora = tempo.agora.plus(Duration.ofMinutes(15));
        mvc.perform(get(caminhoPublico(inicio))).andExpect(jsonPath("$.estado").value("ATRASADO"));
        tempo.agora = tempo.agora.plus(Duration.ofMinutes(5));
        JsonNode prorrogado = acao(inicio, "estender", "{\"minutos\":15}");
        assertEquals("2026-10-27T18:35:00Z", prorrogado.get("prazo").asText());
        mvc.perform(get(caminhoPublico(inicio))).andExpect(jsonPath("$.estado").value("AGUARDANDO"));
    }

    @Test
    void outraContaENavegadorPublicoNaoPodemAlterar() throws Exception {
        JsonNode inicio = iniciar(30, null);
        String outraSessao = login(novaConta("Outra Pessoa")).get("token").asText();
        String endpoint = "/api/acompanhamentos/" + inicio.get("id").asLong() + "/confirmar";
        mvc.perform(post(endpoint)).andExpect(status().isUnauthorized());
        mvc.perform(post(endpoint).header("Authorization", "Bearer " + outraSessao)).andExpect(status().isNotFound());
        mvc.perform(get("/api/acompanhamentos/historico").header("Authorization", "Bearer " + outraSessao))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(caminhoPublico(inicio))).andExpect(jsonPath("$.estado").value("AGUARDANDO"));
    }

    @Test
    void impedeDuplicidadeEDadosInvalidos() throws Exception {
        mvc.perform(post("/api/acompanhamentos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"minutos\":5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/acompanhamentos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"minutos\":15,\"telefone\":\"javascript:123\"}"))
                .andExpect(status().isBadRequest());
        iniciar(60, null);
        mvc.perform(post("/api/acompanhamentos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"minutos\":15}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelamentoNaoViraConfirmacaoELinkExpira() throws Exception {
        JsonNode inicio = iniciar(15, null);
        acao(inicio, "cancelar", null);
        mvc.perform(get(caminhoPublico(inicio))).andExpect(jsonPath("$.estado").value("CANCELADO"));
        mvc.perform(post("/api/acompanhamentos/" + inicio.get("id").asLong() + "/confirmar")
                .header("Authorization", "Bearer " + token)).andExpect(status().isConflict());
        tempo.agora = tempo.agora.plus(Duration.ofHours(24));
        mvc.perform(get(caminhoPublico(inicio))).andExpect(status().isGone());
        mvc.perform(get("/api/acompanhamentos/publico/invalido")).andExpect(status().isNotFound());
    }

    @Test
    void confirmacaoRepetidaNaoAlteraHorarioEEstadoFinal() throws Exception {
        JsonNode inicio = iniciar(15, null);
        JsonNode primeira = acao(inicio, "confirmar", null);
        tempo.agora = tempo.agora.plusSeconds(60);
        JsonNode segunda = acao(inicio, "confirmar", null);
        assertEquals(primeira.get("encerradoEm"), segunda.get("encerradoEm"));
        mvc.perform(post("/api/acompanhamentos/" + inicio.get("id").asLong() + "/estender")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"minutos\":15}")).andExpect(status().isConflict());
    }

    @Test
    void sessaoExpiradaExigeNovoLogin() throws Exception {
        tempo.agora = tempo.agora.plus(Duration.ofDays(8));
        mvc.perform(get("/api/acompanhamentos/ativo").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    void paginaPublicaERecursosSaoServidosPeloBackend() throws Exception {
        mvc.perform(get("/acompanhar/" + "a".repeat(43)))
                .andExpect(status().isOk()).andExpect(forwardedUrl("/acompanhar.html"));
        mvc.perform(get("/acompanhar.html")).andExpect(status().isOk());
        mvc.perform(get("/acompanhamento/acompanhar.js")).andExpect(status().isOk());
        mvc.perform(get("/acompanhamento/logo.png")).andExpect(status().isOk());
    }

    @Test
    void localizacaoAtualizaNasRotasPrivadaEPublica() throws Exception {
        JsonNode inicio = iniciar(30, null);
        enviarPosicao(inicio, -23.5505, -46.6333, tempo.agora);
        mvc.perform(get(caminhoPublico(inicio))).andExpect(status().isOk())
                .andExpect(jsonPath("$.localizacao.latitude").value(-23.5505))
                .andExpect(jsonPath("$.localizacao.longitude").value(-46.6333))
                .andExpect(jsonPath("$.localizacao.precisao").value(12.0))
                .andExpect(jsonPath("$.localizacao.compartilhando").value(true));
        tempo.agora = tempo.agora.plusSeconds(10);
        enviarPosicao(inicio, -23.5510, -46.6340, tempo.agora);
        mvc.perform(get("/api/acompanhamentos/ativo").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.acompanhamento.localizacao.latitude").value(-23.5510));
        mvc.perform(get(caminhoPublico(inicio))).andExpect(jsonPath("$.localizacao.longitude").value(-46.6340))
                .andExpect(jsonPath("$.localizacao.coletadaEm").value("2026-10-27T18:00:10Z"));
    }

    @Test
    void posicaoAntigaNaoSubstituiANova() throws Exception {
        JsonNode inicio = iniciar(30, null);
        Instant antiga = tempo.agora;
        tempo.agora = tempo.agora.plusSeconds(10);
        enviarPosicao(inicio, -23.5510, -46.6340, tempo.agora);
        enviarPosicao(inicio, -20.0, -40.0, antiga);
        mvc.perform(get(caminhoPublico(inicio))).andExpect(jsonPath("$.localizacao.latitude").value(-23.5510));
    }

    @Test
    void pausaImpedeRequisicaoAtrasadaERetomaComNovaPosicao() throws Exception {
        JsonNode inicio = iniciar(30, null);
        enviarPosicao(inicio, -23.5505, -46.6333, tempo.agora);
        Instant requisicaoPendente = tempo.agora.plusSeconds(1);
        tempo.agora = tempo.agora.plusSeconds(2);
        acao(inicio, "localizacao/pausar", null);
        enviarPosicao(inicio, -20.0, -40.0, requisicaoPendente);
        mvc.perform(get(caminhoPublico(inicio)))
                .andExpect(jsonPath("$.localizacao.compartilhando").value(false))
                .andExpect(jsonPath("$.localizacao.latitude").value(-23.5505));
        tempo.agora = tempo.agora.plusSeconds(3);
        enviarPosicao(inicio, -23.5510, -46.6340, tempo.agora);
        mvc.perform(get(caminhoPublico(inicio))).andExpect(jsonPath("$.localizacao.compartilhando").value(true))
                .andExpect(jsonPath("$.localizacao.latitude").value(-23.5510));
    }

    @Test
    void somenteDonaPodeEnviarGpsEEncerramentoInterrompeEnvio() throws Exception {
        JsonNode inicio = iniciar(30, null);
        String endpoint = "/api/acompanhamentos/" + inicio.get("id").asLong() + "/localizacao";
        String corpo = json.writeValueAsString(new com.example.rederosaback.service.AcompanhamentoService.Posicao(-23.55,-46.63,12.0,tempo.agora));
        mvc.perform(put(endpoint).contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isUnauthorized());
        String outraSessao = login(novaConta("Outra Pessoa")).get("token").asText();
        mvc.perform(put(endpoint).header("Authorization","Bearer " + outraSessao)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isNotFound());
        enviarPosicao(inicio, -23.55, -46.63, tempo.agora);
        acao(inicio,"confirmar",null);
        mvc.perform(get(caminhoPublico(inicio))).andExpect(jsonPath("$.localizacao.compartilhando").value(false));
        mvc.perform(put(endpoint).header("Authorization","Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isConflict());
    }

    @Test
    void rejeitaGpsInvalidoOuComHorarioAntigo() throws Exception {
        JsonNode inicio = iniciar(30, null);
        String endpoint = "/api/acompanhamentos/" + inicio.get("id").asLong() + "/localizacao";
        for (var dados : java.util.List.of(
                new com.example.rederosaback.service.AcompanhamentoService.Posicao(91.0,-46.0,10.0,tempo.agora),
                new com.example.rederosaback.service.AcompanhamentoService.Posicao(-23.0,181.0,10.0,tempo.agora),
                new com.example.rederosaback.service.AcompanhamentoService.Posicao(-23.0,-46.0,-1.0,tempo.agora),
                new com.example.rederosaback.service.AcompanhamentoService.Posicao(-23.0,-46.0,10.0,tempo.agora.minusSeconds(121)),
                new com.example.rederosaback.service.AcompanhamentoService.Posicao(-23.0,-46.0,10.0,tempo.agora.plusSeconds(121)))) {
            mvc.perform(put(endpoint).header("Authorization","Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get(caminhoPublico(inicio))).andExpect(jsonPath("$.localizacao").doesNotExist());
    }

    private JsonNode enviarPosicao(JsonNode inicio, double latitude, double longitude, Instant coletadaEm) throws Exception {
        return json.readTree(mvc.perform(put("/api/acompanhamentos/" + inicio.get("id").asLong() + "/localizacao")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new com.example.rederosaback.service.AcompanhamentoService.Posicao(latitude,longitude,12.0,coletadaEm))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private Usuaria novaConta(String nome) {
        Usuaria u = new Usuaria(); u.setNome(nome); u.setEmail(UUID.randomUUID() + "@example.invalid"); u.setSenha("apenas-teste");
        return usuarias.saveAndFlush(u);
    }
    private JsonNode login(Usuaria u) throws Exception {
        return json.readTree(mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("email",u.getEmail(),"senha","apenas-teste"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private JsonNode iniciar(int minutos, String telefone) throws Exception {
        return json.readTree(mvc.perform(post("/api/acompanhamentos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                        new com.example.rederosaback.controller.AcompanhamentoController.Inicio(minutos, telefone))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private JsonNode acao(JsonNode inicio, String acao, String corpo) throws Exception {
        var request = post("/api/acompanhamentos/" + inicio.get("id").asLong() + "/" + acao)
                .header("Authorization", "Bearer " + token);
        if (corpo != null) request.contentType(MediaType.APPLICATION_JSON).content(corpo);
        return json.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private String caminhoPublico(JsonNode inicio) { return "/api/acompanhamentos/publico/" + inicio.get("urlAcompanhamento").asText().substring(inicio.get("urlAcompanhamento").asText().lastIndexOf('/') + 1); }

    @TestConfiguration
    static class TempoConfig { @Bean @Primary TempoControlado tempoControlado() { return new TempoControlado(); } }
    static class TempoControlado extends Clock {
        Instant agora = Instant.parse("2026-10-27T18:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return agora; }
    }
}
