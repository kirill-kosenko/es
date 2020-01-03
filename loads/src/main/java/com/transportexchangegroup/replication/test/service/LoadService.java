package com.transportexchangegroup.replication.test.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportexchangegroup.replication.test.domain.CreateLoadCommand;
import com.transportexchangegroup.replication.test.domain.Event;
import com.transportexchangegroup.replication.test.domain.Load;
import com.transportexchangegroup.replication.test.domain.LoadCreatedEvent;
import com.transportexchangegroup.replication.test.domain.UpdateLoadCommand;
import com.transportexchangegroup.replication.test.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoadService {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public LoadService(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    public void createLoad(CreateLoadCommand loadCommand) {
        Load load = new Load(objectMapper);
        List<Event> events = load.createEvents(loadCommand);
        eventRepository.saveAll(events);
    }

    public void updateLoad(UpdateLoadCommand loadCommand) {
        List<Event> events = eventRepository.findByEntityId(loadCommand.getId());
        Load load = new Load(objectMapper);
        load.apply(events);

        List<Event> updateEvents = load.createEvents(loadCommand);
        eventRepository.saveAll(updateEvents);
    }
}
