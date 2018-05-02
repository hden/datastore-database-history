(ns datastore-database-history.core
  (:require [clojure.core.async :as async])
  (:import [com.google.cloud.datastore DatastoreOptions Entity Query]
           [io.debezium.document DocumentReader DocumentWriter]
           [io.debezium.relational.history HistoryRecord AbstractDatabaseHistory])
  (:gen-class
   :name datastore-database-history.core.DatastoreDatabaseHistory
   :extends io.debezium.relational.history.AbstractDatabaseHistory
   :exposes {config {:get getConfig :set setConfig}}
   :state queue
   :init init))

;; ----- datastore -----
(def ^:private datastore (.getService (DatastoreOptions/getDefaultInstance)))
(def ^:const value-key "value")

(defn ^:private create-key [kind]
  (-> (.newKeyFactory datastore)
      (.setKind kind)
      (.newKey)))

(defn ^:private create-entity [key value]
  (let [builder (Entity/newBuilder key)]
    (.set builder value-key value)
    (.build builder)))

(defn ^:private append! [kind value]
  (let [key (create-key kind)
        entity (create-entity key value)]
    (.add datastore entity)))

(defn ^:private get-all [kind]
  (let [query (-> (Query/newEntityQueryBuilder)
                  (.setKind kind)
                  (.build))]
    (into []
          (map #(.getString % value-key))
          (iterator-seq (.run datastore query)))))

;; ----- debezium -----
(def ^:private reader (DocumentReader/defaultReader))
(def ^:private writer (DocumentWriter/defaultWriter))

(defn -init []
  [[] (async/chan)])

(defn -start [this]
  (let [queue (.queue this)
        config (.getConfig this)
        kind (.getString config "database.history.datastore.kind" "history")]
    (async/go-loop []
      (when-let [{:keys [promise value]} (async/<! queue)]
        (async/>! promise (append! kind value))
        (recur)))))

(defn -stop [this]
  (async/close! (.queue this)))

(defn -storeRecord [this record]
  (let [queue (.queue this)
        promise (async/promise-chan)]
    (->> (.document record)
         (.write writer)
         (assoc {:promise promise} :value)
         (async/put! queue))
    (async/<!! promise)))

(defn -recoverRecords [this consumer]
  (let [config (.getConfig this)
        kind (.getString config "database.history.datastore.kind" "history")]
    (doseq [string (get-all kind)]
      (->> (.read reader string)
           (new HistoryRecord)
           (.accept consumer)))))

(defn -exists [this]
  (let [config (.getConfig this)
        kind (.getString config "database.history.datastore.kind" "history")
        query (-> (Query/newKeyQueryBuilder)
                  (.setKind kind)
                  (.setLimit 1)
                  (.build))
        cursor (.run datastore query)]
    (.hasNext cursor)))
