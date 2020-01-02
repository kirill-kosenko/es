package com.transportexchangegroup.replication.test.repository;

import com.transportexchangegroup.replication.test.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
