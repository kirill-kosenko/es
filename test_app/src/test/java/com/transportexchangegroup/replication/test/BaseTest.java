package com.transportexchangegroup.replication.test;


import com.transportexchangegroup.replication.entity.Child;
import com.transportexchangegroup.replication.entity.Parent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@PropertySource(value = "classpath:/application.properties")
@SqlGroup({
	@Sql(value = {"classpath:clean-db.sql"},
		 config = @SqlConfig(dataSource = "datasource_1")),
	@Sql(value = {"classpath:clean-db.sql"},
		 config = @SqlConfig(dataSource = "datasource_2",
		 transactionManager = "transaction_manager_2"))})
public abstract class BaseTest {

	private static final long RETRY_CHECKING_PERIOD_S = 5;
	private static final int PAUSE_BETWEEN_ATTEMPTS_MS = 500;

	private static final String PARENT_METHOD_PATH = "parent";
	private static final String CHILD_METHOD_PATH = "child";

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	@Qualifier("jdbcTemplate_1")
	private JdbcTemplate jdbcTemplate_1;

	@Autowired
	@Qualifier("jdbcTemplate_2")
	private JdbcTemplate jdbcTemplate_2;

	@Autowired
	private ConsumerFactory<String, String> consumerFactory;

	@Value("${app_1.url}")
	private String app_1_url;

	@Value("${app_2.url}")
	private String app_2_url;

	protected void appFirstDeleteParent(Integer parentId) {
		deleteEntity(parentId, app_1_url, PARENT_METHOD_PATH);
	}

	protected void appSecondDeleteParent(Integer parentId) {
		deleteEntity(parentId, app_2_url, PARENT_METHOD_PATH);
	}

	protected void appFirstUpsertParent(Parent parent) {
		postEntity(parent, app_1_url, PARENT_METHOD_PATH);
	}

	protected void appSecondUpsertParent(Parent parent) {
		postEntity(parent, app_2_url, PARENT_METHOD_PATH);
	}

	protected void appFirstAddChild(Child child) {
		postEntity(child, app_1_url, CHILD_METHOD_PATH);
	}

	protected void appSecondAddChild(Child child) {
		postEntity(child, app_2_url, CHILD_METHOD_PATH);
	}

	private void deleteEntity(Integer entityId, String appUrl, String methodPath) {
		try {
			restTemplate.delete(appUrl + "/" + methodPath + "/{entityId}", entityId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private <T> void postEntity(T entity, String appUrl, String methodPath) {
		try {
			HttpEntity<T> request = new HttpEntity<>(entity);
			restTemplate.postForObject(appUrl + "/" + methodPath, request, entity.getClass());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void dbFirstCheck(int expectedCount, String query) {
		checkDb(expectedCount, 1, query, jdbcTemplate_1);
	}

	protected void dbSecondCheck(int expectedCount, String query) {
		checkDb(expectedCount, 2, query, jdbcTemplate_2);
	}

	protected void appFirstExecuteSql(String sql) {
		restTemplate.postForObject(app_1_url + "/sql", sql, String.class);
	}

	protected void appSecondExecuteSql(String sql) {
		restTemplate.postForObject(app_2_url + "/sql", sql, String.class);
	}

	private void checkDb(int expectedRows, int dbNumber, String query, JdbcTemplate jdbcTemplate) {
		long startNanos = System.nanoTime();

		while (startNanos + (RETRY_CHECKING_PERIOD_S * 1_000_000_000) >= System.nanoTime()) {
			if (expectedRows == executeCountQuery(jdbcTemplate, query, dbNumber, expectedRows)) {
				return;
			} else {
				try {
					TimeUnit.MILLISECONDS.sleep(PAUSE_BETWEEN_ATTEMPTS_MS);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		assertEquals(expectedRows, executeCountQuery(jdbcTemplate, query, dbNumber, expectedRows));
	}

	protected void checkMessagesCount(long additionalCount, long initialCount, String topic) {
		long startNanos = System.nanoTime();

		while (startNanos + (RETRY_CHECKING_PERIOD_S * 1_000_000_000) >= System.nanoTime()) {
			if (getTopicMessageCount(topic) - initialCount == additionalCount) {
				return;
			} else {
				try {
					TimeUnit.MILLISECONDS.sleep(PAUSE_BETWEEN_ATTEMPTS_MS);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		assertEquals(
				"wrong additional messages count: ",
				additionalCount,
				getTopicMessageCount(topic) - initialCount);
	}

	private int executeCountQuery(JdbcTemplate jdbcTemplate, String query, int dbNumber, int expectedRows) {
		int actual = JdbcTestUtils.countRowsInTable(jdbcTemplate, query);
		log.info("checking db-{} rows count with query: [{}], expected: {}, actual: {}",
				dbNumber, query, expectedRows, actual);

		return actual;
	}

	protected long getTopicMessageCount(String topicName) {
		Consumer<String, String> consumer = consumerFactory.createConsumer();

		consumer.subscribe(Collections.singleton(topicName));

		List<TopicPartition> topicPartitions = consumer.partitionsFor("parents")
				.stream().map(PartitionInfo::partition)
				.map(p -> new TopicPartition("parents", p))
				.collect(Collectors.toList());

		Map<TopicPartition, Long> partitionsWithOffset = consumer.endOffsets(topicPartitions);

		return partitionsWithOffset.values().stream().mapToLong(i -> i).sum();
	}
}
