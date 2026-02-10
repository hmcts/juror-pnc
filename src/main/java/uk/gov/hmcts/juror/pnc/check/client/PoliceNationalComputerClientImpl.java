package uk.gov.hmcts.juror.pnc.check.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;
import uk.gov.hmcts.juror.pnc.check.client.contracts.PoliceNationalComputerClient;
import uk.gov.hmcts.juror.pnc.check.client.interceptor.SoapHttpLoggingInterceptor;
import uk.gov.hmcts.juror.pnc.check.mapper.PersonDetailsDtoMapper;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDetailsDto;
import uk.gov.hmcts.juror.standard.client.AbstractSoapClient;
import uk.gov.hmcts.juror.standard.client.SoapWebServiceTemplate;
import uk.gov.hmcts.juror.standard.client.contract.ClientType;
import uk.police.npia.juror.schema.v1.GetPersonDetails;

@Slf4j
@Component
public class PoliceNationalComputerClientImpl extends AbstractSoapClient implements PoliceNationalComputerClient {
    private final PersonDetailsDtoMapper personDetailsDtoMapper;

    @Autowired
    public PoliceNationalComputerClientImpl(
        @ClientType("PoliceNationalComputerClient")
        SoapWebServiceTemplate soapWebServiceTemplate,
        PersonDetailsDtoMapper personDetailsDtoMapper) {
        super(soapWebServiceTemplate);

        this.personDetailsDtoMapper = personDetailsDtoMapper;
        
        CloseableHttpClient httpClient = HttpClients.custom()
            .addInterceptorLast(new SoapHttpLoggingInterceptor())
            .build();

        HttpComponentsMessageSender messageSender = new HttpComponentsMessageSender();
        messageSender.setHttpClient(httpClient);
    }

    @Override
    public PersonDetailsDto call(GetPersonDetails request) {
        return personDetailsDtoMapper.mapToPersonDetailsDto(this.getSoapWebServiceTemplate().call(request));
    }
}
