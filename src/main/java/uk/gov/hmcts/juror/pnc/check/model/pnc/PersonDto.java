package uk.gov.hmcts.juror.pnc.check.model.pnc;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class PersonDto {
    protected String pncId;
    protected String fileName;
    protected String dateOfBirth;
    protected String postCode;
    protected List<DisposalDto> disposals;
    protected Boolean onBail;
}
