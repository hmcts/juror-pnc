package uk.gov.hmcts.juror.pnc.check.rules.disposal;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.rules.Comparator;
import uk.gov.hmcts.juror.pnc.check.testsupport.AbstractDisposalRuleTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class SentenceLengthRuleTest extends AbstractDisposalRuleTest<SentenceLengthRule> {

    private final Comparator defaultComparator;
    private final int defaultValue;

    public SentenceLengthRuleTest() {
        super();
        this.defaultComparator = Comparator.EQUAL_TO;
        this.defaultValue = 100;
    }

    @Override
    protected Stream<DisposalArgument> getPassingDisposalArgumentSource() {
        return Stream.of(
            disposalArgument("Typical",
                DisposalDto.builder().disposalCode("1").sentenceAmount("100").sentencePeriod("Y").build()),
            disposalArgument("Sentence Period type not 'Y'",
                DisposalDto.builder().disposalCode("1").sentenceAmount("1000").sentencePeriod("N").build()),
            disposalArgument("Sentence Period blank - Should default to 'Y'",
                DisposalDto.builder().disposalCode("1").sentenceAmount("100").sentencePeriod("").build()),
            disposalArgument("Sentence Period null - Should default to 'Y'",
                DisposalDto.builder().disposalCode("1").sentenceAmount("100").sentencePeriod(null).build()),
            disposalArgument("Sentence Amount Blank - Should default to '0'",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("Y").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.EQUAL_TO, 0)),
            disposalArgument("Sentence Amount null - Should default to '0'",
                DisposalDto.builder().disposalCode("1").sentenceAmount(null).sentencePeriod("Y").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.EQUAL_TO, 0)),
            disposalArgument("Combined Sentence Period 'Y100'",
                DisposalDto.builder().disposalCode("1").sentencePeriod("Y100").build())
        );
    }

    @Override
    protected Stream<DisposalArgument> getFailingDisposalArgumentSource() {
        return Stream.of(
            disposalArgument("Incorrect sentence amount",
                DisposalDto.builder().disposalCode("1").sentenceAmount("101").sentencePeriod("Y").build()),
            disposalArgument("Incorrect sentence amount - From sentence Period",
                DisposalDto.builder().disposalCode("1").sentencePeriod("Y101").build())
        );
    }

    @Test
    void positiveGetDescriptionTest() {
        assertEquals("Sentence date must be EQUAL_TO 1",
            createRule(null, false, Comparator.EQUAL_TO, 1).getDescription(),
            "Description must match");
        assertEquals("Sentence date must be GREATER_THAN_OR_EQUAL_TO 4",
            createRule(null, false, Comparator.GREATER_THAN_OR_EQUAL_TO, 4).getDescription(),
            "Description must match");
        assertEquals("Sentence date must not be EQUAL_TO 1",
            createRule(null, true, Comparator.EQUAL_TO, 1).getDescription(),
            "Description must match");
        assertEquals("Sentence date must not be GREATER_THAN_OR_EQUAL_TO 4",
            createRule(null, true, Comparator.GREATER_THAN_OR_EQUAL_TO, 4).getDescription(),
            "Description must match");
    }

    @Override
    protected SentenceLengthRule createRule(Set<Integer> supportedCodes, boolean failOnPass) {
        return createRule(supportedCodes, failOnPass, defaultComparator, defaultValue);
    }

    private SentenceLengthRule createRule(Set<Integer> supportedCodes, boolean failOnPass, Comparator comparator,
                                          Integer value) {
        return new SentenceLengthRule(supportedCodes, failOnPass, comparator, value);
    }
}
