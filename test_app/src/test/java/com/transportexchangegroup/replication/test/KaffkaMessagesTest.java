package com.transportexchangegroup.replication.test;

import com.transportexchangegroup.replication.entity.Parent;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Slf4j
public class KaffkaMessagesTest extends BaseTest {

	@Test
	public void newMessagesCountCorrespondsToReplicatedEntities() {
		long messagesOnStartInParentTopic = getTopicMessageCount("parents");

		appFirstUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq-bla"));
		dbSecondCheck(1, "parents");
		assertEquals(1, getTopicMessageCount("parents") - messagesOnStartInParentTopic);

		appFirstUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq-bla"));
		dbSecondCheck(2, "parents");
		assertEquals(2, getTopicMessageCount("parents") - messagesOnStartInParentTopic);

		appSecondUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq-bla"));
		dbFirstCheck(3, "parents");
		assertEquals(3, getTopicMessageCount("parents") - messagesOnStartInParentTopic);

		appSecondUpsertParent(new Parent().setParentName("name").setParentDb1UniqColumn("unq-bla"));
		dbFirstCheck(4, "parents");
		assertEquals(4, getTopicMessageCount("parents") - messagesOnStartInParentTopic);
	}
}
