package uk.gov.hmcts.juror.pnc.check.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckRequest;
import uk.gov.hmcts.juror.pnc.check.model.NameDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JurorRequestMapperTest {

    private final JurorRequestMapper jurorRequestMapper;

    public JurorRequestMapperTest() {
        this.jurorRequestMapper = new JurorRequestMapperImpl();
    }

    private void validate(List<JurorCheckRequest> jurorCheckRequests, List<JurorCheckDetails> jurorCheckDetails) {
        assertEquals(jurorCheckRequests.size(), jurorCheckDetails.size(), "Lists must be same size");

        for (int index = 0; index < jurorCheckRequests.size(); index++) {
            validate(jurorCheckRequests.get(index), jurorCheckDetails.get(index));
        }
    }

    private void validate(JurorCheckRequest jurorCheckRequest, JurorCheckDetails jurorCheckDetails) {
        assertEquals(jurorCheckRequest.getJurorNumber(), jurorCheckDetails.getJurorNumber(),
            "Juror numbers must match");
        assertEquals(jurorCheckRequest.getDateOfBirth(), jurorCheckDetails.getDateOfBirth(),
            "Date of birth must match");
        assertEquals(jurorCheckRequest.getPostCode(), jurorCheckDetails.getPostCode(), "Postcode must match");

        NameDetails expectedName = jurorCheckRequest.getName();
        NameDetails actualName = jurorCheckDetails.getName();
        assertEquals(expectedName.getFirstName(), actualName.getFirstName(), "Firstname must match");
        assertEquals(expectedName.getMiddleName(), actualName.getMiddleName(), "Middle name must match");
        assertEquals(expectedName.getLastName(), actualName.getLastName(), "Last name must match");

    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")//False positive
    void mapJurorCheckRequestToJurorCheckDetailsSingleTest() {
        JurorCheckRequest jurorCheckRequest = JurorCheckRequest.builder()
            .jurorNumber("123456")
            .dateOfBirth("01-01-2000")
            .postCode("AA1 AA1")
            .name(NameDetails.builder()
                .firstName("Ben")
                .middleName("middlename")
                .lastName("Edwards")
                .build())
            .build();
        validate(jurorCheckRequest, jurorRequestMapper
            .mapJurorCheckRequestToJurorCheckDetails(jurorCheckRequest));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")//False positive
    void mapJurorCheckRequestToJurorCheckDetailsListTest() {
        List<JurorCheckRequest> jurorCheckRequests = List.of(
            JurorCheckRequest.builder()
                .jurorNumber("123456")
                .dateOfBirth("01-01-2000")
                .postCode("AA1 AA1")
                .name(NameDetails.builder()
                    .firstName("Ben")
                    .middleName("middlename")
                    .lastName("Edwards")
                    .build())
                .build(),
            JurorCheckRequest.builder()
                .jurorNumber("123")
                .dateOfBirth("01-01-2009")
                .postCode("TF2 TF2")
                .name(NameDetails.builder()
                    .firstName("Ben2")
                    .middleName("middlename2")
                    .lastName("Edwards2")
                    .build())
                .build()
        );
        validate(jurorCheckRequests,jurorRequestMapper.mapJurorCheckRequestToJurorCheckDetails(jurorCheckRequests));
    }
}
