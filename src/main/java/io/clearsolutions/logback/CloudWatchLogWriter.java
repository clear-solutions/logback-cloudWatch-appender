package io.clearsolutions.logback;

import io.clearsolutions.logback.configuration.CloudWatchConfiguration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidSequenceTokenException;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutRetentionPolicyRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceAlreadyExistsException;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

public class CloudWatchLogWriter {

    private static final int MAX_MESSAGE_SIZE = 256 * 1024;
    private static final String THREE_DOTS = "...";
    private static final int PADDING = 42;//because this is answer to everything
    private static final int MAX_RETRIES = 3;

    private final CloudWatchConfiguration configuration;
    private final CloudWatchLogsClient cloudWatchLogsClient;
    private String sequenceToken;

    public CloudWatchLogWriter(CloudWatchConfiguration cloudWatchConfiguration) {
        this.configuration = cloudWatchConfiguration;
        this.cloudWatchLogsClient = build();
        initCloudWatchLogGroup();
    }

    public void write(List<LogEventDTO> logs) {
        writeWithRetry(logs, MAX_RETRIES);
    }
    private void writeWithRetry(List<LogEventDTO> logs, int retriesLeft) {
        try {
            PutLogEventsRequest request = putLogEventsRequest(logs);
            PutLogEventsResponse putLogEventsResponse = cloudWatchLogsClient.putLogEvents(request);
            sequenceToken = putLogEventsResponse.nextSequenceToken();
        } catch (InvalidSequenceTokenException ex) {
            if (retriesLeft > 0) {
                System.out.println("Invalid sequence token, retrying... Retries left: " + (retriesLeft - 1));
                sequenceToken = ex.expectedSequenceToken();
                writeWithRetry(logs, retriesLeft - 1);
            } else {
                System.err.println("Failed to write logs after " + MAX_RETRIES + " retries.");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private PutLogEventsRequest putLogEventsRequest(List<LogEventDTO> logs) {
        var request = PutLogEventsRequest.builder()
                                         .logGroupName(configuration.getLogGroupName())
                                         .logStreamName(configuration.getLogStreamName())
                                         .logEvents(logs.stream()
                                                        .map(log -> {
                                                            String message = ensureNotLargerThan256KB(log.getMessage());
                                                            return InputLogEvent.builder()
                                                                                .message(message)
                                                                                .timestamp(log.getTimestamp())
                                                                                .build();
                                                        })
                                                        .collect(Collectors.toList()));

        // Add the sequenceToken if it is not null
        if (nonNull(sequenceToken)) {
            request = request.sequenceToken(sequenceToken);
        }
        return request.build();
    }

    /**
     * Checks if a string is larger than 256 KB in UTF-8 encoding and cuts it to fit within this size if necessary.
     *
     * @param input The input string to check and possibly cut.
     * @return A string that is guaranteed to be less than or equal to 256 KB in UTF-8 byte size.
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_InputLogEvent.html">InputLogEvent</a>
     */
    public String ensureNotLargerThan256KB(String input) {
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

        if (inputBytes.length <= MAX_MESSAGE_SIZE) {
            return input;
        }

        int cutSize = inputBytes.length - PADDING - THREE_DOTS.getBytes(StandardCharsets.UTF_8).length;

        String unsafeTrimmed = new String(inputBytes, 0, cutSize + 1, StandardCharsets.UTF_8);
        String cutString = unsafeTrimmed.substring(0, unsafeTrimmed.length() - 1);
        return cutString + THREE_DOTS;
    }

    private CloudWatchLogsClient build() {
        CloudWatchLogsClientBuilder builder = CloudWatchLogsClient.builder();
        System.out.println("Creating AWSLogs Client");

        if (nonNull(configuration.getCloudWatchEndpoint())) {
            try {
                builder = builder.endpointOverride(new URI(configuration.getCloudWatchEndpoint()));
            } catch (URISyntaxException e) {
                String error = format("Invalid endpoint endpoint URL: $s. Error:  %s",
                                      configuration.getCloudWatchEndpoint(),
                                      e.getMessage());
                System.out.println(error);
            }
        }

        if (nonNull(configuration.getLogRegion())) {
            builder = builder.region(Region.of(configuration.getLogRegion()));
        }

        if (nonNull(configuration.getAccessKeyId())
            && nonNull(configuration.getSecretAccessKey())) {

            var awsBasicCredentials =
                AwsBasicCredentials.create(configuration.getAccessKeyId(), configuration.getSecretAccessKey());
            AwsCredentialsProvider credentialProvider = StaticCredentialsProvider.create(awsBasicCredentials);

            builder.credentialsProvider(credentialProvider);
        }

        return builder.build();
    }

    private void initCloudWatchLogGroup() {
        try {
            cloudWatchLogsClient.createLogGroup(CreateLogGroupRequest.builder()
                                                                     .logGroupName(configuration.getLogGroupName())
                                                                     .build());
            if (configuration.getRetentionTimeInDays() > 0) {
                cloudWatchLogsClient.putRetentionPolicy(PutRetentionPolicyRequest.builder()
                                                                                 .logGroupName(
                                                                                     configuration.getLogGroupName())
                                                                                 .retentionInDays(
                                                                                     configuration.getRetentionTimeInDays())
                                                                                 .build());
            }
        } catch (ResourceAlreadyExistsException e) {
            //TODO is it better check if the log group exists?
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            cloudWatchLogsClient.createLogStream(CreateLogStreamRequest.builder()
                                                                       .logGroupName(configuration.getLogGroupName())
                                                                       .logStreamName(configuration.getLogStreamName())
                                                                       .build());
        } catch (ResourceAlreadyExistsException e) {
            //TODO is it better check if the log group exists?
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
