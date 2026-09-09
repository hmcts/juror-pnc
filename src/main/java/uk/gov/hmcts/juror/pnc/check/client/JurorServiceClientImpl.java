package uk.gov.hmcts.juror.pnc.check.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.juror.pnc.check.client.contracts.JurorServiceClient;
import uk.gov.hmcts.juror.standard.client.contract.ClientType;
import uk.gov.hmcts.juror.standard.service.exceptions.RemoteGatewayException;

@Slf4j
@Component
public class JurorServiceClientImpl implements JurorServiceClient {

    private final RestTemplate restTemplate;
    private final String url;

    @Autowired
    protected JurorServiceClientImpl(
        @ClientType("JurorService") RestTemplateBuilder restTemplateBuilder,
        @Value("${uk.gov.hmcts.juror.pnc.check.remote.juror-service.scheme}") String scheme,
        @Value("${uk.gov.hmcts.juror.pnc.check.remote.juror-service.host}") String host,
        @Value("${uk.gov.hmcts.juror.pnc.check.remote.juror-service.port}") String port,
        @Value("${uk.gov.hmcts.juror.pnc.check.remote.juror-service.url}") String url) {
        this.restTemplate = restTemplateBuilder.build();
        String urlPrefix = scheme + "://" + host + ":" + port;
        this.url = urlPrefix + url;
    }

    @Override
    public PoliceCheckStatusDto call(String jurorNumber, PoliceCheckStatusDto payload) {
        log.debug("Updating juror: {} pnc check result on juror service backend", jurorNumber);
        HttpEntity<PoliceCheckStatusDto> requestUpdate = new HttpEntity<>(payload);
        ResponseEntity<PoliceCheckStatusDto> response =
            restTemplate.exchange(url, HttpMethod.PATCH, requestUpdate, PoliceCheckStatusDto.class, jurorNumber);
        final HttpStatusCode statusCode = response.getStatusCode();
        if (!statusCode.equals(HttpStatus.ACCEPTED)) {
            throw new RemoteGatewayException("Call to JurorServiceClient failed status code was: " + statusCode);
        }
        log.debug("Successfully updating juror: {} pnc check result on juror service backend", jurorNumber);
        return response.getBody();
    }
}
