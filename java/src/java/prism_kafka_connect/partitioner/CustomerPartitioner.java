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
 * For the ac-user-event, the partitioning scheme is:
 * user_oid,product_id,metric_date
 *
 * For other defaults, scheme is:
 * metric_date
 *
 *
 */
package prism_kafka_connect.partitioner;

import org.apache.kafka.common.config.ConfigException;
import org.joda.time.DateTimeZone;

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
  protected static final Logger log = LoggerFactory.getLogger(DefaultPartitioner.class);
  protected static final String healthMetricName = "com.sts.HealthMetric";
  protected static final String userEventName = "com.sts.user_event";
  protected static final String CUSTOMER_ID = "customer_id";
  protected static final String PRODUCT_ID = "product_id";
  protected static final String INSTANCE_ID = "instance_id";
  protected static final String PRODUCT_INSTANCE_ID = "product_instance_id";
  protected static final String USER_OID = "user_oid";
  protected static final String METRIC_DATE = "metric_date";

  protected List<String> fieldNamesSites;
  protected List<String> fieldNamesUserEvents;
  protected List<String> fieldNamesSaas;
  protected List<String> fieldNamesDefault;


  /*
   * Overriding DefaultPartitioner's configure method
   * initializes the global fields fieldNamesSites, fieldNamesSaas, fieldNamesUserEvents and fieldNamesDefault to the strings
   * that map to each of the respective topics.
   *
   * dierectory delim is set to delimiter, "/"
   *
   * @param: Map<String, Object> config
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(Map<String, Object> config) {
    fieldNamesSites = new ArrayList<String>();
    fieldNamesSites.add(CUSTOMER_ID);
    fieldNamesSites.add(PRODUCT_ID);
    fieldNamesSites.add(INSTANCE_ID);
    fieldNamesSites.add(METRIC_DATE);

    fieldNamesSaas = new ArrayList<String>();
    fieldNamesSaas.add(PRODUCT_INSTANCE_ID);
    fieldNamesSaas.add(PRODUCT_ID);
    fieldNamesSaas.add(METRIC_DATE);

    fieldNamesUserEvents = new ArrayList<String>();
    fieldNamesUserEvents.add(USER_OID);
    fieldNamesUserEvents.add(PRODUCT_ID);
    fieldNamesUserEvents.add(METRIC_DATE);

    fieldNamesDefault = new ArrayList<String>();
    fieldNamesDefault.add(METRIC_DATE);

    delim = (String) config.get(StorageCommonConfig.DIRECTORY_DELIM_CONFIG);
  }

  /*
   * Overriding DefaultPartitioner's encodePartition method
   * first checks to see if sinkRecord.value() is a Struct.
   * If it is, a String list schemaFieldNames gets the list of field names corresponding with the schema the sinkRecord has.
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
                        if(fieldName.equals(METRIC_DATE)){
                            builder.append("year" + "=" + (strRecord.substring(0,4)))
                                .append(delim)
                                .append("month" + "=" + (strRecord.substring(5,7)))
                                .append(delim)
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
     *
     */
    public List<String> whichFieldNames(Struct value, Schema valueSchema) {
        if(valueSchema.name().equals(healthMetricName)){
            return fieldNamesSaas;
        }
        else if(valueSchema.name().equals(userEventName)) {
            return fieldNamesUserEvents;
        } else if(valueSchema.field(CUSTOMER_ID)!=null && valueSchema.field(PRODUCT_ID)!=null && valueSchema.field(INSTANCE_ID)!=null) {
            return fieldNamesSites;
        } else {
            return fieldNamesDefault;
        }

    }
}