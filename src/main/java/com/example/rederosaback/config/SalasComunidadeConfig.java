package com.example.rederosaback.config;

import com.example.rederosaback.model.SalaConversa;
import com.example.rederosaback.repository.SalaConversaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SalasComunidadeConfig implements ApplicationRunner {
    private final SalaConversaRepository salas;
    public SalasComunidadeConfig(SalaConversaRepository salas) { this.salas = salas; }

    @Override
    public void run(ApplicationArguments args) {
        for (SalaConversa sala : List.of(
                new SalaConversa("apoio-emocional", "Apoio emocional", "Um espaço para conversar, ouvir e acolher.", 1),
                new SalaConversa("direitos-das-mulheres", "Direitos das mulheres", "Compartilhe dúvidas, informações e experiências.", 2),
                new SalaConversa("seguranca", "Segurança", "Troque experiências e dicas de cuidado no dia a dia.", 3))) {
            if (salas.existsById(sala.getId())) continue;
            try { salas.saveAndFlush(sala); }
            catch (DataIntegrityViolationException e) {
                if (!salas.existsById(sala.getId())) throw e;
            }
        }
    }
}
