package io.conduktor.kafkastreams;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class KafkaStreamsExample {
    public static void main(String[] args) {
        // Configure and start the streams application
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-app-" + UUID.randomUUID());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:6969");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

// Required for producer
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

// Required for consumer
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("group.id", "test-group-" + UUID.randomUUID());
        props.put(StreamsConfig.ENABLE_METRICS_PUSH_CONFIG, true);



        var builder = new StreamsBuilder();
        KStream<String, String> input = builder.stream("input-topic");

        KStream<String, String> uppercased = input.mapValues(value -> value.toUpperCase());

        uppercased.to("output-topic");

        Topology topology = builder.build();

        var streams = new KafkaStreams(topology, props);
        var latch = new CountDownLatch(1);
        streams.setStateListener((newState, oldState) -> {
            if (newState == KafkaStreams.State.RUNNING) {
                latch.countDown();
            }
        });
        streams.start();

        var a = AdminClient.create(props);
        a.describeShareGroups(List.of("")).all().whenComplete((result, exception) -> {
            if (exception != null) {
                System.err.println("Error describing share groups: " + exception.getMessage());
            } else {
                result.forEach((groupId, description) -> {
                    System.out.println("Share group: " + groupId);
                    description.members().forEach(member -> {
                        System.out.println("  Member: " + member.consumerId() + ", host: " + member.host());
                    });
                });
            }
        });

        var metricsList = streams.metrics().entrySet().stream()
                .filter(entry -> entry.getKey().group().equals("stream-metrics"))
                .toList();

        //Produce some data to the input topic
        try (var producer = new org.apache.kafka.clients.producer.KafkaProducer<String, String>(props)) {
            metricsList.forEach(metric -> {
                producer.registerMetricForSubscription((KafkaMetric) metric.getValue());
            });
            for (int i = 0; i < 10; i++) {
                producer.send(new org.apache.kafka.clients.producer.ProducerRecord<>("input-topic", "key-" + i, "value-" + i));
            }
        }

        //consume the data from the output topic
        try (var consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<String, String>(props)) {
            consumer.subscribe(List.of("output-topic"));
            var records = consumer.poll(Duration.ofSeconds(10));
            records.forEach(record -> System.out.println("Consumed record: " + record.value()));
        }

//        var metrics = streams.metrics();
//        System.out.println("Metrics: " + metrics);

//        var admin = Admin.create(props);
//        var x = admin.metrics();
//        x.forEach((k, v) -> {;
//            System.out.println("Admin metric: " + k.name() + " = " + v.metricValue());
//        });
    }
}