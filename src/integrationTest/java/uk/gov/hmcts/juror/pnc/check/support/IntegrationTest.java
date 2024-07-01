package uk.gov.hmcts.juror.pnc.check.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.xml.bind.JAXBElement;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.juror.pnc.check.client.contracts.JobExecutionServiceClient;
import uk.gov.hmcts.juror.pnc.check.client.contracts.JurorServiceClient;
import uk.gov.hmcts.juror.pnc.check.config.RemoteConfig;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckBatch;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckRequest;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckRequestBulk;
import uk.gov.hmcts.juror.pnc.check.model.NameDetails;
import uk.gov.hmcts.juror.pnc.check.model.PoliceNationalComputerCheckResult;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.service.QueueServiceImpl;
import uk.gov.hmcts.juror.standard.service.contracts.auth.JwtService;
import uk.police.npia.juror.schema.v1.Disposal;
import uk.police.npia.juror.schema.v1.GetPersonDetailsResponse;
import uk.police.npia.juror.schema.v1.Person;
import uk.police.npia.juror.schema.v1.PersonDetails;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingXPath;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.juror.pnc.check.support.TestConstants.NAMESPACE_PREFIX;
import static uk.gov.hmcts.juror.pnc.check.support.TestConstants.NAMESPACE_URL;
import static uk.gov.hmcts.juror.pnc.check.support.TestConstants.VALID_DATE_OF_BIRTH;
import static uk.gov.hmcts.juror.pnc.check.support.TestConstants.VALID_NAME;
import static uk.gov.hmcts.juror.pnc.check.support.TestConstants.VALID_POSTCODE;

@SuppressWarnings({
    "PMD.ExcessiveImports",
    "PMD.AvoidDuplicateLiterals",
    "PMD.LawOfDemeter",
    "PMD.AbstractClassWithoutAbstractMethod",
    "PMD.TooManyMethods"
})
@ActiveProfiles("integration")
public abstract class IntegrationTest {
    private static final Jaxb2Marshaller MARSHALLER;

    static {
        MARSHALLER = new Jaxb2Marshaller();
        MARSHALLER.setContextPath("uk.police.npia.juror.schema.v1");
    }

    @Autowired
    protected JwtService jwtService;

    @Autowired
    private QueueServiceImpl queueService;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected RemoteConfig config;

    @Value("${uk.gov.hmcts.juror.security.secret}")
    private String secret;
    private static final AtomicLong JUROR_NUMBER;

    static {
        JUROR_NUMBER = new AtomicLong(100_000_000);
    }


    protected void awaitQueueFinish() {
        queueService.getExecutorService().awaitQuiescence(TestConstants.MAX_TIME_OUT_SECONDS, TimeUnit.SECONDS);
        queueService.getBatchCheckExecutorService()
            .awaitQuiescence(TestConstants.MAX_TIME_OUT_SECONDS, TimeUnit.SECONDS);
    }

