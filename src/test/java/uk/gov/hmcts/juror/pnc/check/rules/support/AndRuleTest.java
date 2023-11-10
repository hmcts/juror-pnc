package uk.gov.hmcts.juror.pnc.check.rules.support;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.testsupport.AbstractDisposalRuleTest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AndRuleTest extends AbstractDisposalRuleTest<AndRule> {

    private final DisposalRule passingRule;
    private final DisposalRule failingRule;

    public AndRuleTest() {
        super();
        this.passingRule = new TestDisposalRule(true);
        this.failingRule = new TestDisposalRule(false);
    }

    @Override
    protected Stream<AbstractDisposalRuleTest<AndRule>.DisposalArgument> getPassingDisposalArgumentSource() {
        return Stream.of(
            disposalArgument("Single rule", DisposalDto.builder().disposalCode("1").build(),
                createRuleSupplier(List.of(passingRule))),
            disposalArgument("Multiple rules", DisposalDto.builder().disposalCode("1").build(),
                createRuleSupplier(List.of(passingRule, passingRule, passingRule))),
            disposalArgument("Empty rules", DisposalDto.builder().disposalCode("1").build(),
                createRuleSupplier(Collections.emptyList()))
        );
    }

    @Override
    protected Stream<AbstractDisposalRuleTest<AndRule>.DisposalArgument> getFailingDisposalArgumentSource() {
        return Stream.of(
            disposalArgument("Single rule failure", DisposalDto.builder().disposalCode("1").build(),
                createRuleSupplier(List.of(failingRule))),
            disposalArgument("Multiple rules failure", DisposalDto.builder().disposalCode("1").build(),
                createRuleSupplier(List.of(failingRule, failingRule, failingRule))),
            disposalArgument("Passing and failing rules", DisposalDto.builder().disposalCode("1").build(),
                createRuleSupplier(List.of(passingRule, passingRule, failingRule)))
        );
    }


    @Test
    void positiveGetDescriptionSingleRuleTest() {
        assertEquals("MyDescription: testResult: true",
            createRule(null, false, List.of(passingRule))
                .getDescription(),
            "Description must match");
        assertEquals("MyDescription: testResult: true",
            createRule(null, true, List.of(passingRule))
                .getDescription(),
            "Description must match");
    }

    @Test
    void positiveGetDescriptionMultipleRulesTest() {
        assertEquals("MyDescription: testResult: true"
                + " and MyDescription: testResult: false"
                + " and MyDescription: testResult: true",
            createRule(null, false, List.of(passingRule, failingRule, passingRule))
                .getDescription(),
            "Description must match");
    }

    @Override
    protected AndRule createRule(Set<Integer> supportedCodes, boolean failOnPass) {
        return createRule(supportedCodes, failOnPass, Set.of(passingRule));
    }

    private AndRule createRule(Set<Integer> supportedCodes, boolean failOnPass, Collection<DisposalRule> rules) {
        AndRule andRule = new AndRule(supportedCodes, failOnPass);
        rules.forEach(andRule::addRule);
        return andRule;
    }

    private BiFunction<Set<Integer>, Boolean, AndRule> createRuleSupplier(Collection<DisposalRule> rules) {
        return (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, rules);
    }


    @SuppressWarnings("PMD.TestClassWithoutTestCases")//False positive - support class
    private static class TestDisposalRule extends DisposalRule {

        private final boolean testResult;

        private TestDisposalRule(boolean testResult) {
            super(Collections.emptySet(), false);
            this.testResult = testResult;
        }

        @Override
        protected boolean execute(DisposalDto value) {
            return testResult;
        }

        @Override
        public String getDescription() {
            return "MyDescription: testResult: " + testResult;
        }
    }
}
