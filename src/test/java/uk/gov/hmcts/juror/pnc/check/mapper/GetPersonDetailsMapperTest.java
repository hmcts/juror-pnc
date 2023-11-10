package uk.gov.hmcts.juror.pnc.check.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.config.Constants;
import uk.gov.hmcts.juror.pnc.check.config.PncConfig;
import uk.gov.hmcts.juror.pnc.check.config.RemoteConfig;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckBatch;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;
import uk.gov.hmcts.juror.pnc.check.model.NameDetails;
import uk.gov.hmcts.juror.pnc.check.model.PoliceNationalComputerCheckResult;
import uk.police.npia.juror.schema.v1.GetPersonDetails;
import uk.police.npia.juror.schema.v1.PNCAIHeaderType;
import uk.police.npia.juror.schema.v1.PncModeType;
import uk.police.npia.juror.schema.v1.PncTranCodeType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SuppressWarnings("PMD.TooManyMethods")
class GetPersonDetailsMapperTest {

    private static final String TIME_STRING = "2023101112:18:48";

    private static final String SEQUENCE_NUMBER_ERROR_CODE = "Sequence Numbers should increment";
    private RemoteConfig remoteConfig;

    private GetPersonDetailsMapper getPersonDetailsMapper;

    @BeforeEach
    void beforeEach() {
        this.remoteConfig = new RemoteConfig();
        PncConfig policeNationalComputerConfig = new PncConfig();
        policeNationalComputerConfig.setPncTerminal("myPncTerminal");
        policeNationalComputerConfig.setPncUserId("myPncUserId");
        policeNationalComputerConfig.setPncMode(PncModeType.DEMO);
        policeNationalComputerConfig.setPncAuthorisation("myPncAuthorisation");
        policeNationalComputerConfig.setPncTranCode(PncTranCodeType.LO);
        policeNationalComputerConfig.setOriginator("myOriginator");
        policeNationalComputerConfig.setReasonCode(1);
        policeNationalComputerConfig.setGatewayId("myGatewayId");
        policeNationalComputerConfig.setRequestLocation("myRequestLocation");
        policeNationalComputerConfig.setNamespace("myNamespace");

        policeNationalComputerConfig.setRequestMethod("myRequestMethod");
        policeNationalComputerConfig.setResponseMethod("myResponseMethod");


        this.remoteConfig.setPoliceNationalComputerService(policeNationalComputerConfig);
        long currentTimeMs = 1_697_023_128_048L;//2023-10-11 12:18:48 in ms
        Clock clock = Clock.fixed(Instant.ofEpochMilli(currentTimeMs), ZoneId.systemDefault());
        this.getPersonDetailsMapper = new GetPersonDetailsMapperImpl();
        this.getPersonDetailsMapper.setClock(clock);
        this.getPersonDetailsMapper.setRemoteConfig(remoteConfig);
    }


    @Test
    //False positive assert is done via internal method
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void mapJurorCheckRequestToGetPersonDetails() {
        JurorCheckDetails jurorCheckDetails = generateJurorCheckDetails();
        GetPersonDetails personDetails =
            getPersonDetailsMapper.mapJurorCheckRequestToGetPersonDetails(jurorCheckDetails);
        validate(jurorCheckDetails, personDetails);
    }

    @Test
    //False positive assert is done via internal method
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void mapJurorCheckRequestToGetPersonDetailsDefaultLocationCode() {
        JurorCheckDetails jurorCheckDetails = generateJurorCheckDetails();
        remoteConfig.getPoliceNationalComputerService().setRequestLocation(null);
        GetPersonDetails personDetails =
            getPersonDetailsMapper.mapJurorCheckRequestToGetPersonDetails(jurorCheckDetails);
        validate(jurorCheckDetails, personDetails);
    }

    @Test
    void mapJurorCheckRequestToGetPersonDetailsAfterMapping() {
        GetPersonDetails getPersonDetails = new GetPersonDetails();
        PncConfig config = remoteConfig.getPoliceNationalComputerService();
        config.setRequestLocation("MyLocation123");
        getPersonDetailsMapper.mapJurorCheckRequestToGetPersonDetailsAfterMapping(getPersonDetails);
        assertEquals("MyLocation123", getPersonDetails.getLocation(), "Location should match config");
        validate(config, getPersonDetails.getHeader());
    }

    @Test
    void mapJurorCheckRequestToGetPersonDetailsAfterMappingNullLocationCode() {
        GetPersonDetails getPersonDetails = new GetPersonDetails();
        PncConfig config = remoteConfig.getPoliceNationalComputerService();
        config.setRequestLocation(null);
        getPersonDetailsMapper.mapJurorCheckRequestToGetPersonDetailsAfterMapping(getPersonDetails);
        assertNull(getPersonDetails.getLocation(), "Location should be null");
        validate(config, getPersonDetails.getHeader());
    }


    @Test
    void positiveDateFormatter() {
        assertEquals("01012000", getPersonDetailsMapper.dateFormatter("01-01-2000"),
            "Date should have dash " + "removed");
    }

