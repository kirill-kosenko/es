package com.transportexchangegroup.replication.test.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportexchangegroup.replication.test.dto.LoadDto;
import com.transportexchangegroup.replication.test.service.LoadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LoadController {

    private ObjectMapper objectMapper;
    private LoadService loadService;

    public LoadController(ObjectMapper objectMapper, LoadService loadService) {
        this.objectMapper = objectMapper;
        this.loadService = loadService;
    }

    @PostMapping("/loads")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createLoad(@RequestBody LoadDto load) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(load);
        loadService.createLoad(json);
    }
}
