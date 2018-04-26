(defproject com.sts/prism-long-term-storage "0.1.0"
  :description "A service called prism-long-term-storage"
  :url "https://github.com/sts-atlas/prism-long-term-storage"
  :license "Copyright 2016 CA Technologies"
  :repositories [["rally" "http://repo-depot.f4tech.com/artifactory/rally-maven"]
                 ["confluent" "http://packages.confluent.io/maven"]]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.apache.kafka/connect-api "1.0.0" :exclusions [org.slf4j/slf4j-log4j12]]
                 [io.confluent/kafka-connect-s3 "4.0.0"]]

  :java-source-paths ["src/java/prism_kafka_connect"]
  :jar-name "prism-kafka-connect.jar"
  :uberjar-name "prism-kafka-connect-standalone.jar"
  :main prism-kafka-connect.core)
