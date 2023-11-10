package uk.gov.hmcts.juror.pnc.check.rules.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.juror.pnc.check.testsupport.TestRule;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleTest {


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void positiveConstructorTest(boolean failOnPass) {
        Rule<Boolean> rule = new TestRule(failOnPass);
        assertEquals(failOnPass, rule.isFailOnPass(),
            "FailOnPass should match inputted value");
    }

    @Test
    void positiveGetNameTest() {
        Rule<Boolean> rule = new TestRule(true);
        assertEquals("TestRule", rule.getName(),
            "Name should default to class name");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void positiveValidateTypical(boolean result) {
        Rule<Boolean> rule = new TestRule(false);
        assertEquals(result, rule.validate(result),
            "Result should be not flipped. I.e pass should = pass as fail on pass = false");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void positiveValidateWithFailOnPass(boolean result) {
        Rule<Boolean> rule = new TestRule(true);
        assertEquals(!result, rule.validate(result),
            "Result should be flipped. I.e pass should = fail as fail on pass = true");
    }
}