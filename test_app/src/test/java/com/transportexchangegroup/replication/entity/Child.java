package com.transportexchangegroup.replication.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;


@ToString(exclude = "parent")
@Accessors(chain = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Child {


	@JsonProperty("child_id")
    private Integer childId;

	@JsonProperty("parent_id")
    private Integer parentId;

	@JsonProperty("child_name")
	private String childName;

	@JsonProperty("replication_source")
	private String replicationSource;
}
