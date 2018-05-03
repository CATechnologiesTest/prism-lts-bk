/*
 * Copyright 2017 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package prism_kafka_connect.partitioner;

import org.apache.avro.generic.GenericRecord;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import io.confluent.connect.storage.common.SchemaGenerator;
import io.confluent.connect.storage.common.StorageCommonConfig;
import io.confluent.connect.storage.errors.PartitionException;

import io.confluent.connect.storage.common.StorageCommonConfig;
import io.confluent.connect.storage.partitioner.*;

public class SchemaBasedPartitioner<T> extends DefaultPartitioner<T> {
  // Duration of a partition in milliseconds.
  private static final String SCHEMA_GENERATOR_CLASS = "io.confluent.connect.storage.hive.schema.DefaultSchemaGenerator";
  private static final Logger log = LoggerFactory.getLogger(SchemaBasedPartitioner.class);

  protected void init(
      Map<String, Object> config
  ) {
    delim = (String) config.get(StorageCommonConfig.DIRECTORY_DELIM_CONFIG);
    try {
      partitionFields = newSchemaGenerator(config).newPartitionFields("schema,version");
    } catch (IllegalArgumentException e) {
      ConfigException ce = new ConfigException(
          PartitionerConfig.PATH_FORMAT_CONFIG,
          "",
          e.getMessage()
      );
      ce.initCause(e);
      throw ce;
    }
  }

  @Override
  public void configure(Map<String, Object> config) {
    init(config);
  }

  @Override
  public String encodePartition(SinkRecord sinkRecord) {
    String schemaBasedPrefix = getSchemaPrefix(sinkRecord).replace('_', '-');
    return schemaBasedPrefix;
  }

  @Override
  public String generatePartitionedPath(String topic, String encodedPartition) {
    return encodedPartition;
  }

  @Override
  @SuppressWarnings("unchecked")
  public SchemaGenerator<T> newSchemaGenerator(Map<String, Object> config) {
      Class<? extends SchemaGenerator<T>> generatorClass = null;
      try {
          generatorClass = getSchemaGeneratorClass();
          return generatorClass.newInstance();
      } catch (ClassCastException
               | ClassNotFoundException
               | IllegalAccessException
               | InstantiationException e) {
          ConfigException ce = new ConfigException("Invalid generator class: " + generatorClass);
          ce.initCause(e);
          throw ce;
      }
  }

  protected Class<? extends SchemaGenerator<T>> getSchemaGeneratorClass()
      throws ClassNotFoundException {
      return (Class<? extends SchemaGenerator<T>>) Class.forName(SCHEMA_GENERATOR_CLASS);
  }

  private String getSchemaPrefix(SinkRecord sinkRecord) {
      Schema schema;
      try {
          schema = sinkRecord.valueSchema();
          log.info("schema name: " + schema.name());
          log.info("schema version: " + schema.version().toString());
          return "schema=" + schema.name() + "/" + "version=" + schema.version().toString();
      } catch(Exception e){
          log.error("Schema Error. No name or version found for schema.");
          throw e;
      }
  }
}
