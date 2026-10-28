package com.example.rederosaback.controller;

import com.example.rederosaback.model.Usuaria;
import com.example.rederosaback.repository.UsuariaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsuariaControllerTests {
    @Mock
    private UsuariaRepository repository;

    @InjectMocks
    private UsuariaController controller;

    private MockMvc mvc;
    private Usuaria conta;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        conta = new Usuaria();
        ReflectionTestUtils.setField(conta, "id", 7L);
        conta.setNome("Maria");
        conta.setEmail("maria@example.com");
        conta.setSenha("senha-existente");
    }

    @Test
    void atualizaNomeEEmailSemAlterarSenha() throws Exception {
        when(repository.findById(7L)).thenReturn(Optional.of(conta));
        when(repository.save(conta)).thenReturn(conta);

        mvc.perform(put("/usuarias/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\" Maria Silva \",\"email\":\" nova@example.com \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("nova@example.com"));

        assertEquals("senha-existente", conta.getSenha());
        verify(repository).save(conta);
    }

    @Test
    void contaInexistenteRetorna404ComMensagem() throws Exception {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        mvc.perform(put("/usuarias/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"email\":\"maria@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());

        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaCamposAusentes() throws Exception {
        when(repository.findById(7L)).thenReturn(Optional.of(conta));

        mvc.perform(put("/usuarias/7").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());

        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaEmailInvalido() throws Exception {
        when(repository.findById(7L)).thenReturn(Optional.of(conta));

        mvc.perform(put("/usuarias/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"email\":\"invalido\"}"))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaEmailUsadoPorOutraConta() throws Exception {
        Usuaria outraConta = new Usuaria();
        ReflectionTestUtils.setField(outraConta, "id", 8L);
        when(repository.findById(7L)).thenReturn(Optional.of(conta));
        when(repository.findByEmail("outra@example.com")).thenReturn(outraConta);

        mvc.perform(put("/usuarias/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"email\":\"outra@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());

        assertEquals("maria@example.com", conta.getEmail());
        verify(repository, never()).save(any());
    }

    @Test
    void permiteManterEmailDaPropriaConta() throws Exception {
        when(repository.findById(7L)).thenReturn(Optional.of(conta));
        when(repository.findByEmail("maria@example.com")).thenReturn(conta);
        when(repository.save(conta)).thenReturn(conta);

        mvc.perform(put("/usuarias/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria Silva\",\"email\":\"maria@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"));
    }
}
