package uk.gov.hmcts.juror.pnc.check.service.contracts;

import uk.gov.hmcts.juror.pnc.check.model.JurorCheckBatch;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;
import uk.gov.hmcts.juror.pnc.check.model.PoliceNationalComputerCheckResult;

public interface PoliceNationalComputerCheckService {
    PoliceNationalComputerCheckResult performPoliceCheck(
        JurorCheckDetails jurorCheckDetails);

    void reportResults(JurorCheckBatch jurorCheckBatch);
}
