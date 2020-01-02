package com.transportexchangegroup.replication.test;

import com.transportexchangegroup.replication.entity.Parent;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.TimeUnit;

public class SchemaChangingTest extends BaseTest {

	@Before
	@After
	public void restoreDbStructureAndRestetApp1Hibernate() throws Exception {
		appFirstExecuteSql("ALTER TABLE parents DROP COLUMN IF EXISTS new_column");
		appFirstExecuteSql("ALTER TABLE parents ADD COLUMN IF NOT EXISTS unused VARCHAR(20)");

		appFirstExecuteSql("DROP TABLE IF EXISTS new_table");
		appFirstExecuteSql(
				"CREATE TABLE IF NOT EXISTS unused_table ( " +
				" unused_table_id SERIAL, " +
				" unused_name varchar(20), " +
				" CONSTRAINT unused_table_id_pkey PRIMARY KEY (unused_table_id))");

		// necessary to reset Hibernate in app-1, otherwise it may expect deleted column
		// and fail adding with exception
		retryOnAppsHibernateSchemaChangeException(() ->
			appFirstUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq"))
		);

		// necessary because of sometime somehow adding of parents executes after clean-db.sql
		// despite add parent method is synchronous and clean-db.sql executes after
		TimeUnit.SECONDS.sleep(1);

		appFirstExecuteSql(IOUtils.toString(new ClassPathResource("clean-db.sql").getInputStream()));
		appSecondExecuteSql(IOUtils.toString(new ClassPathResource("clean-db.sql").getInputStream()));
	}

	@Test
	public void addingColumnDoesNotBrakeReplication() {
		appFirstExecuteSql(
				"ALTER TABLE parents " +
				"ADD COLUMN new_column VARCHAR(20) NOT NULL DEFAULT 'some_value' ");

		retryOnAppsHibernateSchemaChangeException(() -> {
			appFirstUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq"));

			dbFirstCheck(1, "parents WHERE new_column = 'some_value'");
			dbSecondCheck(1, "parents");
		});
	}

	@Test
	public void droppingUnusedColumnDoesNotBrakeReplication() {
		// check column exists before test
		dbFirstCheck(0, "parents WHERE unused = 'not exists value'");

		appFirstExecuteSql("ALTER TABLE parents DROP COLUMN unused");

		retryOnAppsHibernateSchemaChangeException(() -> {
			appFirstUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq"));

			dbSecondCheck(1, "parents");
		});
	}

	@Test
	public void droppingUnusedTableDoesNotBrakeReplication() {
		appFirstExecuteSql("DROP TABLE unused_table");

		retryOnAppsHibernateSchemaChangeException(() ->
			appFirstUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq"))
		);

		dbSecondCheck(1, "parents");
	}

	@Test
	public void addingNewTableDoesNotBrakeReplication() {
		appFirstExecuteSql(
				"CREATE TABLE new_table (" +
				" unused_table_id SERIAL, " +
				" unused_name varchar(20), " +
				" CONSTRAINT new_table_pkey PRIMARY KEY (unused_table_id))");
		appFirstExecuteSql("INSERT INTO new_table (unused_name) VALUES ('bla-bla')");

		retryOnAppsHibernateSchemaChangeException(() ->
			appFirstUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq"))
		);

		dbSecondCheck(1, "parents");
	}

	private void retryOnAppsHibernateSchemaChangeException(Runnable task) {
		for (int i = 0; i < 3; i++) {
			try {
				task.run();
				break;
			} catch (RuntimeException e) {
				if (e.getCause() instanceof ResourceAccessException) {
					// it appears because Hibernate in app-1 expect 'unused'
					// column to be in result, but when we drop it. So
					// "ERROR: cached plan must not change result type"
					// appears and we get HTTP 500 here. It works after retry.
					continue;
				}
				throw new RuntimeException(e);
			}
		}
	}
}
