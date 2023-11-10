package uk.gov.hmcts.juror.pnc.check.rules.disposal;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.testsupport.AbstractDisposalRuleTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class QualLiteralCheckRuleTest extends AbstractDisposalRuleTest<QualLiteralCheckRule> {

    private final String defaultExpectedValue;


    public QualLiteralCheckRuleTest() {
        super();
        this.defaultExpectedValue = "S";
    }

    @Override
    protected Stream<DisposalArgument> getPassingDisposalArgumentSource() {
        return Stream.of(disposalArgument("Typical", DisposalDto.builder().disposalCode("1").qualLiteral("S").build()),
            disposalArgument("QualLiteral has many characters",
                DisposalDto.builder().disposalCode("1").qualLiteral("S12345").build()));
    }

    @Override
    protected Stream<DisposalArgument> getFailingDisposalArgumentSource() {
        return Stream.of(
            disposalArgument("Wrong literal found", DisposalDto.builder().disposalCode("1").qualLiteral("D").build()),
            disposalArgument("Blank literal found", DisposalDto.builder().disposalCode("1").qualLiteral("").build()),
            disposalArgument("Null literal found", DisposalDto.builder().disposalCode("1").qualLiteral(null).build()));
    }

    @Test
    void positiveGetDescriptionTest() {
        assertEquals("QualLiteral must contain value S", createRule(null, false, "S").getDescription(),
            "Description must match");
        assertEquals("QualLiteral must contain value D", createRule(null, false, "D").getDescription(),
            "Description must match");
        assertEquals("QualLiteral must not contain value S", createRule(null, true, "S").getDescription(),
            "Description must match");
        assertEquals("QualLiteral must not contain value D", createRule(null, true, "D").getDescription(),
            "Description must match");
    }

    @Override
    protected QualLiteralCheckRule createRule(Set<Integer> supportedCodes, boolean failOnPass) {
        return createRule(supportedCodes, failOnPass, defaultExpectedValue);
    }

    protected QualLiteralCheckRule createRule(Set<Integer> supportedCodes, boolean failOnPass, String expectedValue) {
        return new QualLiteralCheckRule(supportedCodes, failOnPass, expectedValue);
    }
}
