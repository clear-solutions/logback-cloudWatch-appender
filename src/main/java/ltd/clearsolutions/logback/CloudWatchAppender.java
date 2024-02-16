package ltd.clearsolutions.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ltd.clearsolutions.logback.configuration.CloudWatchConfiguration;
import ltd.clearsolutions.logback.configuration.LogbackConfiguration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class CloudWatchAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private Layout<ILoggingEvent> layout;
    private Encoder<ILoggingEvent> encoder;

    private String logGroupName;
    private String logStreamName;
    private String logRegion;
    private String cloudWatchEndpoint;
    private String accessKeyId;
    private String secretAccessKey;
    private int retentionTimeDays = 0;

    private final BlockingQueue<ILoggingEvent> logs = new ArrayBlockingQueue<>(10000);
    private Thread worker;

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        boolean offer = logs.offer(iLoggingEvent);
        if (!offer) {
            addWarn("Log queue is full, discarding log event: " + iLoggingEvent);
        }
    }

    @Override
    public void start() {
        super.start();
        var configuration = new CloudWatchConfiguration(logGroupName,
                                                        logStreamName,
                                                        logRegion,
                                                        cloudWatchEndpoint,
                                                        accessKeyId,
                                                        secretAccessKey,
                                                        retentionTimeDays);
        var cloudWatchLogWriter = new CloudWatchLogWriter(configuration);
        var logbackConfiguration = new LogbackConfiguration(layout, encoder);

        worker = new Thread(new Worker(logs, cloudWatchLogWriter, logbackConfiguration));
        worker.setDaemon(true);
        worker.setName("CloudWatchAppender-Worker");
        worker.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (worker != null) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                worker.interrupt();
            }
            worker = null;
        }
        logs.clear();
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    public void setLogGroupName(String logGroupName) {
        this.logGroupName = logGroupName;
    }

    public void setLogStreamName(String logStreamName) {
        this.logStreamName = logStreamName;
    }

    public void setLogRegion(String logRegion) {
        this.logRegion = logRegion;
    }

    public void setCloudWatchEndpoint(String cloudWatchEndpoint) {
        this.cloudWatchEndpoint = cloudWatchEndpoint;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public void setRetentionTimeDays(int retentionTimeDays) {
        this.retentionTimeDays = retentionTimeDays;
    }
}
