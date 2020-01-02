package com.transportexchangegroup.replication.test.kafka.listener;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportexchangegroup.replication.test.KafkaConfig;
import com.transportexchangegroup.replication.test.entity.Child;
import com.transportexchangegroup.replication.test.entity.Parent;
import com.transportexchangegroup.replication.test.repository.ChildRepository;
import com.transportexchangegroup.replication.test.repository.ParentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.isNull;

@Component
@Slf4j
@AllArgsConstructor
public class EntitiesEventListener {

	private static final String MESSAGE_TYPE_UPDATE = "u";

    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
	private EntityManagerFactory entityManagerFactory;

    @Transactional
    @KafkaListener(topics = "parents")
    public void listenToParent(@Payload(required = false) String message,
                               @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) {

    	//deletion case
	    if (isNull(message)) {
		    Integer parentId = convertKey(key, Parent.class).getParentId();

		    //avoids exception when deletes unexist entity
		    if (parentRepository.existsById(parentId)) {
			    parentRepository.deleteById(parentId);
		    }

		    return;
	    }


	    Parent parent = convertMessage(message, Parent.class);

	    if (KafkaConfig.DB_2_REPLICATION_SOURCE.equals(parent.getReplicationSource())) {
	    	return;
	    }

	    Optional<Parent> existsParent = parentRepository.findById(parent.getParentId());

	    // not to rewrite deleted entity in update case
	    if (!existsParent.isPresent() && MESSAGE_TYPE_UPDATE.equals(getMessageType(message))) {
		    return;
	    }


	    log.info("parent: {}", parent);

	    parentRepository.save(parent);
    }

	@KafkaListener(topics = "children")
    public void listenToChild(@Payload String event) {
		Child child = convertMessage(event, Child.class);

		if (KafkaConfig.DB_2_REPLICATION_SOURCE.equals(child.getReplicationSource())) {
			return;
		}

		log.info("child: {}", child);

        childRepository.save(child);

    }

    private <T> T convertMessage(String input, Class<T> targetClass) {
    	try {
		    return objectMapper.convertValue(((Map) (objectMapper.readValue(input, Map.class).get("payload"))).get("after"), targetClass);
	    } catch (Exception e) {
    		throw new RuntimeException(e);
	    }
    }

	private <T> T convertKey(String input, Class<T> targetClass) {
		try {
			return objectMapper.convertValue(objectMapper.readValue(input, Map.class).get("payload"), targetClass);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String getMessageType(String input) {
		try {
			String messageType = String.valueOf(((Map) (objectMapper.readValue(input, Map.class).get("payload"))).get("op"));
			log.info("type: [{}] from message: [{}]", messageType, input);
			return messageType;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
