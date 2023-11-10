package uk.gov.hmcts.juror.pnc.check.client.contracts;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import uk.gov.hmcts.juror.pnc.check.model.PoliceNationalComputerCheckResult;
import uk.gov.hmcts.juror.standard.client.contract.Client;

public interface JurorServiceClient extends Client {

    void call(String jurorNumber, Payload result);

    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    class Payload {
        private PoliceNationalComputerCheckResult.Status status;
    }
}
