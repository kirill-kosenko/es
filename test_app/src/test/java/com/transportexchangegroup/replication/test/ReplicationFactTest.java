package com.transportexchangegroup.replication.test;

import com.transportexchangegroup.replication.entity.Child;
import com.transportexchangegroup.replication.entity.Parent;
import org.junit.Test;

public class ReplicationFactTest extends BaseTest {

	@Test
	public void parentsReplicatesAmongBothDbsWithOddAndEvenIdsAndRightReplicationSource() {

		//parent replicated from db 1 to db 2 with odd id
		appFirstUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq-bla"));

		dbFirstCheck(1, "parents");
		dbSecondCheck(1, "parents");

		dbFirstCheck(1, "parents WHERE parent_id = 1 AND replication_source = 'DB-1'");
		dbSecondCheck(1, "parents WHERE parent_id = 1 AND replication_source = 'DB-1'");


		//parent replicated from db 2 to db 1 with even id
		appSecondUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq-bla"));

		dbFirstCheck(2, "parents");
		dbSecondCheck(2, "parents");

		dbFirstCheck(1, "parents WHERE parent_id = 2 AND replication_source = 'DB-2'");
		dbSecondCheck(1, "parents WHERE parent_id = 2 AND replication_source = 'DB-2'");


		//another parent replicated from db 2 to db 1  with even id
		appSecondUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq-bla"));

		dbFirstCheck(3, "parents");
		dbSecondCheck(3, "parents");

		dbFirstCheck(1, "parents WHERE parent_id = 4 AND replication_source = 'DB-2'");
		dbSecondCheck(1, "parents WHERE parent_id = 4 AND replication_source = 'DB-2'");


		//another parent replicated from db 1 to db 2  with odd id
		appFirstUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq-bla"));

		dbFirstCheck(4, "parents");
		dbSecondCheck(4, "parents");

		dbFirstCheck(1, "parents WHERE parent_id = 3 AND replication_source = 'DB-1'");
		dbSecondCheck(1, "parents WHERE parent_id = 3 AND replication_source = 'DB-1'");
	}

	@Test
	public void childrenReplicatesAmongBothDbsWithOddAndEvenIdsAndRightReplicationSource() {
		// it's necessary to add parent in first DB because there is
		// FK constraint in children table on parent_id in DB-1
		appFirstUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq-bla").setParentId(1));
		dbSecondCheck(1, "parents WHERE parent_id = 1 AND replication_source = 'DB-1'");


		// child replicated from db 1 to db 2 with odd id
		appFirstAddChild(new Child().setChildName("name").setParentId(1));

		dbFirstCheck(1, "children");
		dbSecondCheck(1, "children");

		dbFirstCheck(1, "children WHERE parent_id = 1 AND child_id = 1 AND replication_source = 'DB-1'");
		dbSecondCheck(1, "children WHERE parent_id = 1 AND  child_id = 1 AND replication_source = 'DB-1'");


		// child replicated from db 2 to db 1 with even id
		appSecondAddChild(new Child().setChildName("name").setParentId(1));

		dbFirstCheck(2, "children");
		dbSecondCheck(2, "children");

		dbFirstCheck(1, "children WHERE parent_id = 1 AND child_id = 2 AND replication_source = 'DB-2'");
		dbSecondCheck(1, "children WHERE parent_id = 1 AND  child_id = 2 AND replication_source = 'DB-2'");


		// another child replicated from db 1 to db 2 with odd id
		appFirstAddChild(new Child().setChildName("name").setParentId(1));

		dbFirstCheck(3, "children");
		dbSecondCheck(3, "children");

		dbFirstCheck(1, "children WHERE parent_id = 1 AND child_id = 3 AND replication_source = 'DB-1'");
		dbSecondCheck(1, "children WHERE parent_id = 1 AND  child_id = 3 AND replication_source = 'DB-1'");


		// child replicated from db 2 to db 1 with even id
		appSecondAddChild(new Child().setChildName("name").setParentId(1));

		dbFirstCheck(4, "children");
		dbSecondCheck(4, "children");

		dbFirstCheck(1, "children WHERE parent_id = 1 AND child_id = 4 AND replication_source = 'DB-2'");
		dbSecondCheck(1, "children WHERE parent_id = 1 AND  child_id = 4 AND replication_source = 'DB-2'");
	}

