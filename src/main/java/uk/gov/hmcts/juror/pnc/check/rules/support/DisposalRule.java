package uk.gov.hmcts.juror.pnc.check.rules.support;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.utils.Utilities;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Getter
@ToString(callSuper = true)
public abstract class DisposalRule extends Rule<DisposalDto> {


    private final Set<Integer> supportedCodes;

    protected DisposalRule(Collection<Integer> supportedCodes, boolean failOnPass) {
        super(failOnPass);
        this.supportedCodes = Set.copyOf(Optional.ofNullable(supportedCodes).orElseGet(Collections::emptySet));
    }

    @Override
    public final RuleSet<DisposalDto> getRuleSet() {
        return RuleSets.DISPOSAL_RULE_SET;
    }


    @Override
    public boolean validate(DisposalDto value) {
        Integer disposalCode = Utilities.getInteger(value.getDisposalCode());
        if (!supportedCodes.isEmpty() && (disposalCode == null || !supportedCodes.contains(disposalCode))) {
            log.debug("Skipping rule: " + this.getName() + " disposal code not supported");
            return true;
        }
        return super.validate(value);
    }
}
