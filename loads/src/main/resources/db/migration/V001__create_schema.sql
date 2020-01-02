CREATE sequence parents_parent_id_seq start 1 increment 2;
CREATE sequence children_child_id_seq start 1 increment 2;


CREATE TABLE parents (
 parent_id INTEGER NOT NULL DEFAULT nextval('parents_parent_id_seq'::regclass),
 parent_name varchar(20),
 parent_db_1_uniq_column varchar(20) NOT NULL,
 replication_source varchar(20),
 logical_version INT not null default 0,
 unused varchar(20),

 CONSTRAINT parents_parent_id_pk PRIMARY KEY (parent_id)
);


CREATE TABLE children (
 child_id INTEGER NOT NULL DEFAULT nextval('children_child_id_seq'::regclass),
 parent_id INTEGER NOT NULL,
 child_name varchar(20),
 replication_source varchar(20),

 CONSTRAINT children_child_id_pkey PRIMARY KEY (child_id),
 CONSTRAINT parents_parent_id_fk FOREIGN KEY (parent_id) REFERENCES parents (parent_id)
);

CREATE TABLE unused_table (
 unused_table_id SERIAL,
 unused_name varchar(20),

 CONSTRAINT unused_table_id_pkey PRIMARY KEY (unused_table_id)
);