# datastore-database-history

Using Google Cloud Datastore as a persistence layer for [Debezium](https://github.com/debezium/debezium) database histories.

## Usage

Add the following values to the worker configuration.

* `offset.storage`: `datastore-database-history.core.DatastoreDatabaseHistory`
* `database.history.datastore.kind`: String. The kind of the Cloud Datastore entity.

## License

Copyright © 2018 Haokang Den

Distributed under the Eclipse Public License, the same as Clojure.
