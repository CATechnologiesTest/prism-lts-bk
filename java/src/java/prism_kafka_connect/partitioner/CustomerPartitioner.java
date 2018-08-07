/*
 *
 * The following class partitions an s3 bucket into fields.
 * Different topics have different partitioning schemes.
 *
 * For the saas-usage metrics and the health-metrics, the partitioning scheme is:
 * product_instance_id,product_id,metric_date
 *
 * For any site-based metrics, the partitioning scheme is:
 * customer_id,product_id,instance_id,metric_date
 *
 * For the ac-user-event, the temporary partitioning scheme is:
 * user_oid,product_id,metric_date
 *
 *
 *
 */
package prism_kafka_connect.partitioner;

import org.apache.kafka.common.config.ConfigException;
import org.joda.time.DateTimeZone;

import io.confluent.connect.storage.partitioner.TimestampExtractor;
import io.confluent.connect.storage.partitioner.*;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Schema.Type;

import java.util.Map;


import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.avro.generic.GenericRecord;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import io.confluent.connect.storage.common.SchemaGenerator;
import io.confluent.connect.storage.common.StorageCommonConfig;
import io.confluent.connect.storage.errors.PartitionException;


public class CustomerPartitioner<T> extends DefaultPartitioner<T> {
  protected static final Logger log = LoggerFactory.getLogger(FieldPartitioner.class);
  protected static final String healthMetricName = "com.sts.HealthMetric";
  protected static final String userEventName = "com.sts.user_event";
  protected List<String> fieldNamesSites;
  protected List<String> fieldNamesUserEvents;
  protected List<String> fieldNamesSaas;


  /*
   * Overriding DefaultPartitioner's configure method, ProductPartitioner's configure method
   * sets the global fields fieldNamesSites, fieldNamesSaas and fieldNamesUserEvents to the strings
   * that map to each of the respective topics.
   *
   * delim is set to delimiter, ","
   *
   * @param: Map<String, Object> config
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(Map<String, Object> config) {
    fieldNamesSites = new ArrayList<String>();
    fieldNamesSites.add("customer_id");
    fieldNamesSites.add("product_id");
    fieldNamesSites.add("instance_id");
    fieldNamesSites.add("metric_date");

    fieldNamesSaas = new ArrayList<String>();
    fieldNamesSaas.add("product_instance_id");
    fieldNamesSaas.add("product_id");
    fieldNamesSaas.add("metric_date");

    fieldNamesUserEvents = new ArrayList<String>();
    fieldNamesUserEvents.add("user_oid");
    fieldNamesUserEvents.add("product_id");
    fieldNamesUserEvents.add("metric_date");

    delim = (String) config.get(StorageCommonConfig.DIRECTORY_DELIM_CONFIG);
  }

  /*
   * Overriding DefaultPartitioner's encodePartition method, ProductPartitioner's encodePartition
   * first checks to see if sinkRecord.value() is a Struct.
   * If it is, a String list schemaFieldNames gets the list of field names corresponding with the schema
   * the sinkRecord has.
   * These fields are then traversed, and after finding the type of each field value, are casted to a string and appended
   * to a StringBuilder.
   * metric-date, a field that appears in every topic, is further divided into year month and day.
   *
   * @param: SinkRecord sinkRecord
   * @return: String
   * @throw: PartitionException
   *
   */
  @Override
  public String encodePartition(SinkRecord sinkRecord) {
        Object value = sinkRecord.value();
        List <String> schemaFieldNames;
        int count = 0;
        if (value instanceof Struct) {
            final Schema valueSchema = sinkRecord.valueSchema();
            final Struct struct = (Struct) value;

            StringBuilder builder = new StringBuilder();
            schemaFieldNames = whichFieldNames(struct,valueSchema);
            for (String fieldName : schemaFieldNames) {
                if (builder.length() > 0) {
                    builder.append(this.delim);
                }

                Object partitionKey = struct.get(fieldName);

                Type type = valueSchema.field(fieldName).schema().type();
                switch (type) {
                    case INT8:
                    case INT16:
                    case INT32:
                    case INT64:
                        Number record = (Number) partitionKey;
                        builder.append(fieldName + "=" + record.toString());
                        break;
                    case STRING:
                        String strRecord = (String) partitionKey;
                        if(fieldName.equals("metric_date")){
                            builder.append("year" + "=" + (strRecord.substring(0,4)))
                                .append("/")
                                .append("month" + "=" + (strRecord.substring(5,7)))
                                .append("/")
                                .append("day" + "=" + (strRecord.substring(8,10)));
                        }
                        else {
                            builder.append(fieldName + "=" + (String) partitionKey);
                        }

                        break;
                    case BOOLEAN:
                        boolean booleanRecord = (boolean) partitionKey;
                        builder.append(fieldName + "=" + Boolean.toString(booleanRecord));
                        break;
                    default:
                        log.error("Type {} is not supported as a partition key.", type.getName());
                        throw new PartitionException("Error encoding partition.");
                }
            }
            return builder.toString();
        } else {
            log.error("Value is not Struct type.");
            throw new PartitionException("Error encoding partition.");
        }
    }

    /*
     * Called by encodePartition when checking the valueSchema() of the sinkRecord.
     * whichFieldNames check the name of the valueSchema, and return the corresponding list of fields.
     *
     * @param: Struct value, Schema valueSchema
     * @return: List<String>
     * @throw: PartitionException
     *
     */
    public List<String> whichFieldNames(Struct value, Schema valueSchema) {
        if(valueSchema.name().equals(healthMetricName)){
            return fieldNamesSaas;
        }
        else if(valueSchema.name().equals(userEventName)) {
            return fieldNamesUserEvents;
        }
        else if(valueSchema.name().substring(8,12).toLowerCase().equals("site") && valueSchema.field("customer_id") != null && valueSchema.field("product_id") != null)
            return fieldNamesSites;
        else if(valueSchema.field("customer_id") != null && valueSchema.field("product_id") != null)
            return fieldNamesSites;
        else {
            log.error("topic name not recognized.");
            throw new PartitionException("Error encoding partition.");
        }

    }
}