package com.transportexchangegroup.replication.test.domain;

import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.util.UUID;

@Entity
@NoArgsConstructor
public class LoadCreatedEvent extends Event<Load> {

    public LoadCreatedEvent(String data) {
        super(null, UUID.randomUUID(), "Load", data);
    }

    @Override
    public void apply(Load aggregate, Load data) {
        aggregate.setId(data.getId());
        aggregate.setStatus(data.getStatus());
        aggregate.setFrom(data.getFrom());
        aggregate.setTo(data.getTo());
    }
}
