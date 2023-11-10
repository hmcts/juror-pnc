package uk.gov.hmcts.juror.pnc.check.rules.disposal;

import lombok.Getter;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.rules.support.DisposalRule;
import uk.gov.hmcts.juror.pnc.check.utils.Utilities;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class AllowedDisposalCodesRule extends DisposalRule {

    private final Set<Integer> allowedCodes;

    public AllowedDisposalCodesRule(Set<Integer> allowedCodes, boolean failOnPass) {
        super(null, failOnPass);
        this.allowedCodes = Set.copyOf(Optional.ofNullable(allowedCodes).orElse(Collections.emptySet()));
    }


    @Override
    public String getDescription() {
        return "Disposal code must " + (this.failOnPass
            ? "not "
            : "") + "be one of " + allowedCodes.stream().sorted().map(String::valueOf)
            .collect(Collectors.joining(", "));
    }

    @Override
    public boolean execute(DisposalDto value) {
        if (value.getDisposalCode() == null) {
            return false;
        }
        Integer intValue = Utilities.getInteger(value.getDisposalCode());
        return allowedCodes.contains(intValue);
    }

}
