package io.clearsolutions.logback.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SpecialSymbolsUtilTest {

    @ParameterizedTest
    @MethodSource("checkMultiArgumentsMethodSource")
    void containsSpecialSymbolsExactlyOnceTest(String value, boolean expected) {
        Assertions.assertThat(SpecialSymbolsUtil.containsSpecialSymbolsExactlyOnce(value)).isEqualTo(expected);
    }

    static Stream<Arguments> checkMultiArgumentsMethodSource() {
        return Stream.of(
            Arguments.of("ball-dispenser-${env:env_name}", true),
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