package uk.gov.hmcts.juror.pnc.check.rules.disposal;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.config.Constants;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.rules.DateUnit;
import uk.gov.hmcts.juror.pnc.check.testsupport.AbstractDisposalRuleTest;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DisposalMustNotEndWithInRuleTest extends AbstractDisposalRuleTest<DisposalMustNotEndWithInRule> {

    private final Clock clock;
    private final int defaultValue;
    private final DateUnit defaultDateUnit;
    private final DateFormat slashDateFormat;
    private final DateFormat noSlashDateFormat;

    public DisposalMustNotEndWithInRuleTest() {
        super();
        this.clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        this.defaultValue = 5;
        this.defaultDateUnit = DateUnit.YEARS;
        this.slashDateFormat = new SimpleDateFormat("dd/MM/yy", Constants.LOCALE);
        this.noSlashDateFormat = new SimpleDateFormat("ddMMyyyy", Constants.LOCALE);

    }


    private class CalendarHelper {
        private final Calendar calendar;

        private CalendarHelper() {
            this.calendar = Calendar.getInstance();
            this.calendar.setTimeInMillis(clock.millis());
        }

        public CalendarHelper add(int field, int value) {
            this.calendar.add(field, value);
            return this;
        }

        public CalendarHelper subtract(int field, int value) {
            return add(field, -value);
        }

        public String toSlashDateString() {
            return slashDateFormat.format(this.calendar.getTime());
        }

        public String toNonSlashDateString() {
            return noSlashDateFormat.format(this.calendar.getTime());

        }
    }


    @Override
    protected Stream<DisposalArgument> getPassingDisposalArgumentSource() {
        Stream.Builder<DisposalArgument> builder = Stream.builder();
        CalendarHelper calendar10YearsOneDayAgo = new CalendarHelper().subtract(Calendar.YEAR, 10)
            .subtract(Calendar.DAY_OF_YEAR, 1);
        String dateStringFor10YearsOneDayAgo = calendar10YearsOneDayAgo.toSlashDateString();

        String dateStringFor2YearsOneDayAgo = new CalendarHelper().subtract(Calendar.YEAR, 2)
            .subtract(Calendar.DAY_OF_YEAR, 1).toSlashDateString();
        // disposalEffectiveDate is the date at which the disposal started as such we must take this into account
        // meaning if we add 10 years to today's date it will end exactly 5 years ago. (Plus one day as rule is
        // exclusive)

        //Effective date format
        builder.add(disposalArgument("5 Year Disposal which started 10 years 1 day ago."
                + " Must not end within 5 years (Effective Date has slash)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(calendar10YearsOneDayAgo.toSlashDateString())
                .sentencePeriod("y").sentenceAmount("5").build()));

        builder.add(disposalArgument("5 Year Disposal which started 10 years 1 day ago."
                + " Must not end within 5 years (Effective Date has slash)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(calendar10YearsOneDayAgo.toNonSlashDateString())
                .sentencePeriod("y").sentenceAmount("5").build()));
        //Sentence Period
        builder.add(disposalArgument("5 Year Disposal which started 10 years 1 day ago."
                + " Must not end within 5 years (Sentence Period null - Default to Y)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor10YearsOneDayAgo)
                .sentencePeriod(null).sentenceAmount("5").build()));

        builder.add(disposalArgument("5 Year Disposal which started 10 years 1 day ago."
                + " Must not end within 5 years (Sentence Period blank - Default to Y)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor10YearsOneDayAgo)
                .sentencePeriod("").sentenceAmount("5").build()));

        builder.add(disposalArgument("1 Year Disposal which started 2 years 1 day ago."
                + " Must not end within 1 years (Sentence Period is 'y')",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor2YearsOneDayAgo)
                .sentencePeriod("y").sentenceAmount("1").build(),
            (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, 1, DateUnit.YEARS)));

        builder.add(disposalArgument("1 Year Disposal which started 2 years 1 day ago."
                + " Must not end within 1 years (Sentence Period is 'm')",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor2YearsOneDayAgo)
                .sentencePeriod("m").sentenceAmount("12").build(),
            (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, 1, DateUnit.YEARS)));

        builder.add(disposalArgument("1 Year Disposal which started 2 years 1 day ago."
                + " Must not end within 1 years (Sentence Period is 'd')",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor2YearsOneDayAgo)
                .sentencePeriod("d").sentenceAmount("365").build(),
            (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, 1, DateUnit.YEARS)));


        //Sentence Amount
        builder.add(disposalArgument("1 Year Disposal which started 2 years 1 day ago."
                + " Must not end within 1 years (Sentence amount null - Default to 1)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor2YearsOneDayAgo)
                .sentencePeriod("y").sentenceAmount(null).build(),
            (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, 1, DateUnit.YEARS)));
        builder.add(disposalArgument("1 Year Disposal which started 2 years 1 day ago."
                + " Must not end within 1 years (Sentence amount blank - Default to 1)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor2YearsOneDayAgo)
                .sentencePeriod("y").sentenceAmount("").build(),
            (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, 1, DateUnit.YEARS)));

        //Combined sentence Period
        builder.add(disposalArgument("5 Year Disposal which started 10 years 1 day ago."
                + " Must not end within 5 years (Sentence Period combined - y5)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor10YearsOneDayAgo)
                .sentencePeriod("y5").build()));


        return builder.build();
    }


    @Override
    protected Stream<DisposalArgument> getFailingDisposalArgumentSource() {
        Stream.Builder<DisposalArgument> builder = Stream.builder();
        CalendarHelper calendar10YearsAgo = new CalendarHelper().subtract(Calendar.YEAR, 10);
        String dateStringFor10YearsAgo = calendar10YearsAgo.toSlashDateString();

        String dateStringFor2YearsAgo = new CalendarHelper().subtract(Calendar.YEAR, 2).toSlashDateString();
        // disposalEffectiveDate is the date at which the disposal started as such we must take this into account
        // meaning if we add 10 years to today's date it will end exactly 5 years ago. (Plus one day as rule is
        // exclusive)

        //Effective date format
        builder.add(disposalArgument("5 Year Disposal which started 10 years."
                + " Must not end within 5 years (Effective Date has slash)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(calendar10YearsAgo.toSlashDateString())
                .sentencePeriod("y").sentenceAmount("5").build()));

        builder.add(disposalArgument("5 Year Disposal which started 10 years."
                + " Must not end within 5 years (Effective Date has slash)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(calendar10YearsAgo.toNonSlashDateString())
                .sentencePeriod("y").sentenceAmount("5").build()));
        //Sentence Period
        builder.add(disposalArgument("5 Year Disposal which started 10 years."
                + " Must not end within 5 years (Sentence Period null - Default to Y)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor10YearsAgo)
                .sentencePeriod(null).sentenceAmount("5").build()));

        builder.add(disposalArgument("5 Year Disposal which started 10 years."
                + " Must not end within 5 years (Sentence Period blank - Default to Y)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor10YearsAgo)
                .sentencePeriod("").sentenceAmount("5").build()));

        builder.add(disposalArgument("1 Year Disposal which started 2 years."
                + " Must not end within 1 years (Sentence Period is 'y')",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor2YearsAgo)
                .sentencePeriod("y").sentenceAmount("1").build(),
            (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, 1, DateUnit.YEARS)));

        builder.add(disposalArgument("1 Year Disposal which started 2 years."
                + " Must not end within 1 years (Sentence Period is 'm')",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor2YearsAgo)
                .sentencePeriod("m").sentenceAmount("12").build(),
            (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, 1, DateUnit.YEARS)));

        builder.add(disposalArgument("1 Year Disposal which started 2 years."
                + " Must not end within 1 years (Sentence Period is 'd')",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor2YearsAgo)
                .sentencePeriod("d").sentenceAmount("365").build(),
            (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, 1, DateUnit.YEARS)));


        //Sentence Amount
        builder.add(disposalArgument("1 Year Disposal which started 2 years."
                + " Must not end within 1 years (Sentence amount null - Default to 1)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor2YearsAgo)
                .sentencePeriod("y").sentenceAmount(null).build(),
            (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, 1, DateUnit.YEARS)));
        builder.add(disposalArgument("1 Year Disposal which started 2 years."
                + " Must not end within 1 years (Sentence amount blank - Default to 1)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor2YearsAgo)
                .sentencePeriod("y").sentenceAmount("").build(),
            (supportedCodes, failOnPass) -> createRule(supportedCodes, failOnPass, 1, DateUnit.YEARS)));

        //Combined sentence Period
        builder.add(disposalArgument("5 Year Disposal which started 10 years."
                + " Must not end within 5 years (Sentence Period combined - y5)",
            DisposalDto.builder().disposalCode("1")
                .disposalEffectiveDate(dateStringFor10YearsAgo)
                .sentencePeriod("y5").build()));


        return builder.build();
    }

    @Test
    void positiveGetDescriptionTest() {
        assertEquals("Disposal must have ended at least 5 YEARS ago",
            createRule(null, false, 5, DateUnit.YEARS).getDescription(),
            "Description must match");
        assertEquals("Disposal must have ended at least 3 YEARS ago",
            createRule(null, false, 3, DateUnit.YEARS).getDescription(),
            "Description must match");
        assertEquals("Disposal must not have ended at least 5 YEARS ago",
            createRule(null, true, 5, DateUnit.YEARS).getDescription(),
            "Description must match");
        assertEquals("Disposal must not have ended at least 3 YEARS ago",
            createRule(null, true, 3, DateUnit.YEARS).getDescription(),
            "Description must match");
    }

    @Test
    @SuppressWarnings("java:S5778")
    void negativeInvalidEffectiveEndDate() {
        InternalServerException exception =
            assertThrows(InternalServerException.class,
                () -> createRule(null, true, 3, DateUnit.YEARS)
                    .execute(DisposalDto.builder().disposalCode("1").disposalEffectiveDate("INVALID").build()),
                "InternalServerException should be thrown when an invalid date string is entered");
        assertEquals("Unable to parse date string: 'INVALID'", exception.getMessage(), "Messages must match");
    }

    @Override
    protected DisposalMustNotEndWithInRule createRule(Set<Integer> supportedCodes, boolean failOnPass) {
        return createRule(supportedCodes, failOnPass, defaultValue, defaultDateUnit);
    }

    protected DisposalMustNotEndWithInRule createRule(Set<Integer> supportedCodes, boolean failOnPass,
                                                      int value, DateUnit dateUnit) {
        return new DisposalMustNotEndWithInRule(supportedCodes, failOnPass, clock, value, dateUnit);
    }
}
