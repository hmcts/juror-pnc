package uk.gov.hmcts.juror.pnc.check.config;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.pnc.check.rules.Comparator;
import uk.gov.hmcts.juror.pnc.check.rules.DateUnit;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Data
@ConfigurationProperties("uk.gov.hmcts.juror.pnc.check.rules")
public class RuleConfig {

    private List<ConditionalRule> conditionalRules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionalRule {
        private String description;
        private Set<Integer> whenCodeIsOneOf;

        List<Condition> conditions;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Condition {
            private boolean failOnPass;

            @Size(max = 1)
            private Map<Comparator, @Size(max = 1) Map<DateUnit, Integer>> sentenceLength;

            @Size(max = 1)
            private Map<DateUnit, Integer> disposalMustNotEndWithIn;

            private QualLiteralCheck qualLiteralCheck;

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            public static class QualLiteralCheck {
                private String expectedValue;
            }
        }
    }
}
