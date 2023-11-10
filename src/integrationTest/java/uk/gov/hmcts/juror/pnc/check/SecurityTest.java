package uk.gov.hmcts.juror.pnc.check;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckRequestBulk;
import uk.gov.hmcts.juror.pnc.check.support.IntegrationTest;
import uk.gov.hmcts.juror.pnc.check.support.TestConstants;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WireMockTest(httpPort = TestConstants.WIRE_MOCK_PORT)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@SuppressWarnings({
    "PMD.JUnitTestsShouldIncludeAssert"//False positive
})
class SecurityTest extends IntegrationTest {

    private static final String UNAUTHORISED_PAYLOAD =
        "{\"code\":\"UNAUTHORISED\",\"messages\":[\"You are not authorised\"]}";

    @Test
    void unauthorisedSingle() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(TestConstants.SINGLE_URL)
                .header(HttpHeaders.AUTHORIZATION,
                    "Bearer " + createValidJwt(null))
                .content(convertToJson(getTypicalJurorCheckRequest()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(content().json(UNAUTHORISED_PAYLOAD, true));
    }

    @Test
    void unauthorisedBulk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(TestConstants.BULK_URL)
                .header(HttpHeaders.AUTHORIZATION,
                    "Bearer " + createValidJwt(null))
                .content(convertToJson(JurorCheckRequestBulk.builder()
                    .checks(List.of(getTypicalJurorCheckRequest()))
                    .build()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(content().json(UNAUTHORISED_PAYLOAD, true));
    }


}
