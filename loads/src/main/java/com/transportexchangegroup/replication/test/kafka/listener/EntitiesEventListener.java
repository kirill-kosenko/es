package com.transportexchangegroup.replication.test.kafka.listener;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportexchangegroup.replication.test.KafkaConfig;
import com.transportexchangegroup.replication.test.entity.Child;
import com.transportexchangegroup.replication.test.entity.Parent;
import com.transportexchangegroup.replication.test.repository.ChildRepository;
import com.transportexchangegroup.replication.test.repository.ParentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static org.apache.commons.lang.StringUtils.isBlank;

@Component
@Slf4j
@AllArgsConstructor
public class EntitiesEventListener {

	private static final String FK_VIOLATION_SQL_SQTATE = "23503";
	private static final String MESSAGE_TYPE_UPDATE = "u";

    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;


    @Autowired
    private ObjectMapper objectMapper;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "parents")
    @Transactional
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

	    if (KafkaConfig.DB_1_REPLICATION_SOURCE.equals(parent.getReplicationSource())) {
	    	return;
	    }

	    if (isBlank(parent.getParentDb1UniqColumn())) {
		    parent.setParentDb1UniqColumn(RandomStringUtils.randomAlphanumeric(5));
	    }

	    Optional<Parent> existsParent = parentRepository.findById(parent.getParentId());

	    // not to rewrite deleted entity in update case
	    if (!existsParent.isPresent() && MESSAGE_TYPE_UPDATE.equals(getMessageType(message))) {
	    	return;
	    }

	    // solves concurrent update problem by overriding own changes and firing
	    // new event with this source but same data as in other master server
	    // so that service is considered as source of true in case of version conflict
	    if (existsParent.isPresent() && existsParent.get().getLogicalVersion() == parent.getLogicalVersion()) {
	    	parent.setLogicalVersion(parent.getLogicalVersion() + 1);
	    	parent.setReplicationSource(KafkaConfig.DB_1_REPLICATION_SOURCE);
	    }

	    log.info("parent: {}", parent);

	    parentRepository.save(parent);
    }

	@KafkaListener(topics = "children")
    public void listenToChild(@Payload String event) {
		Child child = convertMessage(event, Child.class);

		if (KafkaConfig.DB_1_REPLICATION_SOURCE.equals(child.getReplicationSource())) {
			return;
		}

		log.info("child: {}", child);

		try {
			childRepository.save(child);
		} catch (DataIntegrityViolationException e) {
			if (isFkViolationException(e)) {
				log.info("Send {} to retry queue because of FK violation", child);
				kafkaTemplate.send("db-1-retry-children", event);
			}
		}
    }

	@KafkaListener(topics = "db-1-retry-children")
	public void listenToRetryChild(@Payload String event) throws InterruptedException {
		Child child = convertMessage(event, Child.class);

		log.info("child to retry: {}", child);

		try {
			childRepository.save(child);
		} catch (DataIntegrityViolationException e) {
			if (isFkViolationException(e)) {
				TimeUnit.MILLISECONDS.sleep(100);//pause between retrying
				log.info("One more retry for {} because of FK violation", child);
				kafkaTemplate.send("db-1-retry-children", event);
			}
		}
	}

	private boolean isFkViolationException(Exception e) {
		return e.getCause() instanceof ConstraintViolationException
				&& e.getCause().getCause() instanceof PSQLException
				&& FK_VIOLATION_SQL_SQTATE.equals((((PSQLException) e.getCause().getCause()).getSQLState()));
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
