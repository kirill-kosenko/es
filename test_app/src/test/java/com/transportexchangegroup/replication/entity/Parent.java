package com.transportexchangegroup.replication.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@ToString
@Data
public class Parent {

	@JsonProperty("parent_id")
    private Integer parentId;

    @JsonProperty("parent_name")
    private String parentName;

	@JsonProperty("parent_db_1_uniq_column")
	private String parentDb1UniqColumn;

	@JsonProperty("replication_source")
    private String replicationSource;
}