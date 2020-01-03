package com.transportexchangegroup.replication.test.api;

import com.transportexchangegroup.replication.test.domain.CreateLoadCommand;
import com.transportexchangegroup.replication.test.domain.UpdateLoadCommand;
import com.transportexchangegroup.replication.test.service.LoadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
public class LoadController {

    private LoadService loadService;

    public LoadController(LoadService loadService) {
        this.loadService = loadService;
    }

    @PostMapping("/loads")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createLoad(@RequestBody CreateLoadCommand load) {
        loadService.createLoad(load);
    }

    @PutMapping("/loads/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void updateLoad(@PathVariable("id") UUID id, @RequestBody UpdateLoadCommand loadCommand) {
        loadCommand.setId(id);
        loadService.updateLoad(loadCommand);
    }
}
