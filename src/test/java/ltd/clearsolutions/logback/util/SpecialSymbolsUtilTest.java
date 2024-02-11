package ltd.clearsolutions.logback.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static ltd.clearsolutions.logback.util.SpecialSymbolsUtil.containsSpecialSymbolsExactlyOnce;
import static org.assertj.core.api.Assertions.assertThat;

class SpecialSymbolsUtilTest {

    @ParameterizedTest
    @MethodSource("checkMultiArgumentsMethodSource")
    void containsSpecialSymbolsExactlyOnceTest(String value, boolean expected) {
        assertThat(containsSpecialSymbolsExactlyOnce(value)).isEqualTo(expected);
    }

    static Stream<Arguments> checkMultiArgumentsMethodSource() {
        return Stream.of(
            Arguments.of("${test}", true),
            Arguments.of("${test}-test", true),
            Arguments.of("test-${test}", true),
            Arguments.of("$test-{test}", false),
            Arguments.of("$test}", false),
            Arguments.of("${test", false),
            Arguments.of("{test}", false),
            Arguments.of("{}", false),
            Arguments.of("${test}${}", false),
            Arguments.of("", false)
        );
    }
}