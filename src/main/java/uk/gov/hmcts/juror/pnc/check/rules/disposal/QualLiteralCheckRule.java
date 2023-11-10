package uk.gov.hmcts.juror.pnc.check.rules.disposal;

import lombok.Getter;
import lombok.ToString;
import org.apache.logging.log4j.util.Strings;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.rules.support.DisposalRule;

import java.util.Set;

@Getter

@ToString(callSuper = true)
public class QualLiteralCheckRule extends DisposalRule {
    private final String expectedValue;

    public QualLiteralCheckRule(Set<Integer> supportsCodes, boolean failOnPass, String expectedValue) {
        super(supportsCodes, failOnPass);
        this.expectedValue = expectedValue;
    }

    @Override
    protected boolean execute(DisposalDto disposal) {
        final String qualLiteral = disposal.getQualLiteral();
        return doesQualLiteralHaveValue(qualLiteral, expectedValue);
    }

    private boolean doesQualLiteralHaveValue(String qualLiteral, String expectedValue) {
        if (Strings.isBlank(qualLiteral)) {
            return false;
        }
        return qualLiteral.contains(expectedValue);
    }

    @Override
    public String getDescription() {
        return "QualLiteral must " + (this.failOnPass
            ? "not "
            : "") + "contain value " + this.expectedValue;
    }
}
