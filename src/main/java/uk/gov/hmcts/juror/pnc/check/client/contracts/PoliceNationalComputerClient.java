package uk.gov.hmcts.juror.pnc.check.client.contracts;

import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDetailsDto;
import uk.gov.hmcts.juror.standard.client.contract.Client;
import uk.police.npia.juror.schema.v1.GetPersonDetails;

public interface PoliceNationalComputerClient extends Client {

    PersonDetailsDto call(GetPersonDetails request);
}