	@Test
	public void primaryKeySavedToDbAsIsIfItPassedForParentAndChildren() {
		//parent from first app
		appFirstUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq-bla").setParentId(58));
		dbFirstCheck(1, "parents WHERE parent_id = 58 AND replication_source = 'DB-1'");
		dbSecondCheck(1, "parents WHERE parent_id = 58 AND replication_source = 'DB-1'");

		dbFirstCheck(1, "parents");
		dbSecondCheck(1, "parents");


		//parent from second app
		appSecondUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq-bla").setParentId(111));
		dbFirstCheck(1, "parents WHERE parent_id = 111 AND replication_source = 'DB-2'");
		dbSecondCheck(1, "parents WHERE parent_id = 111 AND replication_source = 'DB-2'");

		dbFirstCheck(2, "parents");
		dbSecondCheck(2, "parents");


		//child from first app
		appFirstAddChild(new Child().setChildId(555).setParentId(111).setChildName("name-bla"));
		dbFirstCheck(1, "children WHERE child_id = 555 AND replication_source = 'DB-1'");
		dbSecondCheck(1, "children WHERE child_id = 555 AND replication_source = 'DB-1'");

		dbFirstCheck(1, "children");
		dbSecondCheck(1, "children");


		//child from second app
		appSecondAddChild(new Child().setChildId(777).setParentId(58).setChildName("name-bla"));
		dbFirstCheck(1, "children WHERE child_id = 777 AND replication_source = 'DB-2'");
		dbSecondCheck(1, "children WHERE child_id = 777 AND replication_source = 'DB-2'");

		dbFirstCheck(2, "children");
		dbSecondCheck(2, "children");
	}


	@Test
	public void bothAppsHasDataLikeOnSourceOfTruthServiceAfterConcurrentUpdateOfTheSameEntity() {
		// initial insert to have something to update
		appFirstUpsertParent(new Parent().setParentName("initial_version").setParentDb1UniqColumn("unq-bla").setParentId(10));

		// Wait sync, otherwise can be situation when Hibernate try to insert from kafka listener and controller
		// simultaneously, that lead to exception.
		dbSecondCheck(1, "parents WHERE logical_version = 1");

		/* ----- start of testing ----- */


		// app-1 saves and throw version 2 as it has in db version 1]
		appFirstUpsertParent(new Parent().setParentName("app_1_version").setParentDb1UniqColumn("unq-bla").setParentId(10));
		// app-2 saves and throw version 2 as it has in db version 1]
		appSecondUpsertParent(new Parent().setParentName("app_2_version").setParentDb1UniqColumn("unq-bla").setParentId(10));
		// app-1 found that it got alien event with same version as it has in db,
		// so incremented it version, save to db and rethrow as own
		//It means that both db should have version 3 with changes from second app


		dbFirstCheck(1, "parents");
		dbSecondCheck(1, "parents");

		dbFirstCheck(1,
				"parents WHERE parent_name = 'app_2_version' " +
				"AND replication_source = 'DB-1' " +
				"AND logical_version = 3");

		dbSecondCheck(1,
				"parents WHERE parent_name = 'app_2_version' " +
				"AND replication_source = 'DB-1' " +
				"AND logical_version = 3");


		// app-1 saves and throw version 4 as it has in db version 3]
		appFirstUpsertParent(new Parent().setParentName("app_3_version").setParentDb1UniqColumn("unq-bla").setParentId(10));
		// app-2 saves and throw version 4 as it has in db version 3]
		appSecondUpsertParent(new Parent().setParentName("app_4_version").setParentDb1UniqColumn("unq-bla").setParentId(10));
		// app-1 found that it got alien event with same version as it has in db (4),
		// so incremented it version to 5, save to db and rethrow as own

		// two cases possible:
		// 1. app-2 saves and throw version 5 as it has in db version 4 because of it already threw version 4 and]
		appSecondUpsertParent(new Parent().setParentName("app_5_version").setParentDb1UniqColumn("unq-bla").setParentId(10));
		//    app-1 found that it got alien event with same version as it has in db (5),
		//    so incremented it version to 6, save to db and rethrow as own

		// 2. app-2 got event with version 5 from app-1 and save it to db, so next change will be with version 6

		// In both cases both db should have version 6 with last changes from second app.
		// Only replication_source can differs.


		dbFirstCheck(1,
				"parents WHERE parent_name = 'app_5_version' " +
						"AND replication_source = 'DB-1' " +
						"AND logical_version = 6");

		dbSecondCheck(1,
				"parents WHERE parent_name = 'app_5_version' " +
						"AND logical_version = 6");
	}

