package io.conduktor.queues;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaShareConsumer;
import org.apache.kafka.clients.consumer.AcknowledgeType;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class ExplicitAcknowledgementShareConsumer {

    // Counter for demonstration purposes
    private static final AtomicInteger processed = new AtomicInteger(0);
    private static final AtomicInteger failures = new AtomicInteger(0);

    public static void main(String[] args) {
        // Configure the consumer
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "explicit-ack-share-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("share.acknowledgement.mode", "explicit"); // For Kafka 4.1+

        // Create a share consumer
        KafkaShareConsumer<String, String> consumer = new KafkaShareConsumer<>(props);

        // Subscribe to the topic
        consumer.subscribe(Collections.singletonList("orders-topic"));

        try {
            // Consumption loop with explicit acknowledgement
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        // Process the record
                        System.out.printf("Processing: Partition = %d, Offset = %d, Value = %s%n",
                                record.partition(), record.offset(), record.value());

                        // Simulate business logic
                        boolean success = processOrderRecord(record);

                        if (success) {
                            // Explicitly acknowledge successful processing
                            consumer.acknowledge(record, AcknowledgeType.ACCEPT);
                            processed.incrementAndGet();
                            System.out.println("Successfully processed order: " + record.value());
                        } else {
                            // Temporary failure, release for retry
                            consumer.acknowledge(record, AcknowledgeType.RELEASE);
                            System.out.println("Temporarily failed to process order, releasing for retry: " + record.value());
                        }
                    } catch (FatalProcessingException e) {
                        // Permanent failure, reject the record
                        consumer.acknowledge(record, AcknowledgeType.REJECT);
                        failures.incrementAndGet();
                        System.err.println("Permanently failed to process order: " + record.value() +
                                " - Reason: " + e.getMessage());
                    } catch (Exception e) {
                        // Unexpected exception, release for retry
                        consumer.acknowledge(record, AcknowledgeType.RELEASE);
                        System.err.println("Unexpected error while processing order: " + e.getMessage());
                    }
                }

                // Commit acknowledgements to the broker
                consumer.commitSync();

                // Print statistics every 100 records
                int total = processed.get();
                if (total > 0 && total % 100 == 0) {
                    System.out.printf("Progress: Processed %d orders, Failed %d orders%n",
                            processed.get(), failures.get());
                }
            }
        } catch (Exception e) {
            System.err.println("Consumer error: " + e.getMessage());
        } finally {
            consumer.close();
            System.out.printf("Final count: Processed %d orders, Failed %d orders%n",
                    processed.get(), failures.get());
        }
    }

    private static boolean processOrderRecord(ConsumerRecord<String, String> record) throws FatalProcessingException {
        String orderValue = record.value();

        // Simulate processing with occasional failures
        try {
            // Simulate processing time
            Thread.sleep((long) (Math.random() * 200));

            // Simulate occasional temporary failures (25% chance)
            if (Math.random() < 0.25) {
                return false;
            }

            // Simulate occasional permanent failures (5% chance)
            if (Math.random() < 0.05) {
                throw new FatalProcessingException("Invalid order format");
            }

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // Custom exception for permanent failures
    static class FatalProcessingException extends Exception {
        public FatalProcessingException(String message) {
            super(message);
        }
    }
}