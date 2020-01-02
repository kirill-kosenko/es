package com.transportexchangegroup.replication.test.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

@Accessors(chain = true)
@ToString
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "parents")
public class Parent {

    @Id
    @GenericGenerator(
		    name = "UseExistingIdOtherwiseGenerateUsingIdentity",
		    strategy = "com.transportexchangegroup.replication.test.entity.UseExistingIdOtherwiseGenerateUsingIdentity")
    @GeneratedValue(generator = "UseExistingIdOtherwiseGenerateUsingIdentity")
    private Integer parentId;

    private String parentName;

    @Column(name = "parent_db_1_uniq_column")
    @JsonProperty("parent_db_1_uniq_column")
    private String parentDb1UniqColumn;

    @OneToMany(mappedBy = "parent")
    private List<Child> children;

    // --------------- ONLY FOR MASTER-MASTER MONOLITH-MICROSERVICE_OWNER  REPLICATION --------///
    private String replicationSource;

    private int logicalVersion;
}