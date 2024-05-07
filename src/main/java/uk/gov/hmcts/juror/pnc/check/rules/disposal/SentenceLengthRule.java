package uk.gov.hmcts.juror.pnc.check.rules.disposal;

import lombok.Getter;
import lombok.ToString;
import org.apache.logging.log4j.util.Strings;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.rules.Comparator;
import uk.gov.hmcts.juror.pnc.check.rules.DateUnit;
import uk.gov.hmcts.juror.pnc.check.rules.support.DisposalRule;

import java.util.Set;

@Getter
@ToString(callSuper = true)
public class SentenceLengthRule extends DisposalRule {

    private final Comparator comparator;
    private final int value;
    private final DateUnit dateUnit;
    private final long days;

    public SentenceLengthRule(Set<Integer> supportsCodes, boolean failOnPass, Comparator comparator,
                              DateUnit dateUnit, Integer value) {
        super(supportsCodes, failOnPass);
        this.comparator = comparator;
        this.value = value;
        this.dateUnit = dateUnit;
        this.days = dateUnit.toDays(value);
    }

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    protected boolean execute(DisposalDto disposal) {
        String period = Strings.isNotBlank(disposal.getSentencePeriod())
            ? disposal.getSentencePeriod()
            : "Y";

        final String periodCode = period.substring(0, 1);
        DateUnit disposalDateUnit;
        if ("Y".equalsIgnoreCase(periodCode)) {
            disposalDateUnit = DateUnit.YEARS;
        } else if ("M".equalsIgnoreCase(periodCode)) {
            disposalDateUnit = DateUnit.MONTHS;
        } else if ("D".equalsIgnoreCase(periodCode)) {
            disposalDateUnit = DateUnit.DAYS;
        } else {
            return true;
        }
        String disposalDurationStr = Strings.isNotBlank(disposal.getSentenceAmount())
            ? disposal.getSentenceAmount()
            : "0";
        if (period.length() != 1) {
            disposalDurationStr = period.substring(1);
        }
        long disposalDuration = Long.parseLong(disposalDurationStr);
        return comparator.compare(disposalDateUnit.toDays(disposalDuration), days);
    }

    @Override
    public String getDescription() {
        return "Sentence date must " + (this.failOnPass
            ? "not "
            : "") + "be " + comparator.name() + " " + value + " " + dateUnit.name();
    }
}
