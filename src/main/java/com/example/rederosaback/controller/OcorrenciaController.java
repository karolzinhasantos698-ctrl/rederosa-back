package com.example.rederosaback.controller;



import com.example.rederosaback.model.Ocorrencia;
import com.example.rederosaback.repository.OcorrenciaRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;



@RestController
@RequestMapping("/ocorrencias")
@CrossOrigin(origins="*")
public class OcorrenciaController {



    @Autowired
    private OcorrenciaRepository repository;





    @PostMapping
    public Ocorrencia salvar(
            @RequestBody Ocorrencia ocorrencia
    ){



        if(ocorrencia.getStatus()==null){

            ocorrencia.setStatus("Pendente");

        }



        return repository.save(ocorrencia);



    }







    @GetMapping
    public List<Ocorrencia> listar(){


        return repository.findAll();


    }
    @DeleteMapping("/{id}")
    public String excluir(@PathVariable Long id){


        if(repository.existsById(id)){


            repository.deleteById(id);


            return "Ocorrência excluída com sucesso";


        }


        return "Ocorrência não encontrada";


    }



}