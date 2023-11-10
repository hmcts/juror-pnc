package uk.gov.hmcts.juror.pnc.check.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JurorCheckDetails {
    private String jurorNumber;
    private String dateOfBirth;
    private String postCode;
    private NameDetails name;

    private int retryCount;
    private PoliceNationalComputerCheckResult result;
    private JurorCheckBatch batch;

    public void setResult(PoliceNationalComputerCheckResult result) {
        boolean isOldResultNull = this.result == null;
        this.result = result;
        if (isOldResultNull && batch != null) {
            batch.incrementResultsCounter();
        }
    }

    public int getAndIncrementRetryCount() {
        return ++retryCount;
    }
}