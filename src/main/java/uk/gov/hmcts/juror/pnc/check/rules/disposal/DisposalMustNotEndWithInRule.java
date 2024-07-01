package uk.gov.hmcts.juror.pnc.check.rules.disposal;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import uk.gov.hmcts.juror.pnc.check.config.Constants;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.rules.DateUnit;
import uk.gov.hmcts.juror.pnc.check.rules.support.DisposalRule;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

@Slf4j
@ToString(callSuper = true)
public class DisposalMustNotEndWithInRule extends DisposalRule {

    @ToString.Exclude
    private final DateFormat slashDateFormat = new SimpleDateFormat("dd/MM/yy", Constants.LOCALE);
    @ToString.Exclude
    private final DateFormat noSlashDateFormat = new SimpleDateFormat("ddMMyyyy", Constants.LOCALE);
    @ToString.Exclude
    @Getter
    private final Clock clock;
    @Getter
    private final DateUnit dateUnit;
    @Getter
    private final int value;

    public DisposalMustNotEndWithInRule(Set<Integer> supportsCodes, boolean failOnPass, Clock clock, int value,
                                        DateUnit dateUnit) {
        super(supportsCodes, failOnPass);
        this.clock = clock;
        this.dateUnit = dateUnit;
        this.value = value;
    }

    @Override
    protected boolean execute(DisposalDto disposal) {
        final Calendar effectiveEndOfSentenceCalendar = getEndOfSentence(disposal);
        final Calendar maximumAllowedEndDate = getMaximumAllowedEndDate();

        if (log.isDebugEnabled()) {
            log.debug("Sentence must end before: {}\nSentence ended at: {}",
                slashDateFormat.format(Date.from(maximumAllowedEndDate.toInstant())), slashDateFormat.format(
                    Date.from(effectiveEndOfSentenceCalendar.toInstant())));
        }
        return !effectiveEndOfSentenceCalendar.after(maximumAllowedEndDate);
    }

    @Override
    public String getDescription() {
        return "Disposal must " + (this.failOnPass
            ? "not "
            : "") + "have ended at least " + value + " " + dateUnit + " ago";
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private Calendar getEndOfSentence(DisposalDto disposal) {
        final Calendar effectiveDateCalendar = stringToCalendar(disposal.getDisposalEffectiveDate());
        final String sentencePeriod = Strings.isNotBlank(disposal.getSentencePeriod())
            ? disposal.getSentencePeriod()
            : "Y";

        String amountStr;
        if (sentencePeriod.length() == 1) {
            amountStr = disposal.getSentenceAmount();
        } else {
            amountStr = sentencePeriod.substring(1);
        }
        if (Strings.isEmpty(amountStr)) {
            amountStr = "1";
        }

        final String period = sentencePeriod.substring(0, 1);
        int amount = Integer.parseInt(amountStr);
        if ("d".equalsIgnoreCase(period)) {
            effectiveDateCalendar.add(Calendar.DAY_OF_YEAR, amount);
        } else if ("m".equalsIgnoreCase(period)) {
            effectiveDateCalendar.add(Calendar.MONTH, amount);
        } else if ("y".equalsIgnoreCase(period)) {
            effectiveDateCalendar.add(Calendar.YEAR, amount);
        }
        return effectiveDateCalendar;
    }

    private Calendar getMaximumAllowedEndDate() {
        Calendar calendar = stringToCalendar(noSlashDateFormat.format(Date.from(clock.instant())));
        calendar.add(dateUnit.getField(), -value);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        return calendar;
    }

    private Calendar stringToCalendar(String dateStr) {
        Date date;
        try {
            if (dateStr.contains("/")) {
                date = slashDateFormat.parse(dateStr);
            } else {
                date = noSlashDateFormat.parse(dateStr);
            }
        } catch (ParseException e) {
            log.error("stringToDate Error parsing a date {}", dateStr);
            throw new InternalServerException("Unable to parse date string: '" + dateStr + "'", e);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }
}
