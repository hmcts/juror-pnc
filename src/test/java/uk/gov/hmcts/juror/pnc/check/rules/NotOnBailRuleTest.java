package uk.gov.hmcts.juror.pnc.check.rules;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDto;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSets;
import uk.gov.hmcts.juror.pnc.check.testsupport.AbstractRuleTest;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


class NotOnBailRuleTest extends AbstractRuleTest<NotOnBailRule, PersonDto> {

    public NotOnBailRuleTest() {
        super(RuleSets.PERSON_RULE_SET);
    }

    @Override
    protected Stream<? extends AbstractRuleTest<NotOnBailRule, PersonDto>.RuleArgument> getPassingRuleArgumentSource() {
        return Stream.of(
            ruleArgument("Not On Bail", PersonDto.builder().onBail(false).build()),
            ruleArgument("On Bail is null", PersonDto.builder().onBail(null).build())
        );
    }

    @Override
    protected Stream<? extends AbstractRuleTest<NotOnBailRule, PersonDto>.RuleArgument> getFailingRuleArgumentSource() {
        return Stream.of(
            ruleArgument("On Bail", PersonDto.builder().onBail(true).build())
        );
    }

    @Test
    void positiveGetDescription() {
        assertEquals("Juror must not be on bail.", createRule(false).getDescription(),
            "Description must match");
        assertEquals("Juror must be on bail.", createRule(true).getDescription(),
            "Description must match");
    }

    @Test
    void defaultConstructorTest() {
        NotOnBailRule rule = new NotOnBailRule();
        assertFalse(rule.isFailOnPass(), "Should default to failOnPass false");
    }


    @Override
    protected NotOnBailRule createRule(boolean failOnPass) {
        return new NotOnBailRule(failOnPass);
    }
}
