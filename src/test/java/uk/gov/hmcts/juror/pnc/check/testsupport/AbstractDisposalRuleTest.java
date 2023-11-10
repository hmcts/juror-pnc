package uk.gov.hmcts.juror.pnc.check.testsupport;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.rules.support.DisposalRule;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSets;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
@ExtendWith(SpringExtension.class)
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
public abstract class AbstractDisposalRuleTest<R extends DisposalRule>
    extends AbstractRuleTest<R, DisposalDto> {


    protected static final Set<Integer> SUPPORTED_CODES = Set.of(1, 2, 3, 4, 5);

    public AbstractDisposalRuleTest() {
        super(RuleSets.DISPOSAL_RULE_SET);
    }


    @ParameterizedTest(name = "Unsupported disposal code - rule should fail")
    @ValueSource(ints = {-1, 6})
    protected void negativeUnsupportedDisposalCode(Integer disposalCode) {
        R rule = createRule(SUPPORTED_CODES, false);
        assertTrue(rule.validate(DisposalDto.builder().disposalCode(String.valueOf(disposalCode)).build()),
            "Rule should fail validation. As disposal code is not supported");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "one"})
    protected void negativeInvalidDisposalCode(String disposalCode) {
        R rule = createRule(SUPPORTED_CODES, false);
        assertTrue(rule.validate(DisposalDto.builder().disposalCode(disposalCode).build()),
            "Rule should pass validation if no disposal code is found");
    }

    protected abstract R createRule(Set<Integer> supportedCodes, boolean failOnPass);

    @Override
    protected R createRule(boolean failOnPass) {
        return createRule(SUPPORTED_CODES, false);
    }


    protected abstract Stream<DisposalArgument> getPassingDisposalArgumentSource();

    protected abstract Stream<DisposalArgument> getFailingDisposalArgumentSource();


    @Override
    protected final Stream<? extends AbstractRuleTest<R, DisposalDto>.RuleArgument> getPassingRuleArgumentSource() {
        return getPassingDisposalArgumentSource();
    }

    @Override
    protected final Stream<? extends AbstractRuleTest<R, DisposalDto>.RuleArgument> getFailingRuleArgumentSource() {
        return getFailingDisposalArgumentSource();
    }

    protected DisposalArgument disposalArgument(String title, DisposalDto disposalDto,
                                                BiFunction<Set<Integer>, Boolean, R> ruleSupplier) {
        return new DisposalArgument(title, disposalDto, ruleSupplier);
    }

    protected DisposalArgument disposalArgument(String title, DisposalDto disposalDto) {
        return disposalArgument(title, disposalDto, this::createRule);
    }

    protected class DisposalArgument extends RuleArgument {

        public DisposalArgument(String title, DisposalDto disposal, BiFunction<Set<Integer>, Boolean, R> ruleSupplier) {
            super(title, disposal, failOnPass -> ruleSupplier.apply(SUPPORTED_CODES, failOnPass));
        }
    }
}
