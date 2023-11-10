package uk.gov.hmcts.juror.pnc.check.rules.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public class RuleValidationResult {

    private boolean passed;
    private String message;

    public static RuleValidationResult passed() {
        return RuleValidationResult.builder().passed(true).build();
    }

    public static RuleValidationResult failed(String message) {
        return RuleValidationResult.builder().passed(false).message(message).build();
    }
}
