package uk.gov.hmcts.juror.pnc.check;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckRequest;
import uk.gov.hmcts.juror.pnc.check.model.NameDetails;
import uk.gov.hmcts.juror.pnc.check.model.PoliceNationalComputerCheckResult;
import uk.gov.hmcts.juror.pnc.check.support.IntegrationTest;
import uk.gov.hmcts.juror.pnc.check.support.TestConstants;
import uk.police.npia.juror.schema.v1.GetPersonDetailsResponse;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static uk.gov.hmcts.juror.pnc.check.support.TestConstants.VALID_NAME;

@WireMockTest(httpPort = TestConstants.WIRE_MOCK_PORT)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@SuppressWarnings({
    "PMD.LawOfDemeter",
    "PMD.JUnitTestsShouldIncludeAssert"//False positive done via inheritance
})
class EligibilityCheckBulkTest extends IntegrationTest {
    @BeforeEach
    @Override
    protected void beforeEach() {
        super.beforeEach();
        WireMock.stubFor(WireMock.put(urlPathTemplate(config.getJobExecutionService().getUrl()))
            .withHeader("Content-Type", WireMock.containing("application/json"))
            .willReturn(WireMock.status(HttpStatus.ACCEPTED.value())
                .withHeader(HttpHeaders.CONNECTION, "close")));
    }

    @Test
    void positiveMultipleJurorsTypical() throws Exception {
        List<BulkRequest> bulkRequests = List.of(
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenOne"))),
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenTwo"))),
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenThree"))),
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenFour"))),
            createBulkRequestOnBail(getTypicalJurorCheckRequest(createName("BenFive")))
        );
        preformAndValidateBulk(bulkRequests);
    }

    @Test
    void positiveMultipleJurorsAllPass() throws Exception {
        List<BulkRequest> bulkRequests = List.of(
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenOne"))),
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenTwo"))),
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenThree"))),
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenFour"))),
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenFive")))
        );
        preformAndValidateBulk(bulkRequests);
    }

    @Test
    void positiveConnectionIssues() throws Exception {
        List<BulkRequest> bulkRequests = new ArrayList<>(List.of(
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenOne"))),
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenTwo"))),
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenThree"))),
            createBulkRequestValid(getTypicalJurorCheckRequest(createName("BenFour")))
        ));

        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest(createName("BenFive"));
        GetPersonDetailsResponse personResponse =
            createGetPersonDetailsResponse(jurorCheckRequest, "", false, null);
        bulkRequests.add(new BulkRequest(jurorCheckRequest, personResponse,
            PoliceNationalComputerCheckResult.Status.ERROR_RETRY_CONNECTION_ERROR));
        preformAndValidateBulk(bulkRequests);
    }

    @Test
    void positiveMultipleJurorsMax() throws Exception {
        List<BulkRequest> bulkRequests = new ArrayList<>();
        for (int count = 1; count <= TestConstants.MAX_BULK_CHECKS; count++) {
            bulkRequests.add(createBulkRequestValid(getTypicalJurorCheckRequest(
                NameDetails.builder()
                    .firstName(RandomStringUtils.randomAlphabetic(50))
                    .middleName(RandomStringUtils.randomAlphabetic(250))
                    .lastName(RandomStringUtils.randomAlphabetic(50))
                    .build())));
        }
        preformAndValidateBulk(bulkRequests);
    }

    private NameDetails createName(String firstName) {
        return NameDetails.builder()
            .firstName(firstName)
            .middleName(VALID_NAME.getMiddleName())
            .lastName(VALID_NAME.getLastName()).build();
    }
}
