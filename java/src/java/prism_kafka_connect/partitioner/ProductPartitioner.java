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

import io.confluent.connect.storage.common.SchemaGenerator;
import io.confluent.connect.storage.common.StorageCommonConfig;
import io.confluent.connect.storage.errors.PartitionException;

//customerid/productid/productinstanceid/metric-date
public class ProductPartitioner<T> extends DefaultPartitioner<T> {
  protected static final Logger log = LoggerFactory.getLogger(FieldPartitioner.class);
  //for saas and health
  protected List<String> fieldNamesSites;//create get method
    //for site based
    protected List<String> fieldNamesSaas;//create get method
    protected List<String> fieldNamesUserEvents;

  @SuppressWarnings("unchecked")
  @Override
  public void configure(Map<String, Object> config) {
    fieldNamesSites = (List<String>) config.get("partition.sites.field.name");
    fieldNamesSaas = (List<String>) config.get("partition.saas.field.name");
    fieldNamesUserEvents = (List<String>) config.get("partition.userevent.field.name");
    delim = (String) config.get(StorageCommonConfig.DIRECTORY_DELIM_CONFIG);
  }
//method to check if field has anothername in the schema
 /* public String checkForAlias(String fieldName, Struct value) {
        if(fieldName.equals("customer_id")) {//could either be user_id or customer_id
            if(value.get("customer_id")!=null)
                return "customer_id";
            else if(value.get("user_id")!=null)
                return "user_id";
            else if(value.get("user_oid")!=null)
                return "user_oid";
            else {
                return null;

            }
        }
        if(fieldName.equals("product_instance_id")) {//must be instance_id
            if(value.get("product_instance_id") != null)
                return "product_instance_id";
            else if(value.get("instance_id") != null)
                return "instance_id";
            else
                return null;
        }
        else
            return null;

  }
*/

  //method to partition metric_date

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
                /*if(partitionKey == null) {
                    fieldName = checkForAlias(fieldName,struct);
                    partitionKey = struct.get(fieldName);

                }*/
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

    public List<String> whichFieldNames(Struct value, Schema valueSchema) {
        if(valueSchema.name().equals("saas-usage-metrics")||valueSchema.name().equals("health-metrics")){
            return fieldNamesSaas;
        }
        else if(valueSchema.name().equals("ac-user-event")) {
            return fieldNamesUserEvents;
        }
        else
            return fieldNamesSites;

    }
}