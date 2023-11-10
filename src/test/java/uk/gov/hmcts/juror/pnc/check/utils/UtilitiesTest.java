package uk.gov.hmcts.juror.pnc.check.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class UtilitiesTest {

    @Test
    void positiveGetInteger() {
        assertEquals(1, Utilities.getInteger("1"),
            "Must be correct number");
        assertEquals(-1, Utilities.getInteger("-1"),
            "Must be correct number");
        assertEquals(89, Utilities.getInteger("89"),
            "Must be correct number");
    }

    @Test
    void negativeGetIntegerInvalidNumber() {
        assertNull(Utilities.getInteger("INVALID"), "Must be null");
        assertNull(Utilities.getInteger("1.1"), "Must be null");
        assertNull(Utilities.getInteger("-"), "Must be null");
    }

    @Test
    void negativeGetIntegerIsBlank() {
        assertNull(Utilities.getInteger(""), "Must be null");

    }

    @Test
    void negativeGetIntegerIsNull() {
        assertNull(Utilities.getInteger(null), "Must be null");
    }


    public static Stream<Arguments> stripBadCharsArguments() {
        return Stream.of(
            arguments("someText (with brackets) ", "someText"),
            arguments("someText [with brackets] ", "someText"),
            arguments(" someText ", "someText"),
            arguments("text {}+=@€# with non supported chars", "textwithnonsupportedchars"),
            arguments(null, null),
            arguments("", "")
        );
    }

    @ParameterizedTest
    @MethodSource("stripBadCharsArguments")
    void positiveStripBadChars(String textToProcess, String expectedResponse) {
        assertEquals(
            expectedResponse,
            Utilities.stripBadChars(textToProcess),
            "Text must get processed correctly"
        );
    }
}
