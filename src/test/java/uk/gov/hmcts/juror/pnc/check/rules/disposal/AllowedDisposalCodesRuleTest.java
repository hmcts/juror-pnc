package uk.gov.hmcts.juror.pnc.check.rules.disposal;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.testsupport.AbstractDisposalRuleTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllowedDisposalCodesRuleTest extends AbstractDisposalRuleTest<AllowedDisposalCodesRule> {

    private final Set<Integer> allowedCodes = Set.of(1, 2, 3, 4, 5);


    @Override
    protected Stream<DisposalArgument> getPassingDisposalArgumentSource() {
        Stream.Builder<DisposalArgument> builder = Stream.builder();

        this.allowedCodes.forEach(
            code -> builder.add(
                disposalArgument(
                    "Allowed disposal code: " + code, DisposalDto.builder().disposalCode(Integer.toString(code)).build()
                )
            )
        );
        return builder.build();
    }

    @Override
    protected Stream<DisposalArgument> getFailingDisposalArgumentSource() {
        Stream.Builder<DisposalArgument> builder = Stream.builder();

        this.allowedCodes.forEach(
            code -> builder.add(
                disposalArgument(
                    "Allowed disposal code: " + code,
                    DisposalDto.builder().disposalCode(Integer.toString(-code)).build()
                )
            )
        );
        return builder.build();
    }

    @Override
    protected void negativeUnsupportedDisposalCode(Integer disposalCode) {
        //Do nothing overridden to prevent default behaviour
    }

    @Override
    protected void negativeInvalidDisposalCode(String disposalCode) {
        //Do nothing overridden to prevent default behaviour
    }

    @Test
    void positiveGetDescriptionTest() {
        assertEquals("Disposal code must be one of 1, 2, 3, 4, 5",
            createRule(null, false).getDescription(),
            "Description must match");
        assertEquals("Disposal code must not be one of 1, 2, 3, 4, 5",
            createRule(null, true).getDescription(),
            "Description must match");
    }


    @Override
    protected AllowedDisposalCodesRule createRule(Set<Integer> supportedCodes, boolean failOnPass) {
        return new AllowedDisposalCodesRule(allowedCodes, failOnPass);
    }
}
