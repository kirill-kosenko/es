export HOST_IP=$(ip route get 8.8.8.8 | head -1  | rev | cut -d " " -f2 | rev)
export DEBEZIUM_REPLICATION_HOME="$(pwd)"


function recreate-all() {
    stop-all

    _rebuild-filter-and-connect-image

    (\
        cd environment \
        && docker-compose up -d zookeeper \
        && docker-compose up -d kafka \
        && docker-compose up -d postgres_1 \
        && docker-compose up -d connect_1 \
    )

    _db-1-recreate

    _connect-1-configure

    _app-1-rebuild-with-image
#    _app-2-rebuild-with-image
    _apps-clean-restart
}

function _db-1-recreate() {
    local DB=test_db_1
    local DB_SERVER=postgres_1

    _db-recreate $DB $DB_SERVER
}

function _db-recreate() {
    _wait-for-postgres "$DB_SERVER"
    _execSql "postgres" "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB';" "$DB_SERVER"
	_execSql "postgres" "DROP DATABASE IF EXISTS $DB;" "$DB_SERVER"
	_execSql "postgres" "CREATE DATABASE $DB WITH ENCODING='UTF8' OWNER=postgres CONNECTION LIMIT=-1;" "$DB_SERVER"

	_execSql "$DB" "CREATE TABLE IF NOT EXISTS events (event_id    SERIAL PRIMARY KEY, event_type  VARCHAR(100) NOT NULL, entity_type VARCHAR(100) NOT NULL, entity_id   UUID NOT NULL, event_data  VARCHAR(1000) NOT NULL)" "$DB_SERVER"
	_execSql "$DB" "create table entities (entity_type VARCHAR(1000), entity_id VARCHAR(1000), entity_version VARCHAR(1000) NOT NULL, PRIMARY KEY(entity_type, entity_id))" "$DB_SERVER"
	_execSql "$DB" "create table snapshots (entity_type VARCHAR(1000), entity_id VARCHAR(1000), entity_version VARCHAR(1000), snapshot_type VARCHAR(1000) NOT NULL, snapshot_json VARCHAR(1000) NOT NULL, triggering_events VARCHAR(1000), PRIMARY KEY(entity_type, entity_id, entity_version))" "$DB_SERVER"

    _execSql "$DB" "CREATE EXTENSION IF NOT EXISTS postgis;" "$DB_SERVER"
	_execSql "$DB" "CREATE EXTENSION IF NOT EXISTS postgis_topology;" "$DB_SERVER"
}

function _execSql() {
	local db=$1
	local sql=$2
	local db_server=$3
	_echoPhase "execSQL: server[$db_server]  db[$db]: $sql"
	(cd environment && docker-compose run --rm -u postgres postgres-client psql -h "$db_server" -d "$db" -p 5432 -U postgres -c "$sql")
}

function _wait-for-postgres() {
	local RC=0

	_echoPhase "Waiting for postgres on server $1 ..."

	_execSql "postgres" "SELECT 1" "$1" && RC=$? || RC=$?
	echo "==> $RC"
	while [ $RC -ne 0 ]; do
    	_execSql "postgres" "SELECT 1" && RC=$? || RC=$?
    	sleep 2
	done

	_echoPhase "PostgreSQL server $1 is started"
}

function _echoPhase() {
	local MSG=$1
	local date=`date +"%Y-%m-%d-%H:%M:%S"`
	echo -e "SERVER: $SERVER_NUM. NODE: $NODE. ---> [$date] $MSG"
}


function _connect-1-configure() {
    (\
        curl -i -X POST \
        -H "Accept:application/json" \
        -H "Content-Type:application/json" localhost:18083/connectors/ \
        -d '{"name": "db-1-connector", "config": { "connector.class": "io.debezium.connector.postgresql.PostgresConnector", "database.hostname": "postgres_1", "database.port": "5432", "database.user": "postgres", "database.password": "secret", "database.dbname" : "test_db_1", "database.server.name": "db-1-server", "transforms": "filter_and_reroute", "transforms.filter_and_reroute.type": "com.transportexchangegroup.replication.OutgoingFilter", "transforms.filter_and_reroute.replication_source": "DB-1", "transforms.filter_and_reroute.regex": "([^.]+)\\.([^.]+)\\.([^.]+)", "transforms.filter_and_reroute.replacement": "$3"}}'\
    )
}


