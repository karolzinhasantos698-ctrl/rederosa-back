package com.example.rederosaback.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AcompanhamentoPaginaController {
    @GetMapping("/acompanhar/{token}")
    public String acompanhar(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        return "forward:/acompanhar.html";
    }
}
