package com.example.rederosaback.controller;

import com.example.rederosaback.model.Checkin;
import com.example.rederosaback.repository.CheckinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkin")
@CrossOrigin(origins = "*")
public class CheckinController {

    @Autowired
    private CheckinRepository checkinRepository;

    @PostMapping
    public Checkin salvar(@RequestBody Checkin checkin) {
        return checkinRepository.save(checkin);
    }
}