function _rebuild-filter-and-connect-image() {
    (\
        cd $DEBEZIUM_REPLICATION_HOME/smt_replication_source_filter \
        && mvn clean install \
        && cp target/smt_replication_source_filter.jar $DEBEZIUM_REPLICATION_HOME/debezium_connector/docker \
        && docker build -t tegr/debezium-connect $DEBEZIUM_REPLICATION_HOME/debezium_connector/docker/ \
        && rm -f $DEBEZIUM_REPLICATION_HOME/debezium_connector/docker/smt_replication_source_filter.jar
    )
}

function _connects-clean-restart() {
    (curl -i -X DELETE -H "Accept:application/json" -H "Content-Type:application/json" localhost:18083/connectors/db-1-connector)
    (curl -i -X DELETE -H "Accept:application/json" -H "Content-Type:application/json" localhost:28083/connectors/db-2-connector)

    (\
        cd environment \
        && docker-compose stop connect_1 \
        && docker-compose rm -f connect_1 \
        && docker-compose up -d connect_1 \
    )

    sleep 5

    _connect-1-configure
}

function _app-1-rebuild-with-image() {
    (\
        cd $DEBEZIUM_REPLICATION_HOME/loads \
        && mvn clean install \
        && cp target/loads.jar $DEBEZIUM_REPLICATION_HOME/loads/docker \
        && docker build -t tegr/loads $DEBEZIUM_REPLICATION_HOME/loads/docker/ \
        && rm -f $DEBEZIUM_REPLICATION_HOME/loads/docker/loads.jar
    )
}


function _app-2-rebuild-with-image() {
    (\
        cd $DEBEZIUM_REPLICATION_HOME/companies \
        && mvn clean install \
        && cp target/companies-service.jar $DEBEZIUM_REPLICATION_HOME/companies/docker \
        && docker build -t tegr/companies-service $DEBEZIUM_REPLICATION_HOME/companies/docker/ \
        && rm -f $DEBEZIUM_REPLICATION_HOME/companies/docker/companies-service.jar
    )
}


function _apps-clean-restart() {
    (\
        cd environment \
        && docker-compose stop loads \
        && docker-compose rm -f loads \
        && docker-compose up -d loads \
        && docker-compose stop companies \
        && docker-compose rm -f companies \
        && docker-compose up -d companies \
    )
}

function apps-rebuild-restart() {
    (\
        _app-1-rebuild-with-image \
        && _app-2-rebuild-with-image \
        && _apps-clean-restart \
    )
}

function connectors-rebuild-restart() {
    (\
        _rebuild-filter-and-connect-image \
        && _connects-clean-restart \
    )
}

function tests-start() {
    (\
        cd $DEBEZIUM_REPLICATION_HOME/test_app \
        && mvn clean test
    )
}

function stop-all() {
    (\
        cd environment \
        \
        && docker-compose stop connect_1 \
        && docker-compose rm -f connect_1 \
        && docker-compose stop connect_2 \
        && docker-compose rm -f connect_2 \
        && docker-compose stop postgres_1 \
        && docker-compose rm -f postgres_1 \
        && docker-compose stop postgres_2 \
        && docker-compose rm -f postgres_2 \
        && docker-compose stop kafka \
        && docker-compose rm -f kafka \
        && docker-compose stop zookeeper \
        && docker-compose rm -s -f zookeeper \
        && docker-compose stop loads \
        && docker-compose rm -s -f loads \
        && docker-compose stop companies \
        && docker-compose rm -s -f companies \
    )
}