	@Test
	public void deletionReplicatesForBothApps() {
		long messagesOnStartInParentTopic = getTopicMessageCount("parents");

		// add parent in first app
		appFirstUpsertParent(new Parent().setParentId(10).setParentName("name").setParentDb1UniqColumn("unq-bla"));
		dbSecondCheck(1, "parents");

		// delete replicates to second app
		appFirstDeleteParent(10);
		dbFirstCheck(0, "parents");
		dbSecondCheck(0, "parents");


		// add parent in second app
		appSecondUpsertParent(new Parent().setParentId(20).setParentName("name").setParentDb1UniqColumn("unq-bla"));
		dbFirstCheck(1, "parents");

		// delete replicates to first app
		appSecondDeleteParent(20);
		dbSecondCheck(0, "parents");
		dbFirstCheck(0, "parents");

		// 6 because of each deletion led to two new messages: deletion on leader and deletion on replicator.
		checkMessagesCount(6, messagesOnStartInParentTopic, "parents");
	}

	@Test
	public void bothAppsDeletesDataAfterConcurrentDeleteUpdateOfTheSameEntityCase1() {
		long messagesOnStartInParentTopic = getTopicMessageCount("parents");

		// initial insert to have something to update
		appFirstUpsertParent(new Parent().setParentId(10).setParentName("initial_version").setParentDb1UniqColumn("unq-bla"));

		// Wait sync, otherwise can be situation when Hibernate try to insert from kafka listener and controller
		// simultaneously, that lead to exception.
		dbSecondCheck(1, "parents WHERE logical_version = 1");

		/* ----- start of testing ----- */


		// app-1 saves and fires version 2 as it has in db version 1]
		appFirstUpsertParent(new Parent().setParentId(10).setParentName("app_1_version").setParentDb1UniqColumn("unq-bla"));
		// app-2 deletes and fires deletion]
		// app-2 gets 'update' for deleted entity and do nothing]
		appSecondDeleteParent(10);
		// app-1 got deletion, deletes and fire deletion]

		dbFirstCheck(0, "parents");
		dbSecondCheck(0, "parents");

		checkMessagesCount(4, messagesOnStartInParentTopic, "parents");
	}

	@Test
	public void bothAppsDeletesDataAfterConcurrentDeleteUpdateOfTheSameEntityCase2() {
		long messagesOnStartInParentTopic = getTopicMessageCount("parents");

		// initial insert to have something to update
		appFirstUpsertParent(new Parent().setParentId(10).setParentName("initial_version").setParentDb1UniqColumn("unq-bla"));

		// Wait sync, otherwise can be situation when Hibernate try to insert from kafka listener and controller
		// simultaneously, that lead to exception.
		dbSecondCheck(1, "parents WHERE logical_version = 1");

		/* ----- start of testing ----- */


		// app-2 saves and throw version 2 as it has in db version 1]
		appSecondUpsertParent(new Parent().setParentId(10).setParentName("app_1_version").setParentDb1UniqColumn("unq-bla"));
		// app-1 deletes and fore deletion]
		appFirstDeleteParent(10);
		// app-1 gets 'update for deleted entity and do nothing']
		// app-2 gets deletion, deletes from dband fire deletion']

		dbFirstCheck(0, "parents");
		dbSecondCheck(0, "parents");

		checkMessagesCount(4, messagesOnStartInParentTopic, "parents");
	}
}
