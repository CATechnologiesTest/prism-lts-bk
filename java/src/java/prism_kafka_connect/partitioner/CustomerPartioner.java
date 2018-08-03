/*package prism_kafka_connect.partitioner;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import io.confluent.connect.storage.common.SchemaGenerator;
import io.confluent.connect.storage.common.StorageCommonConfig;
import io.confluent.connect.storage.errors.PartitionException;

import io.confluent.connect.storage.partitioner.*;

public class CustomerPartitioner<T> extends FieldPartitioner<T> {
    //private static final Logger log = LoggerFactory.getLogger(FieldPartitioner.class);
    private List<String> fieldNames;


    //@SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, Object> config) {
        fieldNames = (List<String>) config.get(PartitionerConfig.PARTITION_FIELD_NAME_CONFIG);
        delim = (String) config.get(StorageCommonConfig.DIRECTORY_DELIM_CONFIG);
    }

    @Override
    public String encodePartition(SinkRecord sinkRecord) {
        Object value = sinkRecord.value();
        if (value instanceof Struct) {
            final Schema valueSchema = sinkRecord.valueSchema();
            final Struct struct = (Struct) value;

            StringBuilder builder = new StringBuilder();
            for (String fieldName : fieldNames) {
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
                        builder.append(fieldName + "=" + (String) partitionKey);
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

    @Override
    public List<T> partitionFields() {
        if (partitionFields == null) {
            partitionFields = newSchemaGenerator(config).newPartitionFields(
                    Utils.join(fieldNames, ",")
            );
        }
        return partitionFields;
    }
}
*/