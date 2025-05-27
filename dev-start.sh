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
  --replication-factor 3 \
  --partitions 3 \
  --topic my-topic


kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 3 \
  --partitions 3 \
  --topic high-volume-topic

kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 3 \
  --partitions 3 \
  --topic orders-topic

kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 3 \
  --partitions 3 \
  --topic tasks-topic

echo "Producing messages to my-topic..."
kafka-producer-perf-test --producer-props bootstrap.servers=localhost:9092 retries=5 --record-size 10 --throughput 1  --num-records 10 --topic my-topic

echo "Consuming messages from my-topic..."
kafka-console-share-consumer \
--bootstrap-server localhost:9092 \
--topic my-topic \
--timeout-ms 3000


echo "Listing groups from kafka..."
kafka-groups --bootstrap-server localhost:9092 --list
kafka-share-groups --bootstrap-server localhost:9092 --list

