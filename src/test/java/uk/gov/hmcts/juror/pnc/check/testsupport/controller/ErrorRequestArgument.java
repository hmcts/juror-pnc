package uk.gov.hmcts.juror.pnc.check.testsupport.controller;

import io.jsonwebtoken.lang.Collections;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.juror.pnc.check.testsupport.TestUtil.jsonMatcher;

public class ErrorRequestArgument extends RequestArgument {
    private final String code;
    private final Collection<String> expectedErrorMessages;
    private final HttpStatus expectedStatus;

    public ErrorRequestArgument(HttpStatus expectedStatus, String requestPayload, String code,
                                String... expectedErrorMessages) {
        super(null, null, requestPayload);
        this.expectedStatus = expectedStatus;
        this.code = code;
        this.expectedErrorMessages = Set.of(expectedErrorMessages);
    }

    @Override
    public String toString() {
        return String.join(",",this.expectedErrorMessages);
    }

    @Override
    public void runPostActions(ResultActions resultActions, ControllerTest controllerTest) throws Exception {
        resultActions.andExpect(status().is(this.expectedStatus.value()))
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonMatcher(JSONCompareMode.NON_EXTENSIBLE,
                createErrorResponseString(this.code, this.expectedErrorMessages)));
        super.runPostActions(resultActions, controllerTest);
    }

    protected static String createErrorResponseString(String errorCode, Collection<String> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"code\":\"").append(errorCode).append('"');

        if (!Collections.isEmpty(messages)) {
            builder.append(",\"messages\": [");
            builder.append(messages.stream().map(s -> "\"" + s.replaceAll("\"", "\\\\\"") + "\"")
                .collect(Collectors.joining(",")));
            builder.append(']');
        }
        builder.append('}');
        return builder.toString();
    }
}
