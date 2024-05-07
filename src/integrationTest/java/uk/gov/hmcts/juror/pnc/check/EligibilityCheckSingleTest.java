package uk.gov.hmcts.juror.pnc.check;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckRequest;
import uk.gov.hmcts.juror.pnc.check.model.NameDetails;
import uk.gov.hmcts.juror.pnc.check.model.PoliceNationalComputerCheckResult;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.support.IntegrationTest;
import uk.gov.hmcts.juror.pnc.check.support.TestConstants;
import uk.police.npia.juror.schema.v1.GetPersonDetailsResponse;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.juror.pnc.check.support.TestConstants.VALID_DATE_OF_BIRTH;
import static uk.gov.hmcts.juror.pnc.check.support.TestConstants.VALID_POSTCODE;

@WireMockTest(httpPort = TestConstants.WIRE_MOCK_PORT)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@SuppressWarnings({
    "PMD.JUnitTestsShouldIncludeAssert",//False positive validated by WireMock
    "PMD.AvoidDuplicateLiterals",
    "PMD.TooManyMethods",
    "PMD.GodClass"
})
class EligibilityCheckSingleTest extends IntegrationTest {

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("ELIGIBLE: Straight forward")
    void eligibleNotOnBail() throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, null),
            PoliceNationalComputerCheckResult.Status.ELIGIBLE
        );
    }

    @Test
    @DisplayName("ELIGIBLE: Multiple valid disposals")
    void eligibleMultipleDisposals() throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder().disposalCode("1").build()),
                    createDisposal(DisposalDto.builder().disposalCode("2").build()),
                    createDisposal(DisposalDto.builder().disposalCode("3").build())
                )),
            PoliceNationalComputerCheckResult.Status.ELIGIBLE
        );
    }

    @Test
    @DisplayName("INELIGIBLE: Multiple disposals last one invalid")
    void ineligibleMultipleDisposalsLastOneInvalid() throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder().disposalCode("1").build()),
                    createDisposal(DisposalDto.builder().disposalCode("2").build()),
                    createDisposal(DisposalDto.builder().disposalCode("4027").build())
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    @Test
    @DisplayName("INELIGIBLE: Multiple people last one invalid for onBail")
    void ineligibleMultiplePeopleLastOneInvalidOnBail() throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest.getJurorNumber(),
                "", List.of(
                    createPersonFromRequest(jurorCheckRequest, false, List.of(
                        createDisposal(DisposalDto.builder().disposalCode("1").build()))),
                    createPersonFromRequest(jurorCheckRequest, false, List.of(
                        createDisposal(DisposalDto.builder().disposalCode("1").build()))),
                    createPersonFromRequest(jurorCheckRequest, true, List.of(
                        createDisposal(DisposalDto.builder().disposalCode("1").build())))

                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    @Test
    @DisplayName("INELIGIBLE: Multiple people last one invalid for disposal code")
    void ineligibleMultiplePeopleLastOneInvalidDisposalCode() throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest.getJurorNumber(),
                "", List.of(
                    createPersonFromRequest(jurorCheckRequest, false, List.of(
                        createDisposal(DisposalDto.builder().disposalCode("1").build()))),
                    createPersonFromRequest(jurorCheckRequest, false, List.of(
                        createDisposal(DisposalDto.builder().disposalCode("1").build()))),
                    createPersonFromRequest(jurorCheckRequest, false, List.of(
                        createDisposal(DisposalDto.builder().disposalCode("4016").build())))

                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Disposal code {0} is automatically ineligible")
    @ValueSource(strings = {
        "1003", "1091", "4011", "4012", "4013", "4014", "4015", "4016", "4017", "4023", "4027", "4028"
    })
    void ineligibleCodes(String disposalCode) throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder().disposalCode(disposalCode).build())
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    @Test
    @DisplayName("ERROR_RETRY_CONNECTION_ERROR: Remote gateway exception")
    void remoteGatewayException() throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        preformAndValidateRequest(jurorCheckRequest.getJurorNumber(), convertToJson(jurorCheckRequest),
            PoliceNationalComputerCheckResult.Status.ERROR_RETRY_CONNECTION_ERROR);
    }

    @Test
    @DisplayName("ERROR_RETRY_NO_ERROR_REASON: Error Reason is null")
    void errorReasonIsNull() throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                null, true, null),
            PoliceNationalComputerCheckResult.Status.ERROR_RETRY_NO_ERROR_REASON
        );
    }

    @Test
    @DisplayName("ERROR_RETRY_NAME_HAS_NUMERICS: Name has numerics")
    void nameHasNumerics() throws Exception {
        JurorCheckRequest jurorCheckRequest = JurorCheckRequest.builder()
            .jurorNumber(getNewJurorNumber())
            .postCode(VALID_POSTCODE)
            .dateOfBirth(VALID_DATE_OF_BIRTH)
            .name(NameDetails.builder()
                .firstName("Ben10")
                .middleName("Someone")
                .lastName("Edwards")
                .build())
            .build();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                null, false, null),
            PoliceNationalComputerCheckResult.Status.ERROR_RETRY_NAME_HAS_NUMERICS
        );
    }

    @Test
    @DisplayName("ERROR_RETRY_OTHER_ERROR_CODE: Invalid name")
    void invalidName() throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        jurorCheckRequest.getName().setFirstName("Ben-");

        GetPersonDetailsResponse response = createGetPersonDetailsResponse(jurorCheckRequest,
            null, false, null);
        response.setErrorReason("JUR003 - Failed to reach PNC host. Please contact PNC service Desk. Juror Id "
            + jurorCheckRequest.getJurorNumber());

        performValidSingle(
            jurorCheckRequest,
            response,
            PoliceNationalComputerCheckResult.Status.ERROR_RETRY_OTHER_ERROR_CODE
        );
    }

    @Test
    @DisplayName("ELIGIBLE: Error Reason starts with 'JUR001 - No Records Found:'")
    @SuppressWarnings("AbbreviationAsWordInName")
    void eligibleErrorReasonStartsWithJUR001() throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "JUR001 - No Records Found: abc", false, null),
            PoliceNationalComputerCheckResult.Status.ELIGIBLE
        );
    }

    @Test
    @DisplayName("ERROR_RETRY_OTHER_ERROR_CODE: Error Reason starts with something other then 'JUR001 - No Records "
        + "Found:'")
    @SuppressWarnings("AbbreviationAsWordInName")
    void otherErrorCodeErrorReasonDoesNotStartWithJUR001() throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "Some error", false, null),
            PoliceNationalComputerCheckResult.Status.ERROR_RETRY_OTHER_ERROR_CODE
        );
    }

    @Test
    @DisplayName("INELIGIBLE: On Bail")
    void ineligibleOnBail() throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", true, null),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    //Condition 1
    @ParameterizedTest(name = "INELIGIBLE: Sentenced to life (imprisonment, detention and custody for life) and "
        + "sentence amount is 999. Disposal code {0}")
    @ValueSource(strings = {
        "1002", "1006", "1007", "1022", "1024", "1092"
    })
    void sentenceToLifeSentenceAmount999(String disposalCode) throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .sentenceAmount("999")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(LocalDate.now(clock).minusYears(20)))
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    //Condition 1
    @ParameterizedTest(name = "ELIGIBLE: Sentenced to life (imprisonment, detention and custody for life) and "
        + "sentence amount is 12 months. Disposal code {0}")
    @ValueSource(strings = {
        "1002", "1006", "1007", "1022", "1024", "1092"
    })
    void sentenceToLifeSentenceAmount12Months(String disposalCode) throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .sentenceAmount("12")
                        .sentencePeriod("M")
                        .build()
                        .disposalEffectiveDate(LocalDate.now(clock).minusYears(20)))
                )),
            PoliceNationalComputerCheckResult.Status.ELIGIBLE
        );
    }

    //Condition 2
    @ParameterizedTest(name = "INELIGIBLE: Sentenced to 5 years or more, sentence amount is 5 years or more."
        + "Sentence Length 5 years. Disposal code {0}.")
    @ValueSource(strings = {
        "1002", "1006", "1007", "1022", "1024", "1092"
    })
    void sentenceTo5OrMoreYears5(String disposalCode) throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(

                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .sentenceAmount("5")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(LocalDate.now(clock).minusYears(20)))
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    //Condition 2
    @ParameterizedTest(name = "ELIGIBLE: Sentenced to 5 years or more, sentence amount is 5 years or more."
        + "Sentence Length 4 years. Disposal code {0}.")
    @ValueSource(strings = {
        "1002", "1006", "1007", "1022", "1024", "1092"
    })
    void sentenceTo5OrMoreYears4(String disposalCode) throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(

                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .sentenceAmount("4")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(LocalDate.now(clock).minusYears(20)))
                )),
            PoliceNationalComputerCheckResult.Status.ELIGIBLE
        );
    }

    //Condition 2
    @ParameterizedTest(name = "ELIGIBLE: Sentenced to 5 years or more, sentence amount is 5 years or more."
        + "Sentence Length 12 months. Disposal code {0}.")
    @ValueSource(strings = {
        "1002", "1006", "1007", "1022", "1024", "1092"
    })
    void sentenceTo5OrMoreYears12Months(String disposalCode) throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(

                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .sentenceAmount("12")
                        .sentencePeriod("M")
                        .build()
                        .disposalEffectiveDate(LocalDate.now(clock).minusYears(20)))
                )),
            PoliceNationalComputerCheckResult.Status.ELIGIBLE
        );
    }

    //Condition 2
    @ParameterizedTest(name = "INELIGIBLE: Sentenced to 5 years or more, sentence amount is 5 years or more."
        + "Sentence Length 6 years. Disposal code {0}.")
    @ValueSource(strings = {
        "1002", "1006", "1007", "1022", "1024", "1092"
    })
    void sentenceTo5OrMoreYears6(String disposalCode) throws Exception {
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .sentenceAmount("6")
                        .sentencePeriod("Y")
                        .build())
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    //Condition 3
    @ParameterizedTest(name =
        "INELIGIBLE: Sentence of imprisonment or detention in last 10 Years and sentence ended in last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Disposal code {0}.")
    @ValueSource(strings = {
        "1002", "1006", "1007", "1022", "1024", "1081", "1092", "1096", "1114"
    })
    void sentenceOfImprisonmentIneligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(disposalCode);
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Sentence of imprisonment or detention in last 10 Years and sentence ended in last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Sentence ended today. Disposal "
            + "code {0}.")
    @ValueSource(strings = {
        "1002", "1006", "1007", "1022", "1024", "1081", "1092", "1096", "1114"
    })
    void sentenceOfImprisonmentIneligible10YearSentenceStarted10YearsAgoAndEndedToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted10YearsAgoAndEndedToday(disposalCode);
    }

    @ParameterizedTest(name =
        "ELIGIBLE: Sentence of imprisonment or detention in last 10 Years and sentence ended in last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago 1 day. Disposal code {0}.")
    @ValueSource(strings = {
        "1081", "1096", "1114"
    })
    void sentenceOfImprisonmentEligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(
        String disposalCode) throws Exception {
        eligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(disposalCode);
    }

    //Condition 4
    @ParameterizedTest(name =
        "INELIGIBLE: Suspended sentence in last 10 years the sentence was passed in the last 10 years, "
            + "and the sentence is suspended Sentence started 20 years ago. "
            + "Sentence ended exactly 10 years ago. Disposal code {0}.")
    @ValueSource(strings = {
        "1089", "1115", "1134"
    })
    void suspendedSentenceIneligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(
        String disposalCode) throws Exception {
        LocalDate effectiveDate = LocalDate.now(clock).minusYears(20);
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .qualLiteral("S")
                        .sentenceAmount("10")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(effectiveDate))
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }


    @ParameterizedTest(name =
        "INELIGIBLE: Suspended sentence in last 10 years the sentence was passed in the last 10 years, and the "
            + "sentence is suspended"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Sentence ended today. Disposal "
            + "code {0}.")
    @ValueSource(strings = {
        "1089", "1115", "1134"
    })
    void suspendedSentenceIneligible10YearSentenceStarted10YearsAgoAndEndedToday(String disposalCode) throws Exception {
        LocalDate effectiveDate = LocalDate.now(clock).minusYears(10);
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .qualLiteral("S")
                        .sentenceAmount("10")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(effectiveDate))
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Suspended sentence in last 10 years the sentence was passed in the last 10 years, and the "
            + "sentence is suspended"
            + "No Qualify literal. Sentence started 20 years ago. Sentence ended exactly 10 years ago 1 day. Disposal"
            + " code {0}.")
    @ValueSource(strings = {
        "1089", "1115", "1134"
    })
    void noQualifyLiteralEligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(
        String disposalCode) throws Exception {
        LocalDate effectiveDate = LocalDate.now(clock).minusYears(20).minusDays(1);
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .qualLiteral(null)
                        .sentenceAmount("10")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(effectiveDate))
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Suspended sentence in last 10 years the sentence was passed in the last 10 years, and the "
            + "sentence is suspended"
            + "Blank Qualify literal. Sentence started 20 years ago. Sentence ended exactly 10 years ago 1 day. "
            + "Disposal code {0}.")
    @ValueSource(strings = {
        "1089", "1115", "1134"
    })
    void blankQualifyLiteralEligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(
        String disposalCode) throws Exception {
        LocalDate effectiveDate = LocalDate.now(clock).minusYears(20).minusDays(1);
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .qualLiteral("")
                        .sentenceAmount("10")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(effectiveDate))
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Not a Suspended sentence in last 10 years the sentence was passed in the last 10 years, and the "
            + "sentence is suspended"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago 1 day. Disposal code {0}.")
    @ValueSource(strings = {
        "1089", "1115", "1134"
    })
    void notASuspendedSentenceEligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(
        String disposalCode) throws Exception {
        LocalDate effectiveDate = LocalDate.now(clock).minusYears(20).minusDays(1);
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .qualLiteral("F")
                        .sentenceAmount("10")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(effectiveDate))
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    @ParameterizedTest(name =
        "ELIGIBLE: Suspended sentence in last 10 years the sentence was passed in the last 10 years,"
            + " and the sentence is suspended. Is Suspended a and ends in last 10 years. "
            + "Suspended sentence ended 10 years one day ago. Disposal code {0}")
    @ValueSource(strings = {
        "1089", "1115", "1134"
    })
    void suspendedSentenceStarted20YearsOneDayAgoIsSuspended(String disposalCode) throws Exception {
        LocalDate effectiveDate = LocalDate.now(clock).minusYears(20).minusDays(1);
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .qualLiteral("S")
                        .sentenceAmount("10")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(effectiveDate))
                )),
            PoliceNationalComputerCheckResult.Status.ELIGIBLE
        );
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Suspended sentence in last 10 years the sentence was passed in the last 10 years,"
            + " and the sentence is suspended. Is Suspended a and ends in last 10 years. "
            + "Not a Suspended sentence ended 10 years one day ago. Disposal code {0}")
    @ValueSource(strings = {
        "1089", "1115", "1134"
    })
    void notSuspendedSentenceStarted20YearsOneDayAgoIsSuspended(String disposalCode) throws Exception {
        LocalDate effectiveDate = LocalDate.now(clock).minusYears(20).minusDays(1);
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .qualLiteral("F")
                        .sentenceAmount("10")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(effectiveDate))
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }
    //Condition 5

    @ParameterizedTest(name =
        "INELIGIBLE: Community Rehabilitation Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Disposal code {0}.")
    @ValueSource(strings = {
        "1098"
    })
    void communityRehabilitationOrderIneligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(disposalCode);
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Community Rehabilitation Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Sentence ended today. Disposal "
            + "code {0}.")
    @ValueSource(strings = {
        "1098"
    })
    void communityRehabilitationOrderIneligible10YearSentenceStarted10YearsAgoAndEndedToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted10YearsAgoAndEndedToday(disposalCode);
    }

    @ParameterizedTest(name =
        "ELIGIBLE: Community Rehabilitation Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago 1 day. Disposal code {0}.")
    @ValueSource(strings = {
        "1098"
    })
    void communityRehabilitationOrderEligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(
        String disposalCode) throws Exception {
        eligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(disposalCode);
    }

    //Condition 6
    @ParameterizedTest(name =
        "INELIGIBLE: Community Punishment Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Disposal code {0}.")
    @ValueSource(strings = {
        "1099"
    })
    void communityPunishmentOrderIneligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(disposalCode);
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Community Punishment Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Sentence ended today. Disposal "
            + "code {0}.")
    @ValueSource(strings = {
        "1099"
    })
    void communityPunishmentOrderIneligible10YearSentenceStarted10YearsAgoAndEndedToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted10YearsAgoAndEndedToday(disposalCode);
    }

    @ParameterizedTest(name =
        "ELIGIBLE: Community Punishment Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago 1 day. Disposal code {0}.")
    @ValueSource(strings = {
        "1099"
    })
    void communityPunishmentOrderEligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(
        String disposalCode) throws Exception {
        eligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(disposalCode);
    }

    //Condition 7
    @ParameterizedTest(name =
        "INELIGIBLE: Community Punishment & Rehabilitation Order in last 10 years and the sentence passed in the last"
            + " 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Disposal code {0}.")
    @ValueSource(strings = {
        "1102"
    })
    void communityPunishmentAndRehabilitationOrderIneligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(disposalCode);
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Community Punishment & Rehabilitation Order in last 10 years and the sentence passed in the last"
            + " 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Sentence ended today. Disposal "
            + "code {0}.")
    @ValueSource(strings = {
        "1102"
    })
    void communityPunishmentAndRehabilitationOrderIneligible10YearSentenceStarted10YearsAgoAndEndedToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted10YearsAgoAndEndedToday(disposalCode);
    }

    @ParameterizedTest(name =
        "ELIGIBLE: Community Punishment & Rehabilitation Order in last 10 years and the sentence passed in the last "
            + "10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago 1 day. Disposal code {0}.")
    @ValueSource(strings = {
        "1102"
    })
    void communityPunishmentAndRehabilitationOrderEligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(
        String disposalCode) throws Exception {
        eligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(disposalCode);
    }

    //Condition 8
    @ParameterizedTest(name =
        "INELIGIBLE: Drug Treatment and Testing Order in last 10 years and the sentence passed in last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Disposal code {0}.")
    @ValueSource(strings = {
        "1086"
    })
    void drugTreatmentAndTestingOrderIneligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(disposalCode);
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Drug Treatment and Testing Order in last 10 years and the sentence passed in last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Sentence ended today. Disposal "
            + "code {0}.")
    @ValueSource(strings = {
        "1086"
    })
    void drugTreatmentAndTestingOrderIneligible10YearSentenceStarted10YearsAgoAndEndedToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted10YearsAgoAndEndedToday(disposalCode);
    }

    @ParameterizedTest(name =
        "ELIGIBLE: Drug Treatment and Testing Order in last 10 years and the sentence passed in last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago 1 day. Disposal code {0}.")
    @ValueSource(strings = {
        "1086"
    })
    void drugTreatmentAndTestingOrderEligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(
        String disposalCode) throws Exception {
        eligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(disposalCode);
    }

    //Condition 9
    @ParameterizedTest(name =
        "INELIGIBLE: Drug Abstinence Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Disposal code {0}.")
    @ValueSource(strings = {
        "1101"
    })
    void drugAbstinenceOrderIneligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(disposalCode);
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Drug Abstinence Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Sentence ended today. Disposal "
            + "code {0}.")
    @ValueSource(strings = {
        "1101"
    })
    void drugAbstinenceOrderIneligible10YearSentenceStarted10YearsAgoAndEndedToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted10YearsAgoAndEndedToday(disposalCode);
    }

    @ParameterizedTest(name =
        "ELIGIBLE: Drug Abstinence Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago 1 day. Disposal code {0}.")
    @ValueSource(strings = {
        "1101"
    })
    void drugAbstinenceOrderEligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(
        String disposalCode) throws Exception {
        eligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(disposalCode);
    }

    //Condition 10
    @ParameterizedTest(name =
        "INELIGIBLE: Community Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Disposal code {0}.")
    @ValueSource(strings = {
        "1116"
    })
    void communityOrderIneligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(disposalCode);
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Community Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Sentence ended today. Disposal "
            + "code {0}.")
    @ValueSource(strings = {
        "1116"
    })
    void communityOrderIneligible10YearSentenceStarted10YearsAgoAndEndedToday(String disposalCode) throws Exception {
        ineligible10YearSentenceStarted10YearsAgoAndEndedToday(disposalCode);
    }

    @ParameterizedTest(name =
        "ELIGIBLE: Community Order in last 10 years and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago 1 day. Disposal code {0}.")
    @ValueSource(strings = {
        "1116"
    })
    void communityOrderEligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(
        String disposalCode) throws Exception {
        eligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(disposalCode);
    }

    //Condition 11
    @ParameterizedTest(name =
        "INELIGIBLE: Curfew order and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Disposal code {0}.")
    @ValueSource(strings = {
        "1052"
    })
    void curfewOrderIneligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(
        String disposalCode) throws Exception {
        ineligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(disposalCode);
    }

    @ParameterizedTest(name =
        "INELIGIBLE: Curfew order and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago. Sentence ended today. Disposal "
            + "code {0}.")
    @ValueSource(strings = {
        "1052"
    })
    void curfewOrderIneligible10YearSentenceStarted10YearsAgoAndEndedToday(String disposalCode) throws Exception {
        ineligible10YearSentenceStarted10YearsAgoAndEndedToday(disposalCode);
    }

    @ParameterizedTest(name =
        "ELIGIBLE: Curfew order and the sentence passed in the last 10 years"
            + "Sentence started 20 years ago. Sentence ended exactly 10 years ago 1 day. Disposal code {0}.")
    @ValueSource(strings = {
        "1052"
    })
    void curfewOrderEligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(String disposalCode) throws Exception {
        eligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(disposalCode);
    }


    //Support
    void ineligible10YearSentenceStarted20YearsAgoAndEnded10YearsAgoToday(String disposalCode) throws Exception {
        LocalDate effectiveDate = LocalDate.now(clock).minusYears(20);
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .sentenceAmount("10")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(effectiveDate))
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    void ineligible10YearSentenceStarted10YearsAgoAndEndedToday(String disposalCode) throws Exception {
        LocalDate effectiveDate = LocalDate.now(clock).minusYears(10);
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .sentenceAmount("10")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(effectiveDate))
                )),
            PoliceNationalComputerCheckResult.Status.INELIGIBLE
        );
    }

    void eligible10YearSentenceStarted10Years1DayAgoAndEndedYesterday(String disposalCode) throws Exception {
        LocalDate effectiveDate = LocalDate.now(clock).minusYears(20).minusDays(1);
        JurorCheckRequest jurorCheckRequest = getTypicalJurorCheckRequest();
        performValidSingle(
            jurorCheckRequest,
            createGetPersonDetailsResponse(jurorCheckRequest,
                "", false, List.of(
                    createDisposal(DisposalDto.builder()
                        .disposalCode(disposalCode)
                        .sentenceAmount("10")
                        .sentencePeriod("Y")
                        .build()
                        .disposalEffectiveDate(effectiveDate))
                )),
            PoliceNationalComputerCheckResult.Status.ELIGIBLE
        );
    }
}
