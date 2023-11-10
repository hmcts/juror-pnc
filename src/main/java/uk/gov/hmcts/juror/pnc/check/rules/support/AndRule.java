package uk.gov.hmcts.juror.pnc.check.rules.support;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class AndRule extends DisposalRule {

    private final List<Rule<DisposalDto>> rules;

    public AndRule(Set<Integer> supportsCodes, boolean failOnPass) {
        super(supportsCodes, failOnPass);
        this.rules = new ArrayList<>();
    }

    public void addRule(Rule<DisposalDto> rule) {
        this.rules.add(rule);
    }


    @Override
    public boolean execute(DisposalDto value) {
        for (Rule<DisposalDto> rule : this.rules) {
            if (!rule.validate(value)) {
                return false;
            }
        }
        return true;
    }

    public List<Rule<DisposalDto>> getRules() {
        return Collections.unmodifiableList(this.rules);
    }

    @Override
    public String getDescription() {
        return this.rules.stream()
            .map(Rule::getDescription)
            .collect(Collectors.joining(" and "));
    }
}
