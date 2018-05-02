(defproject datastore-database-history "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.4.474"]
                 [io.debezium/debezium-core "0.7.5"]
                 [com.google.cloud/google-cloud-datastore "1.28.0" :exclusions [io.grpc/grpc-core io.netty/netty-codec-http2]]]
  :profiles
  {:uberjar {:aot :all}
   :dev {:aot :all
         :dependencies [[org.apache.kafka/connect-api "1.1.0"]]}})
