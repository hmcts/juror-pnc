package uk.gov.hmcts.juror.pnc.check.service.contracts;

import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSet;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleValidationResult;

import java.util.Collection;

public interface RuleService {
    <T> RuleValidationResult fireRules(RuleSet<T> ruleSet, T value);

    <T> RuleValidationResult fireRules(RuleSet<T> ruleSet, Collection<T> value);
}
