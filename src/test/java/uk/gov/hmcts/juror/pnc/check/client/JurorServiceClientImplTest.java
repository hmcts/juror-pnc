package uk.gov.hmcts.juror.pnc.check.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.juror.pnc.check.client.contracts.JurorServiceClient;
import uk.gov.hmcts.juror.pnc.check.model.PoliceNationalComputerCheckResult;
import uk.gov.hmcts.juror.pnc.check.testsupport.TestConstants;
import uk.gov.hmcts.juror.standard.client.contract.ClientType;
import uk.gov.hmcts.juror.standard.service.exceptions.RemoteGatewayException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class})
class JurorServiceClientImplTest {

    @MockBean
    @ClientType("JurorService")
    private RestTemplateBuilder restTemplateBuilder;
    @MockBean
    private RestTemplate restTemplate;
    @MockBean
    private ResponseEntity<Void> response;

    private JurorServiceClientImpl jurorServiceClient;

    private static final String SCHEME = "https";
    private static final String HOST = "localhost";
    private static final String PORT = "8080";
    private static final String URL_PREFIX = SCHEME + "://" + HOST + ":" + PORT;
    private static final String URL_SUFFIX = "/api/v1/moj/juror-record/pnc/{jurorNumber}";

    private static final String URL = URL_PREFIX + URL_SUFFIX;

    @BeforeEach
    void beforeEach() {
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        jurorServiceClient = new JurorServiceClientImpl(restTemplateBuilder,
            SCHEME, HOST, PORT, URL_SUFFIX);
    }

    @Test
    void positiveValidResponse() {
        JurorServiceClient.Payload payload = new JurorServiceClient.Payload(
            PoliceNationalComputerCheckResult.Status.ELIGIBLE);

        when(restTemplate.exchange(eq(URL), eq(HttpMethod.PATCH), any(), eq(Void.class),
            eq(TestConstants.JUROR_NUMBER)))
            .thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.ACCEPTED);

        assertDoesNotThrow(
            () -> jurorServiceClient.call(TestConstants.JUROR_NUMBER, payload)
        );
    }

    @Test
    void positiveInvalidResponse() {
        JurorServiceClient.Payload payload = new JurorServiceClient.Payload(
            PoliceNationalComputerCheckResult.Status.ELIGIBLE);
        when(restTemplate.exchange(eq(URL), eq(HttpMethod.PATCH), any(), eq(Void.class),
            eq(TestConstants.JUROR_NUMBER)))
            .thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

        RemoteGatewayException exception = assertThrows(RemoteGatewayException.class,
            () -> jurorServiceClient.call(TestConstants.JUROR_NUMBER, payload)
        );

        assertEquals("Call to JurorServiceClient failed status code was: 404 NOT_FOUND",
            exception.getMessage(), "Wrong exception message");

    }
}
