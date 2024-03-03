package io.clearsolutions.logback.configuration;

import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class CloudWatchConfiguration {

    private final String logGroupName;
    private final String logStreamName;
    private final String logRegion;
    private final String cloudWatchEndpoint;
    private final String accessKeyId;
    private final String secretAccessKey;
    private int retentionTimeInDays;

    public CloudWatchConfiguration(String logGroupName,
                                   String logStreamName,
                                   String logRegion,
                                   String cloudWatchEndpoint,
                                   String accessKeyId,
                                   String secretAccessKey,
                                   int retentionTimeInDays) {

        this.logGroupName = removeUndefinedValue(createLogGroupName(logGroupName));
        this.logStreamName = removeUndefinedValue(createLogStreamName(logStreamName));
        this.logRegion = removeUndefinedValue(logRegion);
        this.cloudWatchEndpoint = removeUndefinedValue(cloudWatchEndpoint);
        this.accessKeyId = removeUndefinedValue(accessKeyId);
        this.secretAccessKey = removeUndefinedValue(secretAccessKey);
        this.retentionTimeInDays = retentionTimeInDays;
    }

    private String removeUndefinedValue(String value) {
        if (isNull(value) || value.contains("UNDEFINED")) {
            return null;
        }
        return value;
    }

    public boolean isConfigured() {
        return nonNull(logGroupName)
               && nonNull(logStreamName)
               && nonNull(logRegion)
               && nonNull(accessKeyId)
               && nonNull(secretAccessKey);
    }

    String createLogGroupName(String logGroupName) {
        if (isNull(logGroupName)) {
            return "logback";
        }

        return logGroupName;
    }

    String createLogStreamName(String logStreamName) {
        if (isNull(logStreamName)) {
            String ec2InstanceId = EC2MetadataUtils.getInstanceId();
            if (nonNull(ec2InstanceId)) {
                return ec2InstanceId;
            } else {
                return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date());
            }
        }
        return logStreamName;
    }

    public String getLogGroupName() {
        return logGroupName;
    }

    public String getLogStreamName() {
        return logStreamName;
    }

    public String getLogRegion() {
        return logRegion;
    }

    public String getCloudWatchEndpoint() {
        return cloudWatchEndpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public int getRetentionTimeInDays() {
        return retentionTimeInDays;
    }
}
