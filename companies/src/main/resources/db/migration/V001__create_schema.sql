CREATE sequence parents_parent_id_seq start 2 increment 2;
CREATE sequence children_child_id_seq start 2 increment 2;


CREATE TABLE parents (
 parent_id INTEGER NOT NULL DEFAULT nextval('parents_parent_id_seq'::regclass),
 parent_name varchar(20),
 replication_source varchar(20),
 logical_version INT not null default 0,

 CONSTRAINT parents_parent_id_pk PRIMARY KEY (parent_id)
);


CREATE TABLE children (
 child_id INTEGER NOT NULL DEFAULT nextval('children_child_id_seq'::regclass),
 parent_id INTEGER NOT NULL,
 child_name varchar(20),
 replication_source varchar(20),

 CONSTRAINT children_child_id_pkey PRIMARY KEY (child_id)
);