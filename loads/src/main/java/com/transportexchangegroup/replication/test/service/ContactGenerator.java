package com.transportexchangegroup.replication.test.service;

import com.transportexchangegroup.replication.test.entity.Child;
import com.transportexchangegroup.replication.test.entity.Parent;
import com.transportexchangegroup.replication.test.repository.ChildRepository;
import com.transportexchangegroup.replication.test.repository.ParentRepository;
import com.transportexchangegroup.replication.test.KafkaConfig;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class ContactGenerator {

	@Autowired
	private ParentRepository parentRepository;

	@Autowired
	private ChildRepository childRepository;

//	@Scheduled(fixedDelay = 2000)
	public void generateNewContact() {
		Parent parent = new Parent()
				.setParentName(RandomStringUtils.randomAlphabetic(5))
				.setReplicationSource(KafkaConfig.DB_1_REPLICATION_SOURCE);

		List<Child> children = new ArrayList<>();

		parent = parentRepository.save(parent);

		for (int i=0; i < 5; i++) {
			Child child = new Child()
					.setParentId(parent.getParentId())
					.setChildName(RandomStringUtils.randomAlphabetic(7))
					.setReplicationSource(KafkaConfig.DB_1_REPLICATION_SOURCE);

			children.add(child);
		}

		childRepository.saveAll(children);
	}
}