    @Test
    void negativeDateFormatterNullValue() {
        assertNull(getPersonDetailsMapper.dateFormatter(null), "Should return null");
    }


    @Test
    //False positive assert is done via internal method
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void mapSoapConfigToHeaderType() {
        PncConfig config = remoteConfig.getPoliceNationalComputerService();
        validate(config, getPersonDetailsMapper.mapSoapConfigToHeaderType(config));
    }

    @Test
    void mapCurrentDateTimeStr() {
        assertEquals(TIME_STRING, getPersonDetailsMapper.mapCurrentDateTimeStr(null),
            "Time string must match current time");
    }

    @Test
    void mapHeaderSequenceNumber() {
        assertEquals(1, getPersonDetailsMapper.mapHeaderSequenceNumber(null), SEQUENCE_NUMBER_ERROR_CODE);
        assertEquals(2, getPersonDetailsMapper.mapHeaderSequenceNumber(null), SEQUENCE_NUMBER_ERROR_CODE);
        assertEquals(3, getPersonDetailsMapper.mapHeaderSequenceNumber(null), SEQUENCE_NUMBER_ERROR_CODE);
        assertEquals(4, getPersonDetailsMapper.mapHeaderSequenceNumber(null), SEQUENCE_NUMBER_ERROR_CODE);
    }

    @Test
    void mapHeaderSequenceNumberTooLarge() {
        long maxValue = Long.MAX_VALUE - 100;
        assertEquals(1, getPersonDetailsMapper.mapHeaderSequenceNumber(null), SEQUENCE_NUMBER_ERROR_CODE);
        assertEquals(2, getPersonDetailsMapper.mapHeaderSequenceNumber(null), SEQUENCE_NUMBER_ERROR_CODE);
        getPersonDetailsMapper.getHeaderSequenceNumber().set(maxValue);
        assertEquals(maxValue, getPersonDetailsMapper.mapHeaderSequenceNumber(null), SEQUENCE_NUMBER_ERROR_CODE);
        assertEquals(1, getPersonDetailsMapper.mapHeaderSequenceNumber(null),
            "Sequence Numbers should reset back to 1 after max value is reached");
    }


    private JurorCheckDetails generateJurorCheckDetails() {
        return JurorCheckDetails.builder().jurorNumber("123456").dateOfBirth("01-01-2000").postCode("AA1 AA1")
            .name(NameDetails.builder().firstName("Ben").middleName("middlename").lastName("Edwards").build())
            .retryCount(0).result(PoliceNationalComputerCheckResult.passed()).batch(mock(JurorCheckBatch.class))
            .build();
    }

    private void validate(JurorCheckDetails jurorCheckDetails, GetPersonDetails personDetails) {
        String expectedLocation =
            Optional.ofNullable(remoteConfig.getPoliceNationalComputerService().getRequestLocation()).orElse("MOJ");
        validate(remoteConfig.getPoliceNationalComputerService(), personDetails.getHeader());
        assertEquals(expectedLocation, personDetails.getLocation(), "Location should match");
        assertEquals(jurorCheckDetails.getJurorNumber(), personDetails.getJurorReference(),
            "Juror reference should " + "match");
        assertEquals(getExpectedName(jurorCheckDetails.getName()), personDetails.getSearchName(), "Name should match");
        assertEquals(getExpectedDataOfBirth(jurorCheckDetails.getDateOfBirth()), personDetails.getSearchDateOfBirth(),
            "Date of birth should match");
        assertEquals(jurorCheckDetails.getPostCode(), personDetails.getPostCode(), "Post code should match");

    }

    private void validate(PncConfig config, PNCAIHeaderType header) {
        assertEquals(1, header.getSequenceNumber(), "Sequence Number should be first in sequence");
        assertEquals(TIME_STRING, header.getLocalDateTime(), "Time should be current time");
        assertEquals(config.getPncTerminal(), header.getPncTerminal(), "Pnc Terminal should match");
        assertEquals(config.getPncUserId(), header.getPncUserid(), "Pnc User Id should match");
        assertEquals(config.getPncMode().name(), header.getPncMode().name(), "Pnc Mode should match");
        assertEquals(config.getPncAuthorisation(), header.getPncAuthorisation(), "Pnc Authorisation should match");
        assertEquals(config.getPncTranCode().name(), header.getPncTranCode().name(), "Pnc Tran Code should match");
        assertEquals(config.getReasonCode(), header.getReasonCode(), "Reason code should match");
        assertEquals(config.getGatewayId(), header.getGatewayID(), "Gateway Id should match");
    }

    private String getExpectedDataOfBirth(String dateOfBirth) {
        String newDateOfBirth = dateOfBirth.replace("-", "");
        assertTrue(newDateOfBirth.matches("\\d{8}"), "DateOfBirth should be 8 digits");
        return newDateOfBirth;
    }

    private String getExpectedName(NameDetails name) {
        String combinedName = name.getLastName() + "/" + name.getFirstName();
        if (name.getMiddleName() != null) {
            combinedName += "/" + name.getMiddleName();
        }
        return combinedName.toUpperCase(Constants.LOCALE);
    }
}
