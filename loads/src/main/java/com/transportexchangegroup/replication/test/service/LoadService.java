package com.transportexchangegroup.replication.test.service;

import com.transportexchangegroup.replication.test.entity.Event;
import com.transportexchangegroup.replication.test.entity.LoadCreatedEvent;
import com.transportexchangegroup.replication.test.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LoadService {

    private final EventRepository eventRepository;

    public LoadService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void createLoad(String json) {
        Event event = new LoadCreatedEvent();
        event.setEntityId(UUID.randomUUID());
        event.setEventData(json);
        event.setEntityType("Load");
        eventRepository.save(event);
    }
}
