package com.example.rederosaback;

import com.example.rederosaback.model.Usuaria;
import com.example.rederosaback.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:comunidade;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Transactional
class ComunidadeIntegrationTests {
    private static final String APOIO = "/api/comunidade/salas/apoio-emocional/mensagens";
    private static final String SEGURANCA = "/api/comunidade/salas/seguranca/mensagens";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuariaRepository usuarias;
    @Autowired MensagemComunidadeRepository mensagens;
    @Autowired DenunciaComunidadeRepository denuncias;
    private String maria, ana;

    @BeforeEach
    void preparar() throws Exception { maria = criarConta("Maria Silva"); ana = criarConta("Ana Souza"); }

    @Test
    void criaTresSalasEExigeLoginParaConversar() throws Exception {
        mvc.perform(get("/api/comunidade/salas"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value("apoio-emocional"))
                .andExpect(jsonPath("$[1].id").value("direitos-das-mulheres"))
                .andExpect(jsonPath("$[2].id").value("seguranca"));
        mvc.perform(get(APOIO)).andExpect(status().isUnauthorized());
        mvc.perform(post(APOIO).contentType(MediaType.APPLICATION_JSON)
                .content(corpo("Olá", UUID.randomUUID().toString()))).andExpect(status().isUnauthorized());
    }

    @Test
    void duasUsuariasLeemAMesmaConversaComAutoriaCorreta() throws Exception {
        JsonNode enviada = enviar(APOIO, maria, "Olá, pessoal!", UUID.randomUUID().toString());
        mvc.perform(get(APOIO).header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensagens[0].id").value(enviada.get("id").asLong()))
                .andExpect(jsonPath("$.mensagens[0].autora").value("Maria"))
                .andExpect(jsonPath("$.mensagens[0].propria").value(false))
                .andExpect(jsonPath("$.mensagens[0].criadaEm").isNotEmpty())
                .andExpect(jsonPath("$.mensagens[0].email").doesNotExist())
                .andExpect(jsonPath("$.mensagens[0].autoraId").doesNotExist())
                .andExpect(jsonPath("$.mensagens[0].token").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get(APOIO).header("Authorization", "Bearer " + maria))
                .andExpect(jsonPath("$.mensagens[0].propria").value(true));
        JsonNode resposta = enviar(APOIO, ana, "Bem-vinda, Maria!", UUID.randomUUID().toString());
        mvc.perform(get(APOIO).param("apos", enviada.get("id").asText()).header("Authorization", "Bearer " + maria))
                .andExpect(jsonPath("$.mensagens.length()").value(1))
                .andExpect(jsonPath("$.mensagens[0].id").value(resposta.get("id").asLong()))
                .andExpect(jsonPath("$.mensagens[0].autora").value("Ana"));
    }

    @Test
    void mantemMensagensSeparadasPorSala() throws Exception {
        enviar(APOIO, maria, "Só no apoio", UUID.randomUUID().toString());
        mvc.perform(get(SEGURANCA).header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$.mensagens.length()").value(0));
        enviar(SEGURANCA, ana, "Só na segurança", UUID.randomUUID().toString());
        mvc.perform(get(APOIO).header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$.mensagens.length()").value(1))
                .andExpect(jsonPath("$.mensagens[0].texto").value("Só no apoio"));
        mvc.perform(get("/api/comunidade/salas/inexistente/mensagens").header("Authorization", "Bearer " + maria))
                .andExpect(status().isNotFound());
    }

    @Test
    void paginaHistoricoENovasMensagensSemRepetir() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i=1; i<=6; i++) ids.add(enviar(APOIO, maria, "Mensagem " + i, UUID.randomUUID().toString()).get("id").asLong());
        mvc.perform(get(APOIO).param("limite","2").header("Authorization","Bearer " + ana))
                .andExpect(jsonPath("$.mensagens[0].id").value(ids.get(4)))
                .andExpect(jsonPath("$.mensagens[1].id").value(ids.get(5)))
                .andExpect(jsonPath("$.temAnteriores").value(true));
        mvc.perform(get(APOIO).param("antes",ids.get(4).toString()).param("limite","2").header("Authorization","Bearer " + ana))
                .andExpect(jsonPath("$.mensagens[0].id").value(ids.get(2)))
                .andExpect(jsonPath("$.mensagens[1].id").value(ids.get(3)));
        mvc.perform(get(APOIO).param("apos",ids.get(1).toString()).param("limite","2").header("Authorization","Bearer " + ana))
                .andExpect(jsonPath("$.mensagens[0].id").value(ids.get(2)))
                .andExpect(jsonPath("$.mensagens[1].id").value(ids.get(3)))
                .andExpect(jsonPath("$.temMais").value(true));
        mvc.perform(get(APOIO).param("apos",ids.get(5).toString()).header("Authorization","Bearer " + ana))
                .andExpect(jsonPath("$.mensagens.length()").value(0));
    }

    @Test
    void repetirEnvioNaoDuplicaAMensagem() throws Exception {
        String chave = UUID.randomUUID().toString();
        JsonNode primeira = enviar(APOIO, maria, "Mensagem única", chave);
        JsonNode segunda = enviar(APOIO, maria, "Mensagem única", chave);
        assertEquals(primeira.get("id"), segunda.get("id"));
        assertEquals(1, mensagens.count());
        mvc.perform(post(APOIO).header("Authorization","Bearer " + maria).contentType(MediaType.APPLICATION_JSON)
                .content(corpo("Texto diferente",chave))).andExpect(status().isConflict());
        mvc.perform(post(SEGURANCA).header("Authorization","Bearer " + maria).contentType(MediaType.APPLICATION_JSON)
                .content(corpo("Mensagem única",chave))).andExpect(status().isConflict());
    }

    @Test
    void ignoraAutoriaEnviadaPeloClienteEPreservaTextoLiteral() throws Exception {
        String texto = "<img src=x onerror=alert(1)>\nBom dia";
        JsonNode resposta = json.readTree(mvc.perform(post(APOIO).header("Authorization","Bearer " + maria)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "texto",texto,"chaveEnvio",UUID.randomUUID().toString(),"autoraId",99999,"autora","Impostora"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertEquals("Maria", resposta.get("autora").asText());
        assertEquals(texto, resposta.get("texto").asText());
    }

    @Test
    void rejeitaTextosVaziosGrandesEParametrosInvalidos() throws Exception {
        for (String texto : List.of(" ","\n\t","a".repeat(2001))) {
            mvc.perform(post(APOIO).header("Authorization","Bearer " + maria).contentType(MediaType.APPLICATION_JSON)
                    .content(corpo(texto,UUID.randomUUID().toString()))).andExpect(status().isBadRequest());
        }
        mvc.perform(post(APOIO).header("Authorization","Bearer " + maria).contentType(MediaType.APPLICATION_JSON)
                .content(corpo("Olá","invalida"))).andExpect(status().isBadRequest());
        for (String query : List.of("?limite=0","?limite=101","?apos=-1","?antes=0","?apos=1&antes=2","?apos=texto")) {
            mvc.perform(get(APOIO + query).header("Authorization","Bearer " + maria)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void denunciaFicaRegistradaSemDuplicarENaoAceitaMensagemDeOutraSala() throws Exception {
        long id = enviar(APOIO,maria,"Mensagem para testar denúncia",UUID.randomUUID().toString()).get("id").asLong();
        String rota = APOIO + "/" + id + "/denuncias";
        for (int i=0;i<2;i++) mvc.perform(post(rota).header("Authorization","Bearer " + ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Teste de denúncia\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mensagem").value("Denúncia registrada."));
        assertEquals(1,denuncias.count());
        mvc.perform(post(rota).header("Authorization","Bearer " + maria)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Teste\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(SEGURANCA + "/" + id + "/denuncias").header("Authorization","Bearer " + ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Teste\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void enviosConcorrentesNaoPerdemMensagensDuranteConsultaPorCursor() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Set<Long> recebidas = new HashSet<>();
        List<Future<Long>> envios = new ArrayList<>();
        try {
            for (int i=0;i<12;i++) {
                String texto = "Concorrente " + i, token = i%2==0 ? maria : ana;
                envios.add(executor.submit(() -> enviar(APOIO,token,texto,UUID.randomUUID().toString()).get("id").asLong()));
            }
            long cursor=0;
            for(int i=0;i<100;i++) {
                JsonNode pagina = json.readTree(mvc.perform(get(APOIO).param("apos",Long.toString(cursor))
                        .header("Authorization","Bearer " + maria)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
                for(JsonNode m:pagina.get("mensagens")) { recebidas.add(m.get("id").asLong()); cursor=m.get("id").asLong(); }
                if(envios.stream().allMatch(Future::isDone) && pagina.get("mensagens").isEmpty()) break;
            }
            Set<Long> enviadas=new HashSet<>();
            for(Future<Long> envio:envios)enviadas.add(envio.get(10,TimeUnit.SECONDS));
            JsonNode finalPagina=json.readTree(mvc.perform(get(APOIO).param("apos",Long.toString(cursor))
                    .header("Authorization","Bearer " + maria)).andReturn().getResponse().getContentAsString());
            for(JsonNode m:finalPagina.get("mensagens"))recebidas.add(m.get("id").asLong());
            assertEquals(enviadas,recebidas);
        } finally {
            executor.shutdown();executor.awaitTermination(10,TimeUnit.SECONDS);
            List<Long> ids=new ArrayList<>();for(Future<Long> f:envios)if(f.isDone()&&!f.isCancelled())ids.add(f.get());
            mensagens.deleteAllById(ids);
        }
    }

    private String criarConta(String nome) throws Exception {
        Usuaria u = new Usuaria();u.setNome(nome);u.setEmail(UUID.randomUUID()+"@example.invalid");u.setSenha("teste-comunidade");usuarias.saveAndFlush(u);
        return json.readTree(mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",u.getEmail(),"senha","teste-comunidade"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
    }
    private String corpo(String texto,String chave) throws Exception { return json.writeValueAsString(Map.of("texto",texto,"chaveEnvio",chave)); }
    private JsonNode enviar(String rota,String token,String texto,String chave) throws Exception {
        return json.readTree(mvc.perform(post(rota).header("Authorization","Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(texto,chave))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
}
