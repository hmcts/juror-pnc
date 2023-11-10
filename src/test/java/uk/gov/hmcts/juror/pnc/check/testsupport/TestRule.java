package uk.gov.hmcts.juror.pnc.check.testsupport;

import uk.gov.hmcts.juror.pnc.check.rules.support.Rule;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSet;

@SuppressWarnings("PMD.TestClassWithoutTestCases")//False positive - support class
public class TestRule extends Rule<Boolean> {


    public static final RuleSet<Boolean> RULE_SET = () -> Boolean.class;

    public TestRule(boolean failOnPass) {
        super(failOnPass);
    }

    @Override
    protected boolean execute(Boolean value) {
        return value;
    }

    @Override
    public RuleSet<Boolean> getRuleSet() {
        return RULE_SET;
    }

    @Override
    public String getDescription() {
        return "MyDescription";
    }
}