    @BeforeEach
    protected void beforeEach() {
        WireMock.stubFor(WireMock.patch(urlPathTemplate(config.getJurorService().getUrl()))
            .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.containing("application/json"))
            .willReturn(WireMock.jsonResponse(
                new JurorServiceClient.PoliceCheckStatusDto(PoliceNationalComputerCheckResult.Status.ELIGIBLE),
                    HttpStatus.ACCEPTED.value())
                .withHeader(HttpHeaders.CONNECTION, "close")));
    }

    protected void stubResponse(String jurorNumber, GetPersonDetailsResponse getPersonDetailsResponse) {
        String responseXml =
            "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap"
                + ".org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body>"
                + convertToXml(getPersonDetailsResponse)
                + "</SOAP-ENV:Body></SOAP-ENV:Envelope>";
        WireMock.stubFor(WireMock.post(config.getPoliceNationalComputerService().getUrl())
            .withHeader("Content-Type", WireMock.containing("text/xml"))
            .withBasicAuth(
                config.getPoliceNationalComputerService().getUsername(),
                config.getPoliceNationalComputerService().getPassword())
            .withRequestBody(matchingXPath(
                "/SOAP-ENV:Envelope/SOAP-ENV:Body/ns3:getPersonDetails/ns3:JurorReference[text() = '"
                    + jurorNumber + "']",
                Map.of("SOAP-ENV", "http://schemas.xmlsoap.org/soap/envelope/",
                    "ns3", "http://www.npia.police.uk/juror/schema/v1")))
            .willReturn(WireMock.ok()
                .withHeader("Content-Type", "text/xml")
                .withBody(responseXml)
                .withHeader(HttpHeaders.CONNECTION, "close")));
    }

    protected void verifyJurorServiceCall(String jurorNumber, PoliceNationalComputerCheckResult.Status status) {
        WireMock.verify(1, WireMock.patchRequestedFor(
                urlPathTemplate(config.getJurorService().getUrl()))
            .withPathParam("jurorNumber", equalTo(jurorNumber))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(WireMock.matchingJsonPath("$.status", equalTo(status.name()))));
    }

    protected <T> String convertToJson(T object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private static String convertToXml(GetPersonDetailsResponse getPersonDetailsResponse) {
        try (StringWriter sw = new StringWriter()) {
            Result result = new StreamResult(sw);
            MARSHALLER.marshal(getPersonDetailsResponse, result);
            return sw.toString().replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>", "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    protected String createValidJwt(String permission) {
        return jwtService.generateJwtToken("integration-test", "integration-test", "integration-test",
            30_000,
            Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)),
            Map.of("permissions", permission == null
                ? Collections.emptyList()
                : List.of(permission))
        );
    }

    protected MockHttpServletRequestBuilder buildCheckSingleRequest(String payload) {
        return
            MockMvcRequestBuilders.post("/jurors/check")
                .header(HttpHeaders.AUTHORIZATION,
                    "Bearer " + createValidJwt("pnc::check::single"))
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    private RequestBuilder buildCheckBulkRequest(String payload) {
        return
            MockMvcRequestBuilders.post("/jurors/check/bulk")
                .header(HttpHeaders.AUTHORIZATION,
                    "Bearer " + createValidJwt("pnc::check::bulk"))
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected void performValidSingle(JurorCheckRequest jurorCheckRequest,
                                      GetPersonDetailsResponse personResponse,
                                      PoliceNationalComputerCheckResult.Status expectedStatus) throws Exception {
        performValidSingle(jurorCheckRequest.getJurorNumber(), convertToJson(jurorCheckRequest), personResponse,
            expectedStatus);
    }

    protected void performValidSingle(String jurorNumber,
                                      String payload,
                                      GetPersonDetailsResponse personResponse,
                                      PoliceNationalComputerCheckResult.Status expectedStatus) throws Exception {
        stubResponse(jurorNumber, personResponse);
        preformAndValidateRequest(jurorNumber, payload, expectedStatus);
    }


    protected void preformAndValidateRequest(String jurorNumber,
                                             String payload,
                                             PoliceNationalComputerCheckResult.Status expectedStatus) throws Exception {
        mockMvc.perform(buildCheckSingleRequest(payload))
            .andExpect(status().isOk());
        awaitQueueFinish();
        verifyJurorServiceCall(jurorNumber, expectedStatus);
    }

    protected void preformAndValidateBulk(List<BulkRequest> bulkRequests) throws Exception {
        JurorCheckRequestBulk jurorCheckRequestBulk = new JurorCheckRequestBulk();
        jurorCheckRequestBulk.setChecks(bulkRequests.stream().map(BulkRequest::jurorCheckRequest).toList());
        jurorCheckRequestBulk.setMetaData(JurorCheckBatch.MetaData.builder()
            .taskId(TestConstants.TASK_ID)
            .jobKey(TestConstants.JOB_KEY)
            .build());
        preformAndValidateBulk(bulkRequests, jurorCheckRequestBulk);
    }

    protected void preformAndValidateBulk(List<BulkRequest> bulkRequests,
                                          JurorCheckRequestBulk jurorCheckRequestBulk) throws Exception {
        mockMvc.perform(buildCheckBulkRequest(convertToJson(jurorCheckRequestBulk)))
            .andExpect(status().isOk());
        awaitQueueFinish();
        verifyJurorServiceCalls(bulkRequests);
        verifyExecutionLayerCalls(bulkRequests, jurorCheckRequestBulk.getMetaData());
    }


    private void verifyJurorServiceCalls(List<BulkRequest> bulkRequests) {
        bulkRequests.forEach(bulkRequest ->
            verifyJurorServiceCall(
                bulkRequest.jurorCheckRequest().getJurorNumber(),
                bulkRequest.expectedStatus()));
    }

    private void verifyExecutionLayerCalls(List<BulkRequest> bulkRequests, JurorCheckBatch.MetaData metaData) {

        Map<PoliceNationalComputerCheckResult.Status, Long> countMap = bulkRequests.stream()
            .filter(bulkRequest -> bulkRequest.expectedStatus != null)
            .collect(Collectors.groupingBy(BulkRequest::expectedStatus, Collectors.counting()));

        for (PoliceNationalComputerCheckResult.Status status : PoliceNationalComputerCheckResult.Status.values()) {
            countMap.putIfAbsent(status, 0L);
        }

        RequestPatternBuilder builder =
            WireMock.putRequestedFor(urlPathTemplate(config.getJobExecutionService().getUrl()))
                .withPathParam("jobKey", equalTo(metaData.getJobKey()))
                .withPathParam("taskId", equalTo(metaData.getTaskId().toString()))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo("application/json"));

        JobExecutionServiceClient.StatusUpdatePayload.Status expectedStatus = countMap.entrySet().stream()
            .filter(entry -> entry.getKey() != PoliceNationalComputerCheckResult.Status.ELIGIBLE)
            .filter(entry -> entry.getKey() != PoliceNationalComputerCheckResult.Status.INELIGIBLE)
            .anyMatch(entry -> entry.getValue() > 0)
            ? JobExecutionServiceClient.StatusUpdatePayload.Status.INDETERMINATE
            : JobExecutionServiceClient.StatusUpdatePayload.Status.SUCCESS;

        builder.withRequestBody(
            WireMock.matchingJsonPath("$.status", equalTo(expectedStatus.name())));

        builder.withRequestBody(
            WireMock.matchingJsonPath("$.message", equalTo("Juror check completed.")));


        long nullRequestsCount =
            bulkRequests.stream().filter(bulkRequest -> bulkRequest.expectedStatus == null).count();

        builder.withRequestBody(
            WireMock.matchingJsonPath("$.meta_data.TOTAL_CHECKS_IN_BATCH",
                equalTo(String.valueOf(bulkRequests.size()))));
        builder.withRequestBody(
            WireMock.matchingJsonPath("$.meta_data.TOTAL_NULL_RESULTS", equalTo(String.valueOf(nullRequestsCount))));

        countMap.forEach((status, count) -> builder.withRequestBody(
            WireMock.matchingJsonPath("$.meta_data.TOTAL_WITH_STATUS_" + status.name(), equalTo(count.toString()))));

        int metaDataFieldCount = PoliceNationalComputerCheckResult.Status.values().length;
        metaDataFieldCount += 2; //Add 2 for TOTAL_CHECKS_IN_BATCH and TOTAL_NULL_RESULTS
        builder.withRequestBody(
            WireMock.matchingJsonPath("$.meta_data.size()", equalTo(String.valueOf(metaDataFieldCount))));

        WireMock.verify(1, builder);
    }


    protected GetPersonDetailsResponse createGetPersonDetailsResponse(
        JurorCheckRequest jurorCheckRequest,
        String errorReason,
        boolean onBail,
        List<Disposal> disposals
    ) {
        return createGetPersonDetailsResponse(
            jurorCheckRequest.getJurorNumber(),
            errorReason,
            List.of(
                createPersonFromRequest(jurorCheckRequest, onBail, disposals)
            )
        );
    }

    protected GetPersonDetailsResponse createGetPersonDetailsResponse(
        String jurorNumber,
        String errorReason,
        List<Person> people
    ) {
        GetPersonDetailsResponse getPersonDetailsResponse = new GetPersonDetailsResponse();
        if (errorReason != null) {
            getPersonDetailsResponse.setErrorReason(errorReason);
        }
        getPersonDetailsResponse.setJurorReference(jurorNumber);

        PersonDetails personDetails = createPersonDetails(people);
        getPersonDetailsResponse.setDetails(new JAXBElement<>(createQName("Details"),
            PersonDetails.class, personDetails));
        return getPersonDetailsResponse;
    }

    private PersonDetails createPersonDetails(List<Person> people) {
        PersonDetails personDetails = new PersonDetails();
        if (people != null) {
            personDetails.getPerson().addAll(people);
        }
        return personDetails;
    }

    protected JurorCheckRequest getTypicalJurorCheckRequest() {
        return getTypicalJurorCheckRequest(VALID_NAME);
    }

    protected JurorCheckRequest getTypicalJurorCheckRequest(NameDetails nameDetails) {
        return JurorCheckRequest.builder()
            .jurorNumber(getNewJurorNumber())
            .postCode(VALID_POSTCODE)
            .dateOfBirth(VALID_DATE_OF_BIRTH)
            .name(nameDetails)
            .build();
    }

    protected String getNewJurorNumber() {
        return String.valueOf(JUROR_NUMBER.getAndIncrement());
    }

    protected QName createQName(String tagName) {
        return new QName(NAMESPACE_URL, tagName, NAMESPACE_PREFIX);
    }

    protected Person createPersonFromRequest(JurorCheckRequest jurorCheckRequest,
                                             Boolean onBail,
                                             List<Disposal> disposals) {

        Person person = new Person();
        person.setDateOfBirth(
            new JAXBElement<>(createQName("DateOfBirth"), String.class, jurorCheckRequest.getDateOfBirth())
        );
        person.setPostCode(
            new JAXBElement<>(createQName("PostCode"), String.class, jurorCheckRequest.getPostCode())
        );
        if (onBail != null) {
            person.setOnBail(
                new JAXBElement<>(createQName("OnBail"), Boolean.class, onBail)
            );
        }
        if (disposals != null) {
            person.getDisposals().addAll(disposals);
        }
        return person;
    }


    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public Disposal createDisposal(DisposalDto disposalDto) {
        Disposal disposal = new Disposal();
        disposal.setDisposalCode(disposalDto.getDisposalCode());
        if (disposalDto.getSentenceAmount() != null) {
            disposal.setSentenceAmount(new JAXBElement<>(createQName("SentenceAmount"),
                String.class, disposalDto.getSentenceAmount()));
        }
        if (disposalDto.getSentencePeriod() != null) {
            disposal.setSentencePeriod(new JAXBElement<>(createQName("SentencePeriod"),
                String.class, disposalDto.getSentencePeriod()));
        }
        if (disposalDto.getDisposalEffectiveDate() != null) {
            disposal.setDisposalEffectiveDate(disposalDto.getDisposalEffectiveDate());
        }
        if (disposalDto.getQualLiteral() != null) {
            disposal.setQualLiteral(new JAXBElement<>(createQName("QualLiteral"),
                String.class, disposalDto.getQualLiteral()));
        }
        return disposal;
    }


    public record BulkRequest(JurorCheckRequest jurorCheckRequest, GetPersonDetailsResponse personResponse,
                              PoliceNationalComputerCheckResult.Status expectedStatus) {
    }

    protected BulkRequest createBulkRequestValid(JurorCheckRequest jurorCheckRequest) {
        GetPersonDetailsResponse personResponse =
            createGetPersonDetailsResponse(jurorCheckRequest, "", false, null);
        stubResponse(jurorCheckRequest.getJurorNumber(), personResponse);
        return new BulkRequest(jurorCheckRequest, personResponse,
            PoliceNationalComputerCheckResult.Status.ELIGIBLE);
    }


    protected BulkRequest createBulkRequestOnBail(JurorCheckRequest jurorCheckRequest) {
        GetPersonDetailsResponse personResponse = createGetPersonDetailsResponse(jurorCheckRequest, "", true, null);
        stubResponse(jurorCheckRequest.getJurorNumber(), personResponse);
        return new BulkRequest(jurorCheckRequest, personResponse,
            PoliceNationalComputerCheckResult.Status.INELIGIBLE);
    }
}
