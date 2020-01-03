package com.transportexchangegroup.replication.test.domain;

import javax.persistence.Entity;
import java.util.UUID;

@Entity
public class LoadUpdatedEvent extends Event<Load> {

    public LoadUpdatedEvent() {}

    public LoadUpdatedEvent(UUID aggregateId, String data) {
        super(null, aggregateId, "Load", data);
    }

    @Override
    public void apply(Load aggregate, Load data) {
        if (data.getStatus() != null) {
            aggregate.setStatus(data.getStatus());
        }
        if (data.getFrom() != null) {
            aggregate.setFrom(data.getFrom());
        }
        if (data.getTo() != null) {
            aggregate.setTo(data.getTo());
        }
    }
}
