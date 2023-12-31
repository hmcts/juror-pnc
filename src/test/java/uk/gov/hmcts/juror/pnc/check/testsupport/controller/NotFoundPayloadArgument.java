package uk.gov.hmcts.juror.pnc.check.testsupport.controller;

import org.springframework.http.HttpStatus;

public class NotFoundPayloadArgument extends ErrorRequestArgument {
    public NotFoundPayloadArgument(String requestPayload) {
        super(HttpStatus.NOT_FOUND, requestPayload, "NOT_FOUND", "The requested resource could not be located.");
    }
}
