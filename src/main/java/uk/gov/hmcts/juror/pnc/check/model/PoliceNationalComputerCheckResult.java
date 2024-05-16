package uk.gov.hmcts.juror.pnc.check.model;

import lombok.Data;

@Data
public class PoliceNationalComputerCheckResult {
    private Status status;
    private String message;
    private boolean isMaxRetiresExceed;

    public PoliceNationalComputerCheckResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public PoliceNationalComputerCheckResult(Status status) {
        this(status, null);
    }

    public static PoliceNationalComputerCheckResult passed() {
        return new PoliceNationalComputerCheckResult(Status.ELIGIBLE);
    }

    @Override
    public String toString() {
        return "Result: " + this.status + " Message: " + this.message;
    }

    public enum Status {
        ELIGIBLE,
        INELIGIBLE,
        ERROR_RETRY_NAME_HAS_NUMERICS,
        ERROR_RETRY_CONNECTION_ERROR,
        ERROR_RETRY_OTHER_ERROR_CODE,
        ERROR_RETRY_NO_ERROR_REASON,
        ERROR_RETRY_UNEXPECTED_EXCEPTION,
        ERROR_RETRY_FAILED_TO_UPDATE_BACKEND,
        UNCHECKED_MAX_RETRIES_EXCEEDED;
    }
}
