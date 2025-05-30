package io.conduktor.queues;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.MessageFormatter;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.tools.consumer.group.share.ShareGroupStateMessageFormatter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static java.util.Collections.emptyMap;

public class StateShareConsumer {
    public static void main(String[] args) {
        // Configure the consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "share-group-state-reader-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList("__share_group_state"));

            MessageFormatter formatter = new ShareGroupStateMessageFormatter();
            formatter.configure(emptyMap());

            while (true) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<byte[], byte[]> record : records) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    formatter.writeTo(record, new PrintStream(out));
                    System.out.println(out.toString().trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
