package uk.gov.hmcts.juror.pnc.check.testsupport;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.juror.pnc.check.rules.support.Rule;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSet;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
@ExtendWith(SpringExtension.class)
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
public abstract class AbstractRuleTest<R extends Rule<T>, T> {

    private final RuleSet<T> ruleSet;

    public AbstractRuleTest(RuleSet<T> ruleSet) {
        this.ruleSet = ruleSet;
    }

    @ParameterizedTest(name = "Valid rule should pass: {0}")
    @MethodSource({"getPassingRuleArgumentSource"})
    @DisplayName("Valid rule - FailOnPass: false")
    void positiveRulePassWithFailOnPassFalse(String title, T data,
                                             Function<Boolean, R> ruleSupplier) {
        R rule = ruleSupplier.apply(false);
        assertTrue(rule.validate(data), "Rule should pass validation");
    }

    @ParameterizedTest(name = "Valid rule - rule should fail: {0}")
    @MethodSource({"getPassingRuleArgumentSource"})
    @DisplayName("Passing rule - FailOnPass: true")
    void positiveRulePassWithFailOnPassTrue(String title, T data,
                                            Function<Boolean, R> ruleSupplier) {
        R rule = ruleSupplier.apply(true);
        assertFalse(rule.validate(data), "Rule should fail validation");
    }

    @ParameterizedTest(name = "Invalid rule - rule should fail: {0}")
    @MethodSource({"getFailingRuleArgumentSource"})
    @DisplayName("Failing rule - FailOnPass: false")
    void negativeRuleFailedWithFailOnPassFalse(String title, T data,
                                               Function<Boolean, R> ruleSupplier) {
        R rule = ruleSupplier.apply(false);
        assertFalse(rule.validate(data), "Rule should fail validation");
    }


    @ParameterizedTest(name = "Invalid rule - rule should pass: {0}")
    @MethodSource({"getFailingRuleArgumentSource"})
    @DisplayName("Failing rule - FailOnPass: true")
    void negativeRuleFailedWithFailOnPassTrue(String title, T data,
                                              Function<Boolean, R> ruleSupplier) {
        R rule = ruleSupplier.apply(true);
        assertTrue(rule.validate(data), "Rule should pass validation");
    }

    protected abstract R createRule(boolean failOnPass);


    @Test
    void validateRuleSet() {
        R rule = createRule(true);
        assertEquals(ruleSet, rule.getRuleSet(), "Rules set should match rule set");
    }

    protected abstract Stream<? extends RuleArgument> getPassingRuleArgumentSource();

    protected abstract Stream<? extends RuleArgument> getFailingRuleArgumentSource();

    protected RuleArgument ruleArgument(String title, T data,
                                        Function<Boolean, R> ruleSupplier) {
        return new RuleArgument(title, data, ruleSupplier);
    }

    protected RuleArgument ruleArgument(String title, T data) {
        return ruleArgument(title, data, this::createRule);
    }

    protected class RuleArgument implements Arguments {
        private final Function<Boolean, R> ruleSupplier;
        private final String title;
        private final T data;

        public RuleArgument(String title, T data, Function<Boolean, R> ruleSupplier) {
            this.title = title;
            this.data = data;
            this.ruleSupplier = ruleSupplier;
        }

        @Override
        public final Object[] get() {
            return new Object[]{title, data, ruleSupplier};
        }
    }
}
