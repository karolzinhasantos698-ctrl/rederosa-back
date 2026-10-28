package com.example.rederosaback.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class AcompanhamentoConfig {
    @Bean public Clock clock() { return Clock.systemUTC(); }
}
