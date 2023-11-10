package uk.gov.hmcts.juror.pnc.check.rules.disposal;

import lombok.Getter;
import lombok.ToString;
import org.apache.logging.log4j.util.Strings;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.rules.Comparator;
import uk.gov.hmcts.juror.pnc.check.rules.support.DisposalRule;

import java.util.Set;

@Getter
@ToString(callSuper = true)
public class SentenceLengthRule extends DisposalRule {

    private final Comparator comparator;
    private final Integer value;

    public SentenceLengthRule(Set<Integer> supportsCodes, boolean failOnPass, Comparator comparator, Integer value) {
        super(supportsCodes, failOnPass);
        this.comparator = comparator;
        this.value = value;
    }

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    protected boolean execute(DisposalDto disposal) {
        String disposalDurationStr = Strings.isNotBlank(disposal.getSentenceAmount())
            ? disposal.getSentenceAmount()
            : "0";
        String period = Strings.isNotBlank(disposal.getSentencePeriod())
            ? disposal.getSentencePeriod()
            : "Y";

        final String periodCode = period.substring(0, 1);
        if ("Y".equals(periodCode)) {
            if (period.length() != 1) {
                disposalDurationStr = period.substring(1);
            }
            long disposalDuration = Long.parseLong(disposalDurationStr);
            return comparator.compare(disposalDuration, value);
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "Sentence date must " + (this.failOnPass
            ? "not "
            : "") + "be " + comparator.name() + " " + value;
    }
}
