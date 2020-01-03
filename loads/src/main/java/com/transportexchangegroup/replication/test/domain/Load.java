package com.transportexchangegroup.replication.test.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Load implements Aggregate {

    @JsonIgnore
    private ObjectMapper objectMapper;

    private UUID id;
    private String status;
    private Details from;
    private Details to;


    public Load() {
    }

    public Load(UUID id, String status, Details from, Details to) {
        this.id = id;
        this.status = status;
        this.from = from;
        this.to = to;
    }

    public Load(String status, Details from, Details to) {
        this.status = status;
        this.from = from;
        this.to = to;
    }

    public Load(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void apply(List<Event> events) {
        events.forEach(event -> {
            try {
                event.apply(this, objectMapper.readValue(event.getEventData(), Load.class));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    public List<Event> createEvents(CreateLoadCommand command) {
        // validate
        try {
            return Collections.singletonList(new LoadCreatedEvent(objectMapper.writeValueAsString(command)));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<Event> createEvents(UpdateLoadCommand command) {
        // validate
        try {
            return Collections.singletonList(new LoadUpdatedEvent(command.getId(), objectMapper.writeValueAsString(command)));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Details getFrom() {
        return from;
    }

    public void setFrom(Details from) {
        this.from = from;
    }

    public Details getTo() {
        return to;
    }

    public void setTo(Details to) {
        this.to = to;
    }
}
