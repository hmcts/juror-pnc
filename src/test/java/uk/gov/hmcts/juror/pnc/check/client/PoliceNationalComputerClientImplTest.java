package uk.gov.hmcts.juror.pnc.check.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.mapper.PersonDetailsDtoMapper;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDetailsDto;
import uk.gov.hmcts.juror.standard.client.SoapWebServiceTemplate;
import uk.police.npia.juror.schema.v1.GetPersonDetails;
import uk.police.npia.juror.schema.v1.GetPersonDetailsResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class PoliceNationalComputerClientImplTest {

    private PoliceNationalComputerClientImpl client;
    private SoapWebServiceTemplate soapWebServiceTemplate;

    private PersonDetailsDtoMapper personDetailsDtoMapper;

    @BeforeEach
    void beforeEach() {
        this.personDetailsDtoMapper = mock(PersonDetailsDtoMapper.class);
        this.soapWebServiceTemplate = mock(SoapWebServiceTemplate.class);
        this.client = spy(new PoliceNationalComputerClientImpl(soapWebServiceTemplate, personDetailsDtoMapper));
    }

    @Test
    void positive() {
        GetPersonDetails request = new GetPersonDetails();
        GetPersonDetailsResponse expectedResponse = new GetPersonDetailsResponse();
        PersonDetailsDto personDetailsDto = new PersonDetailsDto();
        when(personDetailsDtoMapper.mapToPersonDetailsDto(expectedResponse)).thenReturn(personDetailsDto);

        when(soapWebServiceTemplate.call(request)).thenReturn(expectedResponse);
        assertEquals(personDetailsDto, client.call(request), "Response must match expected");
    }
}
