package uk.gov.hmcts.juror.pnc.check.rules.disposal;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.rules.Comparator;
import uk.gov.hmcts.juror.pnc.check.rules.DateUnit;
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
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.EQUAL_TO,
                    DateUnit.YEARS, 0)),
            disposalArgument("Sentence Amount null - Should default to '0'",
                DisposalDto.builder().disposalCode("1").sentenceAmount(null).sentencePeriod("Y").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.EQUAL_TO,
                    DateUnit.YEARS, 0)),
            disposalArgument("Combined Sentence Period 'Y100'",
                DisposalDto.builder().disposalCode("1").sentencePeriod("Y100").build()),


            disposalArgument("Combined Sentence Period 'M23' - Year",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("M23").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.YEARS, 2)),
            disposalArgument("Combined Sentence Period 'M23' - Months",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("M23").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.MONTHS, 24)),
            disposalArgument("Combined Sentence Period 'M10' - Days",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("M10").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.DAYS, 310)),


            disposalArgument("Combined Sentence Period 'D366' - Year",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("D366").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.YEARS, 1)),
            disposalArgument("Combined Sentence Period 'D62' - Months",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("D62").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.MONTHS, 2)),
            disposalArgument("Combined Sentence Period 'D10' - Days",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("D10").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.DAYS, 10))
        );
    }

    @Override
    protected Stream<DisposalArgument> getFailingDisposalArgumentSource() {
        return Stream.of(
            disposalArgument("Incorrect sentence amount",
                DisposalDto.builder().disposalCode("1").sentenceAmount("101").sentencePeriod("Y").build()),
            disposalArgument("Incorrect sentence amount - From sentence Period",
                DisposalDto.builder().disposalCode("1").sentencePeriod("Y101").build()),

            disposalArgument("Combined Sentence Period 'M23' - Year",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("M25").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.YEARS, 2)),
            disposalArgument("Combined Sentence Period 'M23' - Months",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("M25").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.MONTHS, 24)),
            disposalArgument("Combined Sentence Period 'M10' - Days",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("M10").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.DAYS, 309)),


            disposalArgument("Combined Sentence Period 'D366' - Year",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("D367").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.YEARS, 1)),
            disposalArgument("Combined Sentence Period 'D62' - Months",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("D63").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.MONTHS, 2)),
            disposalArgument("Combined Sentence Period 'D10' - Days",
                DisposalDto.builder().disposalCode("1").sentenceAmount("").sentencePeriod("D11").build(),
                (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, Comparator.LESS_THAN_OR_EQUAL_TO,
                    DateUnit.DAYS, 10))

        );
    }

    @Test
    void positiveGetDescriptionTest() {
        assertEquals("Sentence date must be EQUAL_TO 1 YEARS",
            createRule(null, false, Comparator.EQUAL_TO, DateUnit.YEARS, 1).getDescription(),
            "Description must match");
        assertEquals("Sentence date must be GREATER_THAN_OR_EQUAL_TO 4 MONTHS",
            createRule(null, false, Comparator.GREATER_THAN_OR_EQUAL_TO, DateUnit.MONTHS, 4).getDescription(),
            "Description must match");
        assertEquals("Sentence date must not be EQUAL_TO 1 DAYS",
            createRule(null, true, Comparator.EQUAL_TO, DateUnit.DAYS, 1).getDescription(),
            "Description must match");
        assertEquals("Sentence date must not be GREATER_THAN_OR_EQUAL_TO 4 YEARS",
            createRule(null, true, Comparator.GREATER_THAN_OR_EQUAL_TO, DateUnit.YEARS, 4).getDescription(),
            "Description must match");
    }

    @Override
    protected SentenceLengthRule createRule(Set<Integer> supportedCodes, boolean failOnPass) {
        return createRule(supportedCodes, failOnPass, defaultComparator, DateUnit.YEARS, defaultValue);
    }

    private SentenceLengthRule createRule(Set<Integer> supportedCodes, boolean failOnPass, Comparator comparator,
                                          DateUnit dateUnit, Integer value) {
        return new SentenceLengthRule(supportedCodes, failOnPass, comparator, dateUnit, value);
    }
}
