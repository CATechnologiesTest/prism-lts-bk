package prism_kafka_connect;

import io.confluent.connect.storage.partitioner.TimestampExtractor;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Schema;

import java.util.Map;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class PrismTimestampExtractor implements TimestampExtractor {

    private static final Logger LOGGER = Logger.getLogger(PrismTimestampExtractor.class.getName());

    DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);
    DateTimeFormatter sampleDateFmt = DateTimeFormat.forPattern("YYYY-MM-dd").withZone(DateTimeZone.UTC);
    protected DateTimeFormatter formatter;

    @Override
    public void configure(Map<String, Object> config) {}
    @Override
    public Long extract(ConnectRecord<?> record) {
        Long retVal;
        try {

            Struct extractedRecord = (Struct)record.value();
            if(extractedRecord == null) {
                throw new java.lang.NullPointerException();
            }
            return new Long(extractFromStruct(extractedRecord).getMillis());
        } catch (Exception e) {
            retVal = System.currentTimeMillis();
            LOGGER.warning("Timestamp extraction on topic " + record.topic() + " failed with exception " + e  + " , defaulting to system time for record " + ((Struct) record.value()));
        }
        return retVal;
    }

    private DateTime extractFromStruct(Struct record) {
        Schema schema = record.schema();
        if (schema.field("metric_date") != null) {
            return sampleDateFmt.parseDateTime(record.get("metric_date").toString());
        } else if (schema.field("objectChangeMetadata") != null) {
            return fmt.parseDateTime(((Struct)record.get("objectChangeMetadata")).get("timestamp").toString());
        } else if (schema.field("sampleDate") != null) {
            Object sampleDate = record.get("sampleDate");
            return sampleDateFmt.parseDateTime(sampleDate.toString());
        } else if (schema.field("event")  != null) {
            return fmt.parseDateTime(((Struct)record.get("event")).get("timestamp").toString());
        } else if (schema.field("timestamp") != null) {
            //need to check if date is string or long in this case because of ocm messages
            if (record.get("timestamp") instanceof Long){
                return fmt.parseDateTime(new DateTime(record.get("timestamp")).toString());
            }
            return fmt.parseDateTime(record.get("timestamp").toString());
        } else if (schema.field("date") != null) {
            return fmt.parseDateTime(record.get("date").toString());
        } else {
            return null;
        }
    }
}
