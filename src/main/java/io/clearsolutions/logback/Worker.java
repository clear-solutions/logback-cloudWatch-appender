package io.clearsolutions.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.clearsolutions.logback.configuration.LogbackConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class Worker implements Runnable {

    private final long thresholdTime = 1000; // 1 seconds in milliseconds
    private final int maxSize = 100; // Minimum number of log events to process at once

    private final BlockingQueue<ILoggingEvent> logs;
    private final CloudWatchLogWriter cloudWatchLogWriter;
    private final LogbackConfiguration configuration;

    public Worker(BlockingQueue<ILoggingEvent> logs, CloudWatchLogWriter cloudWatchLogWriter,
                  LogbackConfiguration configuration) {
        this.logs = logs;
        this.cloudWatchLogWriter = cloudWatchLogWriter;
        this.configuration = configuration;
    }

    @Override
    public void run() {
        try {
            long lastDrainTime = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted()) {
                if ((System.currentTimeMillis() - lastDrainTime >= thresholdTime
                    || logs.size() >= maxSize)
                    && ! logs.isEmpty()) {

                    List<ILoggingEvent> buffer = new ArrayList<>();
                    logs.drainTo(buffer);
                    processBuffer(buffer);
                    lastDrainTime = System.currentTimeMillis();
                } else {
                    // Sleep for a short duration to avoid tight looping, adjust as necessary
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Queue processing thread was interrupted.");
        }
    }

    public void processBuffer(List<ILoggingEvent> buffer) {
        List<LogEventDTO> collect = buffer.stream()
                                          .map(this::createLogEventDTO)
                                          .sorted(Comparator.comparingLong(LogEventDTO::getTimestamp))
                                          .collect(Collectors.toList());
        cloudWatchLogWriter.write(collect);
    }

    private LogEventDTO createLogEventDTO(ILoggingEvent log) {
        String message = Optional.ofNullable(configuration.getEncoder())
                                 .map(encoder -> new String(encoder.encode(log), StandardCharsets.UTF_8))
                                 .orElseGet(() -> configuration.getLayout().doLayout(log));
        return new LogEventDTO(message, log.getTimeStamp());
    }
}
