package uk.gov.hmcts.juror.pnc.check.client.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.juror.standard.client.contract.Client;

import java.util.Map;

public interface JobExecutionServiceClient extends Client {

    void call(StatusUpdatePayload request, String jobKey, long taskId);

    @AllArgsConstructor
    @Getter
    class StatusUpdatePayload {
        private Status status;
        private String message;

        @JsonProperty("meta_data")
        private Map<String, String> metaData;

        @Getter
        public enum Status {
            PENDING,
            PROCESSING,
            VALIDATION_PASSED,
            VALIDATION_FAILED,
            FAILED_UNEXPECTED_EXCEPTION,
            SUCCESS,
            FAILED,
            INDETERMINATE;
        }
    }
}
