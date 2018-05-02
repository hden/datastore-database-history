(ns datastore-database-history.core-test
  (:require [clojure.test :refer :all])
  (:import [io.debezium.config Configuration]
           [io.debezium.relational Tables]
           [io.debezium.relational.ddl DdlParserSql2003]
           [datastore-database-history.core DatastoreDatabaseHistory]))

(defn create-config [m]
  (let [builder (Configuration/create)]
    (doseq [[key value] m]
      (.with builder key value))
    (.build builder)))

(defn create-histoy [m]
  (let [history (new DatastoreDatabaseHistory)]
    (doto history
      (.configure (create-config m) nil)
      (.start))
    history))

(defn create-position [filename position entry]
  {"file" filename
   "position" position
   "entry" entry})

(deftest abstract-database-history-test
  ;; Reproduce tests from https://bit.ly/2Kwg216
  (testing "should record changes and recover to various points"
    (let [parser (new DdlParserSql2003)
          tables (new Tables)
          t0 (new Tables)
          t1 (new Tables)
          t2 (new Tables)
          t3 (new Tables)
          all (new Tables)
          source1 {"server" "abc"}
          source2 {"server" "xyz"}
          history (create-histoy {"database.history.datastore.kind" "foobar"})
          record! (fn [pos entry ddl & update]
                    (.record history source1 (create-position "a.log" pos entry) "db" ddl)
                    (doseq [tables update]
                      (.setCurrentSchema parser "db")
                      (.parse parser ddl tables)))
          recover (fn [pos entry]
                     (let [result (new Tables)]
                       (.recover history source1 (create-position "a.log" pos entry) result parser)
                       result))]
      (record! 1 0 "CREATE TABLE foo ( first VARCHAR(22) NOT NULL );" all t3 t2 t1 t0)
      (record! 23 1 "CREATE TABLE person ( name VARCHAR(22) NOT NULL );" all t3 t2 t1)
      (record! 30 2 "CREATE TABLE address ( street VARCHAR(22) NOT NULL );" all t3 t2)
      (record! 32 3 "ALTER TABLE address ADD city VARCHAR(22) NOT NULL;" all t3)

      (println "t0 = " t0)
      (println "t1 = " t1)
      (println "t2 = " t2)
      (println "t3 = " t3)
      (println "all = " all)

      (are [pos entry t] (= (recover pos entry) t)
        ;; t0
        01 0 t0
        01 3 t0
        10 1 t0
        22 999999 t0
        23 0 t0
        ;; t1
        23 1 t1
        23 2 t1
        23 3 t1
        29 999 t1
        30 1 t1
        ;; t2
        30 2 t2
        30 3 t2
        32 2 t2
        ;; t3
        32 3 t3
        32 4 t3
        33 0 t3
        1033 4 t3
        ;; all
        33 0 all
        1033 4 all))))
