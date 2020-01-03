package com.transportexchangegroup.replication.test.repository;

import com.transportexchangegroup.replication.test.domain.Aggregate;
import com.transportexchangegroup.replication.test.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByEntityId(UUID uuid);
}
