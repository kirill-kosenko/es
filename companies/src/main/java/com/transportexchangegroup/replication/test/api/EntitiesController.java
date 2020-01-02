package com.transportexchangegroup.replication.test.api;

import com.transportexchangegroup.replication.test.entity.Child;
import com.transportexchangegroup.replication.test.entity.Parent;
import com.transportexchangegroup.replication.test.repository.ChildRepository;
import com.transportexchangegroup.replication.test.repository.ParentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

import static java.util.Objects.nonNull;

@Slf4j
@RestController
public class EntitiesController {

	@Autowired
    private ParentRepository parentRepository;

	@Autowired
    private ChildRepository childRepository;

	@PersistenceContext
	private EntityManager entityManager;

    @PostMapping("parent")
    public Parent saveParent(@RequestBody Parent parent) {
    	parent.setReplicationSource("DB-2");
	    int logicalVersion = 0;

	    //imitate that entity were loaded through Hibernate
	    if (nonNull(parent.getParentId())) {
		    Optional<Parent> existsParent = parentRepository.findById(parent.getParentId());
		    if (existsParent.isPresent()) {
			    logicalVersion = existsParent.get().getLogicalVersion();
		    }
	    }

	    return parentRepository.save(parent.setLogicalVersion(++logicalVersion));
    }

	@DeleteMapping("parent/{parentId}")
	public void deleteParent(@PathVariable Integer parentId) {
		parentRepository.deleteById(parentId);
	}

    @PostMapping("child")
    public Child saveChild(@RequestBody Child child) {
    	child.setReplicationSource("DB-2");
        return childRepository.save(child);
    }

	@PostMapping("sql")
	@Transactional
	public void executeSql(@RequestBody String sql) {
		log.info("will execute sql: {}", parentRepository.findAll());
		entityManager.createNativeQuery(sql).executeUpdate();
	}
}
