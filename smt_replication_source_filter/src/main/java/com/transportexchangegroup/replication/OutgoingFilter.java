package com.transportexchangegroup.replication;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.RegexValidator;
import org.apache.kafka.connect.transforms.util.SimpleConfig;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

@Slf4j
public class OutgoingFilter<R extends ConnectRecord<R>> implements Transformation<R> {

	private static final String REPLICATION_SOURCE_PROPERTY = "replication_source";
	private static final String REROUTE_TOPIC_REGEX = "regex";
	private static final String REROUTE_TOPIC_REPLACEMENT = "replacement";

	private static final ConfigDef CONFIG_DEF = new ConfigDef()
			.define(REPLICATION_SOURCE_PROPERTY,
					ConfigDef.Type.STRING,
					ConfigDef.NO_DEFAULT_VALUE,
					new RegexValidator(),
					ConfigDef.Importance.HIGH,
					"Preventing outgoing messages from not own service.")
			.define(REROUTE_TOPIC_REGEX,
					ConfigDef.Type.STRING,
					ConfigDef.NO_DEFAULT_VALUE,
					new RegexValidator(),
					ConfigDef.Importance.HIGH,
					"Regular expression to use for matching.")
			.define(REROUTE_TOPIC_REPLACEMENT,
					ConfigDef.Type.STRING,
					ConfigDef.NO_DEFAULT_VALUE,
					ConfigDef.Importance.HIGH,
					"Replacement string.");

	private Pattern rerouteTopicPattern;
	private String rerouteTopicReplacement;
	private Pattern replicationSourceFieldPattern;

	@Override
	public R apply(R record) {
		log.info("----------------------------------------------------------------");
		log.info("apply invoked: {}", record);

//		 deletion case
//		if (isNull(record.value())) {
//			return rerouteTopic(record);
//		}

		log.info("apply record value: {}", record.value().toString());

		if (!replicationSourceFieldPattern.matcher(record.value().toString()).matches()) {
			log.info("do filtering");
			return null;
		}

		log.info("without filtering, rerouting");

		return rerouteTopic(record);
	}

	private R rerouteTopic(R record) {
		Matcher matcher = rerouteTopicPattern.matcher(record.topic());

		if (rerouteTopicPattern.matcher(record.topic()).matches()) {
			final String topic = matcher.replaceFirst(rerouteTopicReplacement);
			return record.newRecord(
					topic,
					record.kafkaPartition(),
					record.keySchema(),
					record.key(),
					record.valueSchema(),
					record.value(),
					record.timestamp());
		}

		return record;
	}

	@Override
	public ConfigDef config() {
		return CONFIG_DEF;
	}

	@Override
	public void configure(Map<String, ?> configs) {
		SimpleConfig config = new SimpleConfig(CONFIG_DEF, configs);

		String rerouteTopicRegex = config.getString(REROUTE_TOPIC_REGEX);
		rerouteTopicReplacement = config.getString(REROUTE_TOPIC_REPLACEMENT);
		rerouteTopicPattern = Pattern.compile(rerouteTopicRegex);

		String eventsSourceRegex = ".*Struct.*source=Struct.*table=events";
		replicationSourceFieldPattern = Pattern.compile(eventsSourceRegex);

		log.info("eventsSourceRegex: {}", eventsSourceRegex);
	}

	@Override
	public void close() {
	}
}
