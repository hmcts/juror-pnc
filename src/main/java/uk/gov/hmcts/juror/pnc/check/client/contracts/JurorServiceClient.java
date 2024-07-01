package uk.gov.hmcts.juror.pnc.check.client.contracts;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.juror.pnc.check.model.PoliceNationalComputerCheckResult;
import uk.gov.hmcts.juror.standard.client.contract.Client;

public interface JurorServiceClient extends Client {

    PoliceCheckStatusDto call(String jurorNumber, PoliceCheckStatusDto result);

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @EqualsAndHashCode
    class PoliceCheckStatusDto {
        private PoliceNationalComputerCheckResult.Status status;
    }
}

