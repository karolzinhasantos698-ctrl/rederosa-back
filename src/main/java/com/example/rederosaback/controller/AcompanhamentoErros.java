package com.example.rederosaback.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = AcompanhamentoController.class)
public class AcompanhamentoErros {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> tratar(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).header("Cache-Control", "no-store")
                .body(Map.of("mensagem", e.getReason() == null ? "Não foi possível realizar esta ação." : e.getReason()));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> entradaInvalida() {
        return ResponseEntity.badRequest().body(Map.of("mensagem", "Confira as informações e tente novamente."));
    }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<?> concorrencia() {
        return ResponseEntity.status(409).body(Map.of("mensagem", "O acompanhamento mudou em outra página. Atualize antes de tentar novamente."));
    }
}
