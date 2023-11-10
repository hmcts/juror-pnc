package uk.gov.hmcts.juror.pnc.check.rules.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleValidationResultTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void positiveConstructorTest(boolean passed) {
        final String message = "MyMessage";
        RuleValidationResult result = new RuleValidationResult(passed, message);
        assertEquals(message, result.getMessage(),
            "Message should match inputted value");
        assertEquals(passed, result.isPassed(),
            "Passed should match inputted value");
    }

    @Test
    void positiveConstructorPassedTest() {
        RuleValidationResult result = RuleValidationResult.passed();
        assertNull(result.getMessage(), "Message should be null on default passed");
        assertTrue(result.isPassed(),
            "Passed should match true passed");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"MyMessage", "a new message", "some other message"})
    void positiveConstructorFailedTest(String message) {
        RuleValidationResult result = RuleValidationResult.failed(message);
        assertEquals(message, result.getMessage(), "Message should be match inputtedred value");
        assertFalse(result.isPassed(),
            "Passed should be false on failed");
    }
}
