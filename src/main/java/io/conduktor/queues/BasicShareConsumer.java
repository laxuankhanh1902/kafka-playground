package io.conduktor.queues;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaShareConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class BasicShareConsumer {
    public static void main(String[] args) {
        // Configure the consumer
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "my-share-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        // Create a KafkaShareConsumer instead of KafkaConsumer
        KafkaShareConsumer<String, String> consumer = new KafkaShareConsumer<>(props);

        // Subscribe to the topic
        consumer.subscribe(Collections.singletonList("my-topic"));

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
}
