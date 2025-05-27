package io.conduktor.queues;

import org.apache.kafka.clients.consumer.AcknowledgeType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaShareConsumer;
import org.apache.kafka.clients.consumer.AcknowledgeType;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorkQueueShareConsumer {

    // Flag for graceful shutdown
    private static final AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) {
        // Consumer ID for logging
        final String consumerId = UUID.randomUUID().toString().substring(0, 8);

        // Configure the consumer
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", "localhost:9092");
        consumerProps.put("group.id", "work-queue-share-group");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("share.acknowledgement.mode", "explicit");
        consumerProps.put("share.acquisition.lock.duration.ms", "300000"); // 5 minutes for long tasks

        // Configure the producer for results
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", "localhost:9092");
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // Create consumer and producer
        KafkaShareConsumer<String, String> consumer = new KafkaShareConsumer<>(consumerProps);
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down consumer " + consumerId + "...");
            running.set(false);
        }));

        // Subscribe to the tasks topic
        consumer.subscribe(Collections.singletonList("tasks-topic"));

        System.out.println("Worker " + consumerId + " started and waiting for tasks...");

        try {
            // Work queue consumption pattern - process one message at a time
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                // Process at most one record per poll
                if (!records.isEmpty()) {
                    ConsumerRecord<String, String> task = records.iterator().next();

                    System.out.println("Worker " + consumerId + " received task: " + task.key());

                    try {
                        // Process the task - this could take significant time
                        String result = processLongRunningTask(task);

                        // Send result to results topic
                        producer.send(new ProducerRecord<>("tasks-results", task.key(), result));

                        // Acknowledge successful processing
                        consumer.acknowledge(task, AcknowledgeType.ACCEPT);
                        System.out.println("Worker " + consumerId + " completed task: " + task.key());
                    } catch (Exception e) {
                        System.err.println("Worker " + consumerId + " failed on task: " + task.key() +
                                " - " + e.getMessage());

                        // Release for retry if it's a recoverable error
                        if (e instanceof RecoverableTaskException) {
                            consumer.acknowledge(task, AcknowledgeType.RELEASE);
                            System.out.println("Task released for retry");
                        } else {
                            // Reject permanently failed tasks
                            consumer.acknowledge(task, AcknowledgeType.REJECT);
                            System.out.println("Task rejected permanently");

                            // Send failure notification
                            producer.send(new ProducerRecord<>("tasks-failures",
                                    task.key(), "Failed: " + e.getMessage()));
                        }
                    }

                    // Commit after processing each task
                    consumer.commitSync();
                }
            }
        } catch (Exception e) {
            System.err.println("Consumer error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            consumer.close();
            producer.close();
            System.out.println("Worker " + consumerId + " shut down");
        }
    }

    private static String processLongRunningTask(ConsumerRecord<String, String> task) throws Exception {
        System.out.println("Processing task: " + task.key());

        // Simulate a long-running process
        try {
            // Simulate variable processing time (20 seconds to 2 minutes)
            long processingTime = 20000 + (long)(Math.random() * 100000);
            System.out.println("Task will take approximately " + (processingTime / 1000) + " seconds");

            // Simulate work with progress updates
            long startTime = System.currentTimeMillis();
            int steps = 10;
            for (int i = 1; i <= steps; i++) {
                // Sleep for a portion of the total time
                Thread.sleep(processingTime / steps);

                // Report progress
                System.out.printf("Task %s: %d%% complete%n", task.key(), i * 100 / steps);

                // Simulate occasional recoverable errors
                if (Math.random() < 0.05) {
                    throw new RecoverableTaskException("Temporary resource unavailable");
                }

                // Simulate occasional permanent failures
                if (Math.random() < 0.02) {
                    throw new PermanentTaskException("Invalid task parameters");
                }
            }

            long actualTime = System.currentTimeMillis() - startTime;
            return "Task completed successfully in " + (actualTime / 1000) + " seconds";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RecoverableTaskException("Task was interrupted");
        }
    }

    // Custom exceptions for different error types
    static class RecoverableTaskException extends Exception {
        public RecoverableTaskException(String message) {
            super(message);
        }
    }

    static class PermanentTaskException extends Exception {
        public PermanentTaskException(String message) {
            super(message);
        }
    }
}