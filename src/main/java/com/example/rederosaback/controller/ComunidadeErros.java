package com.example.rederosaback.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ComunidadeController.class)
public class ComunidadeErros {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> tratar(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).header("Cache-Control", "no-store")
                .body(Map.of("mensagem", erro.getReason() == null ? "Não foi possível atualizar a conversa." : erro.getReason()));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> invalido() {
        return ResponseEntity.badRequest().header("Cache-Control", "no-store")
                .body(Map.of("mensagem", "Confira as informações da mensagem e tente novamente."));
    }
}
