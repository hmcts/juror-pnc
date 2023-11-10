package uk.gov.hmcts.juror.pnc.check.rules.support;

public interface RuleSet<T> {
    Class<T> getSupportedClass();
}
