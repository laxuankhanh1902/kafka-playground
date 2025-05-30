package io.conduktor.queues;

import org.apache.kafka.clients.consumer.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

public class BasicShareConsumer1 {
    public static void main(String[] args) {
        // Configure the consumer
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:6969");
        props.put("group.id", "my-share-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("share.group." + ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Enable unstable APIs to access newer features like the queue protocol
        props.put("unstable.api.versions.enable", "true");
        // KIP-932 configuration for Kafka 4.0+
        // Set the GROUP_PROTOCOL_CONFIG to CONSUMER for queue semantics
        props.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "CONSUMER");
        // Process fewer records at a time for better load balancing
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5");
        // Use shorter poll intervals
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "5000");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        // Create a KafkaShareConsumer instead of KafkaConsumer
        KafkaShareConsumer<String, String> consumer = new KafkaShareConsumer<>(props);

        // Subscribe to the topic
        consumer.subscribe(Collections.singletonList("high-volume-topic"));

        try {
            // Basic consumption loop with implicit acknowledgement
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    // Process the record
                    System.out.printf("Partition = %d, Offset = %d, Key = %s, Value = %s%n",
                            record.partition(), record.offset(), record.key(), record.value());

                    // Perform business logic on the record
                    processRecord(record);
                }

                // With implicit acknowledgement, all records are automatically
                // accepted when the next poll occurs

                // Sleep briefly to avoid tight polling loops in this example
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            System.out.println("Consumer interrupted");
        } finally {
            // Close the consumer to leave the group cleanly
            consumer.close();
        }
    }

    private static void processRecord(ConsumerRecord<String, String> record) {
        // Simulate processing delay
        try {
            long processingTime = (long) (Math.random() * 50);
            Thread.sleep(processingTime);
            System.out.println("Processed record with value: " + record.value() +
                    " (took " + processingTime + "ms)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static ShareConsumer<String, String> createConsumer() throws IOException {
        Properties propOverrides = new Properties();
        propOverrides.put(ConsumerConfig.GROUP_ID_CONFIG, "share-consumer-group-frank");

        // TODO: Can I read from the VERY Beginning of time for a topic?
        propOverrides.put("share.group." + ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Enable unstable APIs to access newer features like the queue protocol
        propOverrides.put("unstable.api.versions.enable", "true");
        // KIP-932 configuration for Kafka 4.0+
        // Set the GROUP_PROTOCOL_CONFIG to CONSUMER for queue semantics
        propOverrides.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "CONSUMER");
        // Process fewer records at a time for better load balancing
        propOverrides.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5");
        // Use shorter poll intervals
        propOverrides.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "5000");
        propOverrides.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        return new KafkaShareConsumer<>(propOverrides);
    }
}
