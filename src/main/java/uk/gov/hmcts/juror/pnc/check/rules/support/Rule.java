package uk.gov.hmcts.juror.pnc.check.rules.support;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("PMD.ShortClassName")
@Getter
@ToString
public abstract class Rule<T> {

    protected final boolean failOnPass;

    protected Rule(boolean failOnPass) {
        this.failOnPass = failOnPass;
    }

    protected abstract boolean execute(T value);

    public abstract RuleSet<T> getRuleSet();

    public abstract String getDescription();

    public boolean validate(T value) {
        boolean result = execute(value);
        if (failOnPass) {
            result = !result;
        }
        log.debug("Rule: " + this.getName() + ": " + (result
            ? "PASSED"
            : "FAILED"));
        return result;
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }
}
