package com.transportexchangegroup.replication.test.entity;


import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

@Accessors(chain = true)
@ToString
@Data
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

    @OneToMany(mappedBy = "parent")
    private List<Child> children;

    // --------------- ONLY FOR MASTER-MASTER MONOLITH-MICROSERVICE_OWNER  REPLICATION --------///
    private String replicationSource;

    private int logicalVersion;
}