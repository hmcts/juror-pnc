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
import uk.gov.hmcts.juror.pnc.check.client.contracts.JobExecutionServiceClient;
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
class JobExecutionServiceClientImplTest {

    @MockBean
    @ClientType("JobExecutionService")
    private RestTemplateBuilder restTemplateBuilder;
    @MockBean
    private RestTemplate restTemplate;
    @MockBean
    private ResponseEntity<Void> response;

    private JobExecutionServiceClientImpl jobExecutionServiceClient;


    @BeforeEach
    void beforeEach() {
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        jobExecutionServiceClient = new JobExecutionServiceClientImpl(restTemplateBuilder, TestConstants.URL);
    }

    @Test
    void positiveValidResponse() {
        JobExecutionServiceClient.StatusUpdatePayload payload = new JobExecutionServiceClient.StatusUpdatePayload(
            JobExecutionServiceClient.StatusUpdatePayload.Status.SUCCESS, "Message",null);
        when(restTemplate.exchange(eq(TestConstants.URL), eq(HttpMethod.PUT), any(), eq(Void.class),
            eq(TestConstants.JOB_KEY), eq(TestConstants.TASK_ID)))
            .thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.ACCEPTED);

        assertDoesNotThrow(
            () -> jobExecutionServiceClient.call(payload, TestConstants.JOB_KEY, TestConstants.TASK_ID)
        );
    }

    @Test
    void positiveInvalidResponse() {
        JobExecutionServiceClient.StatusUpdatePayload payload = new JobExecutionServiceClient.StatusUpdatePayload(
            JobExecutionServiceClient.StatusUpdatePayload.Status.SUCCESS, "Message",null);
        when(restTemplate.exchange(eq(TestConstants.URL), eq(HttpMethod.PUT), any(), eq(Void.class),
            eq(TestConstants.JOB_KEY), eq(TestConstants.TASK_ID)))
            .thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

        RemoteGatewayException exception = assertThrows(RemoteGatewayException.class,
            () -> jobExecutionServiceClient.call(payload, TestConstants.JOB_KEY, TestConstants.TASK_ID)
        );

        assertEquals("Call to JobExecutionServiceClient failed status code was: 404 NOT_FOUND",
            exception.getMessage(), "Wrong exception message");

    }
}
