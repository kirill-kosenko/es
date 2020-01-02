package com.transportexchangegroup.replication.test;

import com.transportexchangegroup.replication.entity.Child;
import com.transportexchangegroup.replication.entity.Parent;
import org.junit.Test;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class RetryingOnFkConstraintViolationTest extends BaseTest {

	@Test
	public void childrenReplicatedToDbWithParentFkConstraintAfterConstraintViolationFixed() {
		long messagesOnStartInDb1RetryChildrenTopic = getTopicMessageCount("db-1-retry-children");

		// add children that violate DB-1 FK constraint
		appSecondAddChild(new Child().setParentId(1).setChildName("cName1"));
		appSecondAddChild(new Child().setParentId(1).setChildName("cName2"));
		appSecondAddChild(new Child().setParentId(2).setChildName("cName3"));
		appSecondAddChild(new Child().setParentId(2).setChildName("cName4"));

		dbSecondCheck(4, "children");


		// check replication works by adding parent that won't fix DB-1 FK constraint
		appSecondUpsertParent(new Parent().setParentId(10).setParentName("pName1").setParentDb1UniqColumn("unq"));

		dbFirstCheck(1, "parents");
		dbSecondCheck(1, "parents");
		dbFirstCheck(0, "children");


		// add parent that fix DB-1 FK violation for first two children
		appSecondUpsertParent(new Parent().setParentId(1).setParentName("pName2").setParentDb1UniqColumn("unq"));

		dbFirstCheck(2, "parents");
		dbSecondCheck(2, "parents");
		dbFirstCheck(2, "children");
		dbFirstCheck(2, "children WHERE parent_id = 1");


		// add parent that fix DB-1 FK violation for last two children
		appFirstUpsertParent(new Parent().setParentId(2).setParentName("pName3").setParentDb1UniqColumn("unq"));

		dbFirstCheck(3, "parents");
		dbSecondCheck(3, "parents");
		dbFirstCheck(4, "children");
		dbFirstCheck(2, "children WHERE parent_id = 1");
		dbFirstCheck(2, "children WHERE parent_id = 2");

		assertThat(messagesOnStartInDb1RetryChildrenTopic, lessThan(getTopicMessageCount("db-1-retry-children")));
	}
}
