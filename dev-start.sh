docker compose up --detach --wait

#kafka-topics --create \
#  --bootstrap-server localhost:9092 \
#  --replication-factor 3 \
#  --partitions 3 \
#  --topic input-topic
#
#kafka-topics --create \
#  --bootstrap-server localhost:9092 \
#  --replication-factor 3 \
#  --partitions 3 \
#  --topic output-topic

#kafka-client-metrics --bootstrap-server localhost:9092 \
#   --alter \
#   --name 'basic_metrics'\
#   --metrics * \
#   --interval 500

#kafka-producer-perf-test --producer-props bootstrap.servers=localhost:9092 retries=5 --record-size 10 --throughput 1  --num-records 10 --topic input-topic
#
#kafka-console-producer \
#  --bootstrap-server localhost:9092 \
#  --topic input-topic
#
#kafka-console-consumer \
#  --bootstrap-server localhost:9092 \
#  --topic input-topic \
#  --from-beginning --property print.key=true

echo "Creating topics..."
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic my-topic


kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic high-volume-topic

kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic orders-topic

kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic tasks-topic

#echo "Producing messages to my-topic..."
#kafka-producer-perf-test --producer-props bootstrap.servers=localhost:9092 retries=5 --record-size 10 --throughput 1  --num-records 10 --topic my-topic
#
#echo "Consuming messages from my-topic... using kafka-console-consumer"
#kafka-console-consumer \
#--bootstrap-server localhost:9092 \
#--topic my-topic \
#--from-beginning \
#--timeout-ms 3000
#
#echo "Consuming messages from my-topic... using kafka-console-share-consumer"
#kafka-console-share-consumer \
#--bootstrap-server localhost:9092 \
#--topic my-topic \
#--timeout-ms 3000
#
#
#echo "Listing groups from kafka..."
#kafka-groups --bootstrap-server localhost:9092 --list
#kafka-share-groups --bootstrap-server localhost:9092 --list

echo "Setting up Kafka Queues Monitor interceptor..."
curl 'http://localhost:8888/gateway/v2/interceptor' \
  -X 'PUT' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Basic YWRtaW46Y29uZHVrdG9y' \
  -H 'Origin: http://localhost:8080' \
  -H 'Referer: http://localhost:8080/' \
  --data-raw '{
    "apiVersion": "gateway/v2",
    "kind": "Interceptor",
    "metadata": {
      "name": "kafka-queues-monitor",
      "scope": {}
    },
    "spec": {
      "pluginClass": "io.conduktor.gateway.interceptor.KafkaQueuesMonitorPlugin",
      "priority": 100,
      "config": {

      }
    }
  }'

