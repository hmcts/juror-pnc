package uk.gov.hmcts.juror.pnc.check.rules.support;

import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDto;

public final class RuleSets {
    public static final RuleSet<PersonDto> PERSON_RULE_SET = () -> PersonDto.class;
    public static final RuleSet<DisposalDto> DISPOSAL_RULE_SET = () -> DisposalDto.class;

    private RuleSets() {

    }
}
