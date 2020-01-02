package com.transportexchangegroup.replication.test.repository;

import com.transportexchangegroup.replication.test.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentRepository extends JpaRepository<Parent, Integer> {
}
