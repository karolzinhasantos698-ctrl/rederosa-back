package com.example.rederosaback.controller;

import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*")
public class ChatIAController {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ============================================================
    // TESTE DO BACKEND
    // ============================================================

    @GetMapping
    public String teste() {
        return "Chat IA funcionando 🌸";
    }

    // ============================================================
    // CHAT COM A IA
    // ============================================================

    @PostMapping
    public String conversar(@RequestBody String mensagem) {

        // --------------------------------------------------------
        // 1. PEGA A CHAVE DA GROQ
        // --------------------------------------------------------

        String apiKey = System.getenv("GROQ_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {

            System.out.println("ERRO: GROQ_API_KEY não encontrada.");

            return "Erro: GROQ_API_KEY não foi configurada.";
        }

        // --------------------------------------------------------
        // 2. DATA E HORA
        // --------------------------------------------------------

        LocalDate hoje = LocalDate.now();
        LocalTime agora = LocalTime.now();

        String dataFormatada = hoje.format(
                DateTimeFormatter.ofPattern(
                        "EEEE, dd 'de' MMMM 'de' yyyy",
                        new Locale("pt", "BR")
                )
        );

        String horaFormatada = agora.format(
                DateTimeFormatter.ofPattern("HH:mm")
        );

        // --------------------------------------------------------
        // 3. LIMPA A MENSAGEM RECEBIDA
        // --------------------------------------------------------

        if (mensagem == null) {
            mensagem = "";
        }

        mensagem = mensagem.trim();

        // Caso o frontend envie a mensagem como JSON string:
        // "oie"
        //
        // removemos as aspas externas.

        if (mensagem.startsWith("\"") && mensagem.endsWith("\"")) {

            mensagem = mensagem.substring(
                    1,
                    mensagem.length() - 1
            );
        }

        // --------------------------------------------------------
        // 4. ESCAPA CARACTERES PARA O JSON
        // --------------------------------------------------------

        String mensagemEscapada = escaparJson(mensagem);

        // --------------------------------------------------------
        // 5. INSTRUÇÕES DA ROSA
        // --------------------------------------------------------

        String instrucoes = """
                Você é a Rosa, uma assistente virtual da Rede Rosa.

                Responda sempre em português do Brasil.

                Seja natural, clara, educada, acolhedora e simpática.

                A data de hoje é %s.
                A hora atual é %s.

                Quando o usuário perguntar qual é a data de hoje,
                informe a data fornecida pelo sistema.

                Quando o usuário perguntar a hora,
                informe a hora fornecida pelo sistema.

                Não diga que você não possui acesso à data ou hora,
                pois o sistema está fornecendo essas informações.
                """.formatted(
                dataFormatada,
                horaFormatada
        );

        String instrucoesEscapadas = escaparJson(instrucoes);

        // --------------------------------------------------------
        // 6. JSON CORRETO PARA A GROQ
        // --------------------------------------------------------

        String json = """
                {
                    "model": "openai/gpt-oss-120b",
                    "messages": [
                        {
                            "role": "system",
                            "content": "%s"
                        },
                        {
                            "role": "user",
                            "content": "%s"
                        }
                    ]
                }
                """.formatted(
                instrucoesEscapadas,
                mensagemEscapada
        );

        // --------------------------------------------------------
        // 7. MOSTRA NO CONSOLE O QUE ESTÁ SENDO ENVIADO
        // --------------------------------------------------------

        System.out.println();
        System.out.println("========================================");
        System.out.println("        ENVIANDO PARA A GROQ");
        System.out.println("========================================");
        System.out.println("Mensagem: " + mensagem);
        System.out.println("Modelo: openai/gpt-oss-120b");
        System.out.println("Data: " + dataFormatada);
        System.out.println("Hora: " + horaFormatada);
        System.out.println("========================================");

        // --------------------------------------------------------
        // 8. ENVIA PARA A GROQ
        // --------------------------------------------------------

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(
                            URI.create(
                                    "https://api.groq.com/openai/v1/chat/completions"
                            )
                    )
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .header(
                            "Authorization",
                            "Bearer " + apiKey
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(json)
                    )
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            // ----------------------------------------------------
            // 9. MOSTRA A RESPOSTA DA GROQ NO INTELLIJ
            // ----------------------------------------------------

            System.out.println();
            System.out.println("========================================");
            System.out.println("           RESPOSTA DA GROQ");
            System.out.println("========================================");
            System.out.println(
                    "Status Groq: " + response.statusCode()
            );
            System.out.println(
                    "Resposta Groq: " + response.body()
            );
            System.out.println("========================================");
            System.out.println();

            // ----------------------------------------------------
            // 10. VERIFICA SE A GROQ RETORNOU ERRO
            // ----------------------------------------------------

            if (response.statusCode() != 200) {

                return "Erro da Groq: " + response.body();
            }

            // ----------------------------------------------------
            // 11. PEGA O CONTENT DA RESPOSTA
            // ----------------------------------------------------

            String resposta = response.body();

            String marcador = "\"content\":\"";

            int inicio = resposta.indexOf(marcador);

            if (inicio == -1) {

                System.out.println(
                        "Não encontrei o campo content na resposta."
                );

                return "Não foi possível obter uma resposta da IA.";
            }

            inicio += marcador.length();

            StringBuilder resultado =
                    new StringBuilder();

            boolean escapado = false;

            // ----------------------------------------------------
            // 12. LÊ O TEXTO DA IA
            // ----------------------------------------------------

            for (
                    int i = inicio;
                    i < resposta.length();
                    i++
            ) {

                char c = resposta.charAt(i);

                if (escapado) {

                    switch (c) {

                        case 'n':
                            resultado.append('\n');
                            break;

                        case 'r':
                            resultado.append('\r');
                            break;

                        case 't':
                            resultado.append('\t');
                            break;

                        case '"':
                            resultado.append('"');
                            break;

                        case '\\':
                            resultado.append('\\');
                            break;

                        case '/':
                            resultado.append('/');
                            break;

                        default:
                            resultado.append(c);
                            break;
                    }

                    escapado = false;

                } else {

                    if (c == '\\') {

                        escapado = true;

                    } else if (c == '"') {

                        break;

                    } else {

                        resultado.append(c);
                    }
                }
            }

            // ----------------------------------------------------
            // 13. RESPOSTA FINAL PARA O FRONTEND
            // ----------------------------------------------------

            String respostaFinal =
                    resultado.toString().trim();

            if (respostaFinal.isEmpty()) {

                return "A IA não retornou nenhum texto.";
            }

            System.out.println(
                    "Resposta final da Rosa: "
                            + respostaFinal
            );

            return respostaFinal;

        } catch (Exception e) {

            // ----------------------------------------------------
            // 14. ERRO DE CONEXÃO
            // ----------------------------------------------------

            System.out.println();
            System.out.println("========================================");
            System.out.println("          ERRO AO CHAMAR A GROQ");
            System.out.println("========================================");

            e.printStackTrace();

            System.out.println("========================================");
            System.out.println();

            return "Erro ao conectar com a IA: "
                    + e.getMessage();
        }
    }

    // ============================================================
    // MÉTODO PARA ESCAPAR JSON
    // ============================================================

    private String escaparJson(String texto) {

        if (texto == null) {
            return "";
        }

        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}