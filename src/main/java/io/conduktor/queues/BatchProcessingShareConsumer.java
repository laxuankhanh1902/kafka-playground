package io.conduktor.queues;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaShareConsumer;
import org.apache.kafka.clients.consumer.AcknowledgeType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BatchProcessingShareConsumer {

    // Number of worker threads for parallel processing
    private static final int WORKER_THREADS = 4;

    public static void main(String[] args) {
        // Configure the consumer
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "batch-share-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("share.max.delivery.attempts", "3");  // Customize retry attempts
        props.put("share.acquisition.lock.duration.ms", "60000");  // 60-second processing window
        props.put("share.acknowledgement.mode", "explicit");

        // Create a thread pool for parallel processing
        ExecutorService executor = Executors.newFixedThreadPool(WORKER_THREADS);

        // Create a share consumer
        KafkaShareConsumer<String, String> consumer = new KafkaShareConsumer<>(props);

        // Subscribe to multiple topics
        consumer.subscribe(Collections.singletonList("high-volume-topic"));

        try {
            // Track metrics
            long startTime = System.currentTimeMillis();
            long recordsProcessed = 0;

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

                if (records.count() > 0) {
                    System.out.printf("Received batch of %d records%n", records.count());

                    // Process records in parallel
                    List<Future<RecordResult>> futures = new ArrayList<>();

                    for (ConsumerRecord<String, String> record : records) {
                        // Submit each record for processing
                        futures.add(executor.submit(() -> processRecord(record)));
                    }

                    // Wait for all processing to complete and handle results
                    for (int i = 0; i < futures.size(); i++) {
                        Future<RecordResult> future = futures.get(i);
                        try {
                            RecordResult result = future.get(55, TimeUnit.SECONDS); // Leave 5s margin
                            ConsumerRecord<String, String> record = result.getRecord();

                            // Acknowledge based on processing result
                            switch (result.getStatus()) {
                                case SUCCESS:
                                    consumer.acknowledge(record, AcknowledgeType.ACCEPT);
                                    recordsProcessed++;
                                    break;
                                case TEMPORARY_FAILURE:
                                    consumer.acknowledge(record, AcknowledgeType.RELEASE);
                                    System.out.println("Released record for retry: " + record.key());
                                    break;
                                case PERMANENT_FAILURE:
                                    consumer.acknowledge(record, AcknowledgeType.REJECT);
                                    System.err.println("Rejected record: " + record.key());
                                    break;
                            }
                        } catch (Exception e) {
                            // Timed out or execution exception
                            ConsumerRecord<String, String> record = records.iterator().next(); // Approximation
                            consumer.acknowledge(record, AcknowledgeType.RELEASE);
                            System.err.println("Failed to process record: " + e.getMessage());
                        }
                    }

                    // Commit all acknowledgements
                    consumer.commitSync();

                    // Log throughput statistics every 10000 records
                    if (recordsProcessed % 10000 == 0) {
                        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
                        double recordsPerSecond = elapsedSeconds > 0 ?
                                (double) recordsProcessed / elapsedSeconds : 0;
                        System.out.printf("Throughput: %.2f records/second (%d total)%n",
                                recordsPerSecond, recordsProcessed);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Consumer error: " + e.getMessage());
        } finally {
            consumer.close();
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                    System.err.println("Executor did not terminate in the specified time.");
                    List<Runnable> droppedTasks = executor.shutdownNow();
                    System.err.println("Executor was abruptly shut down. " +
                            droppedTasks.size() + " tasks will not be executed.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static RecordResult processRecord(ConsumerRecord<String, String> record) {
        try {
            // Simulate complex processing that takes variable time
            long processingTime = (long) (Math.random() * 500);
            Thread.sleep(processingTime);

            // Simulate occasional failures
            double random = Math.random();
            if (random < 0.05) {
                return new RecordResult(record, ProcessingStatus.PERMANENT_FAILURE);
            } else if (random < 0.15) {
                return new RecordResult(record, ProcessingStatus.TEMPORARY_FAILURE);
            }

            return new RecordResult(record, ProcessingStatus.SUCCESS);
        } catch (Exception e) {
            return new RecordResult(record, ProcessingStatus.TEMPORARY_FAILURE);
        }
    }

    // Helper class to track processing results
    static class RecordResult {
        private final ConsumerRecord<String, String> record;
        private final ProcessingStatus status;

        public RecordResult(ConsumerRecord<String, String> record, ProcessingStatus status) {
            this.record = record;
            this.status = status;
        }

        public ConsumerRecord<String, String> getRecord() {
            return record;
        }

        public ProcessingStatus getStatus() {
            return status;
        }
    }

    // Possible processing outcomes
    enum ProcessingStatus {
        SUCCESS,
        TEMPORARY_FAILURE,
        PERMANENT_FAILURE
    }
}