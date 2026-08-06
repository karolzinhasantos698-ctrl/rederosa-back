package com.example.rederosaback.controller;


import com.example.rederosaback.model.Contato;
import com.example.rederosaback.repository.ContatoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/contatos")
@CrossOrigin(origins="*")
public class ContatoController {



    @Autowired
    private ContatoRepository repository;




    @PostMapping
    public Contato salvar(@RequestBody Contato contato){


        return repository.save(contato);


    }





    @GetMapping
    public List<Contato> listar(){


        return repository.findAll();


    }

    @DeleteMapping("/{id}")
    public String excluir(@PathVariable Long id){


        if(repository.existsById(id)){


            repository.deleteById(id);


            return "Contato excluído";


        }


        return "Contato não encontrado";

    }
    @PutMapping("/{id}")
    public Contato editar(
            @PathVariable Long id,
            @RequestBody Contato novoContato
    ){


        Contato contato = repository.findById(id)
                .orElseThrow();


        contato.setNome(novoContato.getNome());

        contato.setTelefone(novoContato.getTelefone());


        return repository.save(contato);

    }
}