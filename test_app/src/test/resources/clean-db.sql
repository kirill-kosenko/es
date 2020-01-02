TRUNCATE parents CASCADE;
ALTER SEQUENCE parents_parent_id_seq RESTART;

TRUNCATE children CASCADE;
ALTER SEQUENCE children_child_id_seq RESTART;
