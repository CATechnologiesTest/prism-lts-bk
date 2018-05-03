FROM clojure:lein-2.7.1 as builder
WORKDIR /app
COPY project.clj ./
COPY src ./src/
RUN lein uberjar

FROM confluentinc/cp-kafka-connect:4.0.0
ENV KAFKA_CONNECT_JAR_NAME="prism-kafka-connect-standalone.jar"
COPY --from=builder /app/target/$KAFKA_CONNECT_JAR_NAME /usr/share/java
