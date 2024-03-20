package uk.gov.hmcts.juror.pnc.check.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.pnc.check.client.contracts.JobExecutionServiceClient;
import uk.gov.hmcts.juror.standard.client.AbstractRemoteRestClient;
import uk.gov.hmcts.juror.standard.client.contract.ClientType;
import uk.gov.hmcts.juror.standard.service.exceptions.RemoteGatewayException;

@Component
@Slf4j
public class JobExecutionServiceClientImpl extends AbstractRemoteRestClient implements JobExecutionServiceClient {

    private final String url;

    @Autowired
    public JobExecutionServiceClientImpl(
        @ClientType("JobExecutionService") RestTemplateBuilder restTemplateBuilder,
        @Value("${uk.gov.hmcts.juror.pnc.check.remote.job-execution-service.scheme}") String scheme,
        @Value("${uk.gov.hmcts.juror.pnc.check.remote.job-execution-service.host}") String host,
        @Value("${uk.gov.hmcts.juror.pnc.check.remote.job-execution-service.port}") String port,
        @Value("${uk.gov.hmcts.juror.pnc.check.remote.job-execution-service.url}") String url) {
        super(restTemplateBuilder);
        String urlPrefix = scheme + "://" + host + ":" + port;
        this.url = urlPrefix + url;
    }

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public void call(StatusUpdatePayload payload, String jobKey, long taskId) {
        HttpEntity<StatusUpdatePayload> requestUpdate = new HttpEntity<>(payload);
        ResponseEntity<Void> response =
            restTemplate.exchange(url, HttpMethod.PUT, requestUpdate, Void.class, jobKey, taskId);
        final HttpStatusCode statusCode = response.getStatusCode();
        if (!HttpStatus.ACCEPTED.equals(statusCode)) {
            throw new RemoteGatewayException("Call to JobExecutionServiceClient failed status code was: " + statusCode);
        }
    }
}
