package io.clearsolutions.logback.configuration;

import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static io.clearsolutions.logback.util.SpecialSymbolsUtil.containsSpecialSymbolsExactlyOnce;
import static io.clearsolutions.logback.util.SpecialSymbolsUtil.extractValue;

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

        this.logGroupName = createLogGroupName(logGroupName);
        this.logStreamName = createLogStreamName(logStreamName);
        this.logRegion = logRegion;
        this.cloudWatchEndpoint = cloudWatchEndpoint;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.retentionTimeInDays = retentionTimeInDays;
    }

    private String createLogGroupName(String logGroupName) {
        if (isNull(logGroupName)) {
            return "logback";
        }

        if (containsSpecialSymbolsExactlyOnce(logGroupName)) {
            return resolveName(logGroupName);
        }

        return logGroupName;
    }

    private String resolveName(String name) {
        String groupNameBaseOnEnv = extractValue(name);
        if (groupNameBaseOnEnv.contains("env:")) {
            String env = System.getenv(groupNameBaseOnEnv.split(":")[1]);
            if (env != null) {
                return name.replace("${" + groupNameBaseOnEnv + "}", env);
            }
        }
        return name;
    }

    private String createLogStreamName(String logStreamName) {
        if (isNull(logStreamName)) {
            String ec2InstanceId = EC2MetadataUtils.getInstanceId();
            if (nonNull(ec2InstanceId)) {
                return ec2InstanceId;
            } else {
                return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date());
            }
        }

        if (containsSpecialSymbolsExactlyOnce(logStreamName)) {
            return resolveName(logStreamName);
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
