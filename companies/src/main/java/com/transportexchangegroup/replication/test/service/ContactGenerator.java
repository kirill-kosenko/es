//package com.transportexchangegroup.replication.service;
//
//import com.transportexchangegroup.replication.entity.Child;
//import com.transportexchangegroup.replication.entity.Parent;
//import com.transportexchangegroup.replication.repository.ContactNameRepository;
//import com.transportexchangegroup.replication.repository.ParentRepository;
//import org.apache.commons.lang.RandomStringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static com.transportexchangegroup.replication.KafkaConfig.DB_2_REPLICATION_SOURCE;
//
//
//@Service
//public class ContactGenerator {
//
//	@Autowired
//	private ParentRepository parentRepository;
//
//	@Autowired
//	private ContactNameRepository contactNameRepository;
//
////	@Scheduled(fixedDelay = 2000)
//	public void generateNewContact() {
//		Parent parent = (Parent) new Parent()
//				.setParentName(RandomStringUtils.randomAlphabetic(5))
//				.setReplicationSource(DB_2_REPLICATION_SOURCE);
//
//		List<Child> children = new ArrayList<>();
//
//		parent = parentRepository.save(parent);
//
//		for (int i=0; i < 5; i++) {
//			Child child = new Child()
//					.setParentId(parent.getParentId())
//					.setChildName(RandomStringUtils.randomAlphabetic(7))
//					.setReplicationSource(DB_2_REPLICATION_SOURCE);
//
//			children.add(child);
//		}
//
//		contactNameRepository.saveAll(children);
//	}
//}
