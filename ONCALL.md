# Prism LTS Oncall Docs

## What does this service do?
Prism long term storage consumes messages from [specified topics](prism-lts/values.yaml#L27) and stores them in [AWS S3](https://s3.console.aws.amazon.com/s3/buckets/prod-prism-lts/?region=us-west-2&tab=overview) partitioned by topic and metric date.

### Example
Messages written to the saas-usage-metrics topic with a metric date of 6-16-18 will end up in `s3://prod-prism-lts/topics/saas-usage-metrics/year=2018/month=06/day=16/saas-usage-metrics+<kafka-partition>+<kafka-offset>.avro` 

**NOTE** Kafka Message and .avro files in S3 are not 1 for 1. A .avro file in S3 may contain 1 - N messages up to 5MB of messages.

## What's the impact of the service being down?
  - No other services are impacted by an outage in Prism LTS.
  - The Kafka message retension policy allows for Prism-LTS to be down for ~7 days before it starts missing messages.

## Which customers need to be notified, and how should they be notified?
  - No customers need to be notified of a Prism LTS outage

## What should I do to troubleshoot an outage?
 - Check the Number of Active Tasks in [Grafana](https://grafana.commonstack.io/d/000000021/prism-long-term-storage?panelId=21&fullscreen&orgId=1&from=now-3h&to=now)
   - 12/12 indicates that Prism LTS is health
   - \< 12/12 indicates that tasks are dying
   - 0/12 indicates no messages are being written to S3
 - Check Prism-LTS Rest API for a Stack Trace
    - This is Much easier that searching Loggly; Sometimes errors are not logged
    - SSH into the Prod Jumpgate
    - Do a `kubetl port-forward <prism-lts pod> 8083:8083` to expose the Rest API
    - Check the Status of the Tasks with `curl 127.0.0.1:33250/connectors/s3-connector/status`
    - All 12 tasks should be in the `RUNNING` state
 - Check the logs to ensure Prism-LTS is writing and committing message offsets
    - example log message: `INFO Files committed to S3. Target commit offset for ac-user-event-2 is 1839407`


## How do I fix common outages?
Manually Restart the Failed Tasks:

  1. SSH into the Prod Jumpgate
  2. Do a `kubetl port-forward <prism-lts pod> 8083:8083` to expose the Rest API
  3. Check the Status of the Tasks with `curl 127.0.0.1:33250/connectors/s3-connector/status`
  4. Restart a failed Task with `curl -X POST 127.0.0.1:33250/connectors/s3-connector/tasks/<task-id/restart`

## Where is other alerting documentation located?
- https://cawiki.ca.com/display/SolEng/Alerts+and+Paging+rules+for+Prism-lts