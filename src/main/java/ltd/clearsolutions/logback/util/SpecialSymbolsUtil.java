package ltd.clearsolutions.logback.util;

import ltd.clearsolutions.logback.exception.CloudWatchAppenderException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpecialSymbolsUtil {

    private static final Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
    public static boolean containsSpecialSymbolsExactlyOnce(String text) {
        Matcher matcher = pattern.matcher(text);

        // Check if the pattern is found
        boolean found = matcher.find();

        // If found, check if there's another occurrence. If not, return true.
        // This ensures the pattern "${...}" occurs exactly once.
        return found && !matcher.find();
    }

    public static String extractValue(String input) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new CloudWatchAppenderException("No special symbols found in the input");
    }
}
