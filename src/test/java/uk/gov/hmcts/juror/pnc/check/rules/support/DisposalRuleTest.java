package uk.gov.hmcts.juror.pnc.check.rules.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DisposalRuleTest {


    @Test
    void constructorTestValidSupportCodes() {
        Set<Integer> supportCodes = Set.of(1, 2, 3, 4);
        DisposalRule disposalRule = new TestDisposalRule(supportCodes, true, true);
        assertThat("Support codes should match input", disposalRule.getSupportedCodes(),
            hasItems(supportCodes.toArray(new Integer[0])));
        assertTrue(disposalRule.isFailOnPass(), "failOnPass should match inputted value");
    }

    @Test
    void constructorTestNullSupportCodes() {
        DisposalRule disposalRule = new TestDisposalRule(null, false, true);
        assertEquals(0, disposalRule.getSupportedCodes().size(),
            "Supported code should still be set up (as an empty set) even if input is null");
        assertFalse(disposalRule.isFailOnPass(), "failOnPass should match inputted value");
    }

    @Test
    void verifyRuleSetIsCorrect() {
        DisposalRule disposalRule = new TestDisposalRule(null, true, true);
        assertEquals(RuleSets.DISPOSAL_RULE_SET, disposalRule.getRuleSet(),
            "Rule set must match standard rules set object");
    }

    @Test
    void negativeInvalidDisposalCode() {
        //Execute Result =  false as default for unsupported is true
        DisposalRule disposalRule = new TestDisposalRule(null, true, false);
        assertTrue(disposalRule.validate(DisposalDto.builder().disposalCode("INVALID").build()),
            "Rule should be skipped if disposal code is invalid");
    }

    @Test
    void negativeNullDisposalCode() {
        //Execute Result =  false as default for unsupported is true
        DisposalRule disposalRule = new TestDisposalRule(Collections.emptyList(), true, false);
        assertTrue(disposalRule.validate(DisposalDto.builder().disposalCode(null).build()),
            "Rule should be skipped if disposal code is invalid");
    }

    @Test
    void negativeNotSupportedDisposalCode() {
        //Execute Result =  false as default for unsupported is true
        DisposalRule disposalRule = new TestDisposalRule(Set.of(1, 2, 3), false, false);
        assertTrue(disposalRule.validate(DisposalDto.builder().disposalCode("0").build()),
            "Rule should be skipped if disposal code is no supported");
        assertTrue(disposalRule.validate(DisposalDto.builder().disposalCode("4").build()),
            "Rule should be skipped if disposal code is no supported");
        assertTrue(disposalRule.validate(DisposalDto.builder().disposalCode("-1").build()),
            "Rule should be skipped if disposal code is no supported");
    }

    @Test
    void negativeNotSupportedDisposalCodeNotAffectedByFailOnPass() {
        //Execute Result =  false as default for unsupported is true
        DisposalRule disposalRule = new TestDisposalRule(Set.of(1, 2, 3), true, false);
        assertTrue(disposalRule.validate(DisposalDto.builder().disposalCode("0").build()),
            "Rule should be skipped if disposal code is no supported");
        assertTrue(disposalRule.validate(DisposalDto.builder().disposalCode("4").build()),
            "Rule should be skipped if disposal code is no supported");
        assertTrue(disposalRule.validate(DisposalDto.builder().disposalCode("-1").build()),
            "Rule should be skipped if disposal code is no supported");
    }

    @Test
    void positiveValidateWhenDisposalCodeIsValid() {
        //Execute Result =  false as default for unsupported is true
        DisposalRule disposalRule = new TestDisposalRule(Set.of(1, 2, 3), false, false);
        assertFalse(disposalRule.validate(DisposalDto.builder().disposalCode("1").build()),
            "Rule should be triggered if disposal code is supported");
        assertFalse(disposalRule.validate(DisposalDto.builder().disposalCode("2").build()),
            "Rule should be triggered if disposal code is supported");
        assertFalse(disposalRule.validate(DisposalDto.builder().disposalCode("3").build()),
            "Rule should be triggered if disposal code is supported");
    }

    @Test
    @DisplayName("Should still validate if supported codes are empty.")
    void positiveEmptyDisposalCodes() {
        //Execute Result =  false as default for unsupported is true
        DisposalRule disposalRule = new TestDisposalRule(Collections.emptyList(), false, false);
        assertFalse(disposalRule.validate(DisposalDto.builder().disposalCode("1").build()),
            "Rule should be triggered if no supported codes are given");
    }

    @Test
    void positiveResultShouldFlipIfFailOnPass() {
        //Execute Result =  false as default for unsupported is true
        DisposalRule disposalRule = new TestDisposalRule(Set.of(1, 2, 3), true, false);
        assertTrue(disposalRule.validate(DisposalDto.builder().disposalCode("1").build()),
            "Rule should be triggered if disposal code is supported");
        assertTrue(disposalRule.validate(DisposalDto.builder().disposalCode("2").build()),
            "Rule should be triggered if disposal code is supported");
        assertTrue(disposalRule.validate(DisposalDto.builder().disposalCode("3").build()),
            "Rule should be triggered if disposal code is supported");
    }

    @SuppressWarnings("PMD.TestClassWithoutTestCases")//False positive - support class

    private class TestDisposalRule extends DisposalRule {

        private final boolean executeResult;

        protected TestDisposalRule(Collection<Integer> supportedCodes, boolean failOnPass, boolean executeResult) {
            super(supportedCodes, failOnPass);
            this.executeResult = executeResult;
        }

        @Override
        protected boolean execute(DisposalDto value) {
            return executeResult;
        }

        @Override
        public String getDescription() {
            return "MyDescription";
        }
    }
}
