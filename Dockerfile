FROM clojure:lein-2.7.1 as builder

COPY java /java
WORKDIR /java
RUN lein deps
RUN lein test
RUN lein jar

FROM confluentinc/cp-kafka-connect:3.3.1
COPY --from=builder /java/target/prism-kafka-connect.jar /usr/share/java/kafka-connect-s3/
