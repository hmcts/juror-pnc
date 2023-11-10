package uk.gov.hmcts.juror.pnc.check.rules;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDto;
import uk.gov.hmcts.juror.pnc.check.rules.support.Rule;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSet;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSets;

@Component
public class NotOnBailRule extends Rule<PersonDto> {
    public NotOnBailRule() {
        this(false);
    }

    public NotOnBailRule(boolean failOnPass) {
        super(failOnPass);
    }

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public boolean execute(PersonDto person) {
        return person.getOnBail() == null || Boolean.FALSE.equals(person.getOnBail());
    }

    @Override
    public RuleSet<PersonDto> getRuleSet() {
        return RuleSets.PERSON_RULE_SET;
    }

    @Override
    public String getDescription() {
        return "Juror must " + (isFailOnPass()
            ? ""
            : "not ") + "be on bail.";
    }
}
