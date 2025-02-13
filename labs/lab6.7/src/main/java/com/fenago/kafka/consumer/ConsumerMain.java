package com.fenago.kafka.consumer;

import com.fenago.kafka.StockAppConstants;
import com.fenago.kafka.model.StockPrice;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static com.fenago.kafka.StockAppConstants.TOPIC;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class ConsumerMain {
    private static final Logger logger =
            LoggerFactory.getLogger(ConsumerMain.class);


    private static Consumer<String, StockPrice> createConsumer() {
        final Properties props = new Properties();

        //Turn off auto commit - "enable.auto.commit".
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                StockAppConstants.BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                "KafkaExampleConsumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        //Custom Deserializer
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StockDeserializer.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);


        // Create the consumer using props.
        return new KafkaConsumer<>(props);
    }

    //TODO edit this method.
    public static void main(String... args) throws Exception {



        //TODO Look up partitionInfos from the consumer.
        final List<PartitionInfo> partitionInfos = null; //createConsumer().partitionsFor(TOPIC);

        //TODO Set threadCount to partition count.
        final int threadCount = -1; // FIX THIS

        final int workerThreads = 3;

        final ExecutorService executorService = newFixedThreadPool(threadCount);
        final AtomicBoolean stopAll = new AtomicBoolean();
        final List<Consumer> consumerList = new ArrayList<>(threadCount);


        IntStream.range(0, threadCount).forEach(index -> {
            final Consumer<String, StockPrice> consumer = createConsumer();

            // TODO Get the partition info.
            final PartitionInfo partitionInfo = null; //FIX THIS. HINT: partitionInfos.get(index);

            // TODO importantPartition
            final boolean importantPartition = false; //FIX THIS. HINT partition == partitionInfos.size() -1;

            // TODO use three X the workers if this is a priority partition.
            final int workerCount = -1; // FIX THIS. HINT if importantPartition workerThreads * 3 else workerThreads


            final StockPriceConsumerRunnable stockPriceConsumer =
                    new StockPriceConsumerRunnable(consumer,
                            100, index, stopAll, workerCount);
            consumerList.add(consumer);
            executorService.submit(stockPriceConsumer);
        });

        //Register nice shutdown of thread pool, then close consumer.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping app");
            stopAll.set(true);
            sleep();
            consumerList.forEach(Consumer::wakeup);
            executorService.shutdown();
            try {
                executorService.awaitTermination(5_000, TimeUnit.MILLISECONDS);
                if (!executorService.isShutdown())
                    executorService.shutdownNow();
            } catch (InterruptedException e) {
                logger.warn("shutting down", e);
            }
            sleep();
            consumerList.forEach(Consumer::close);
        }));
    }

    private static void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            logger.error("", e);
        }
    }

}
