package com.transportexchangegroup.replication.test.repository;

import com.transportexchangegroup.replication.test.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, Integer> {
}
