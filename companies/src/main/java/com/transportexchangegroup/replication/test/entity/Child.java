package com.transportexchangegroup.replication.test.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;


@ToString(exclude = "parent")
@Accessors(chain = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "children")
public class Child {


	@Id
	@GenericGenerator(
			name = "UseExistingIdOtherwiseGenerateUsingIdentity",
			strategy = "com.transportexchangegroup.replication.test.entity.UseExistingIdOtherwiseGenerateUsingIdentity")
	@GeneratedValue(generator = "UseExistingIdOtherwiseGenerateUsingIdentity")
    private Integer childId;

	@ManyToOne
	@JoinColumn(name = "parent_id", insertable = false, updatable = false)
	private Parent parent;

	@Column(name = "parent_id")
    private Integer parentId;

	private String childName;


	// --------------- ONLY FOR MASTER-MASTER MONOLITH-MICROSERVICE_OWNER  REPLICATION --------///
	private String replicationSource;
}
