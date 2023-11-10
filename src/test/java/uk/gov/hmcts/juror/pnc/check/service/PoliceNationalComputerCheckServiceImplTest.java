package uk.gov.hmcts.juror.pnc.check.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.hmcts.juror.pnc.check.client.contracts.JobExecutionServiceClient;
import uk.gov.hmcts.juror.pnc.check.client.contracts.JurorServiceClient;
import uk.gov.hmcts.juror.pnc.check.client.contracts.PoliceNationalComputerClient;
import uk.gov.hmcts.juror.pnc.check.mapper.GetPersonDetailsMapper;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckBatch;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;
import uk.gov.hmcts.juror.pnc.check.model.PoliceNationalComputerCheckResult;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDetailsDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDto;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSets;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleValidationResult;
import uk.gov.hmcts.juror.pnc.check.service.contracts.RuleService;
import uk.gov.hmcts.juror.pnc.check.testsupport.TestConstants;
import uk.gov.hmcts.juror.standard.service.exceptions.RemoteGatewayException;
import uk.police.npia.juror.schema.v1.GetPersonDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("PoliceNationalComputerCheckServiceImpl")
@SuppressWarnings({
    "unchecked",
    "PMD.LawOfDemeter",
    "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessiveImports"
})
class PoliceNationalComputerCheckServiceImplTest {

    private PoliceNationalComputerClient policeNationalComputerClient;
    private JobExecutionServiceClient jobExecutionServiceClient;
    private JurorServiceClient jurorServiceClient;
    private GetPersonDetailsMapper getPersonDetailsMapper;
    private RuleService ruleService;
    private PoliceNationalComputerCheckServiceImpl policeNationalComputerCheckService;


    @BeforeEach
    void beforeEach() {
        this.policeNationalComputerClient = mock(PoliceNationalComputerClient.class);
        this.jobExecutionServiceClient = mock(JobExecutionServiceClient.class);
        this.jurorServiceClient = mock(JurorServiceClient.class);
        this.getPersonDetailsMapper = mock(GetPersonDetailsMapper.class);
        this.ruleService = mock(RuleService.class);
        this.policeNationalComputerCheckService = spy(new PoliceNationalComputerCheckServiceImpl(
            policeNationalComputerClient,
            jobExecutionServiceClient,
            jurorServiceClient,
            getPersonDetailsMapper,
            ruleService
        ));
    }


    @Nested
    @DisplayName("Optional<PoliceNationalComputerCheckResult> checkErrorReason("
        + "final String jurorNumber, final PersonDetailsDto personDetailsDto)")
    class CheckErrorCode {
        @Test
        @DisplayName("Error Reason not found")
        void negativeCheckErrorReasonNoErrorReason() {

            Optional<PoliceNationalComputerCheckResult> resultOptional =
                policeNationalComputerCheckService.checkErrorReason(
                    TestConstants.JUROR_NUMBER,
                    PersonDetailsDto.builder().errorReason(null).build()
                );
            assertTrue(resultOptional.isPresent(), "Result should be present");
            PoliceNationalComputerCheckResult result = resultOptional.get();

            assertEquals(PoliceNationalComputerCheckResult.Status.ERROR_RETRY_NO_ERROR_REASON,
                result.getStatus(),
                "Status should be ERROR_RETRY_NO_ERROR_REASON");
            assertEquals("No data returned for juror. Unable to check",
                result.getMessage(),
                "Message should be correct");
        }


        @Test
        @DisplayName("Error Reason is No Records found (JUR001)")
        void positiveCheckErrorReasonIsNoRecordsFound() {
            Optional<PoliceNationalComputerCheckResult> resultOptional =
                policeNationalComputerCheckService.checkErrorReason(
                    TestConstants.JUROR_NUMBER,
                    PersonDetailsDto.builder()
                        .errorReason("JUR001 - No Records Found:").build()
                );
            assertFalse(resultOptional.isPresent(),
                "Result should not be returned if no records are found for juror");
        }

        @Test
        @DisplayName("Error Reason is not Records found (JUR001)")
        void negativeCheckErrorReasonIsNotNoRecordsFound() {
            final String errorReason = "Some other error reason";
            Optional<PoliceNationalComputerCheckResult> resultOptional =
                policeNationalComputerCheckService.checkErrorReason(
                    TestConstants.JUROR_NUMBER,
                    PersonDetailsDto.builder().errorReason(errorReason).build()
                );
            assertTrue(resultOptional.isPresent(), "Result should be present");
            PoliceNationalComputerCheckResult result = resultOptional.get();

            assertEquals(PoliceNationalComputerCheckResult.Status.ERROR_RETRY_OTHER_ERROR_CODE,
                result.getStatus(),
                "Status should be ERROR_RETRY_OTHER_ERROR_CODE");

            assertEquals("Returned error code: " + errorReason + " for juror",
                result.getMessage(),
                "Message should be correct");
        }

        @Test
        @DisplayName("Error Reason is blank")
        void positiveCheckErrorReasonIsBlank() {
            Optional<PoliceNationalComputerCheckResult> resultOptional =
                policeNationalComputerCheckService.checkErrorReason(
                    TestConstants.JUROR_NUMBER,
                    PersonDetailsDto.builder().errorReason("").build()
                );
            assertFalse(resultOptional.isPresent(),
                "Result should not be returned if a blank error reason is found");
        }
        //Error reason is blank
    }


    @Nested
    @DisplayName("PoliceNationalComputerCheckResult validatePoliceCheckResponse("
        + "String jurorNumber, PersonDetailsDto personDetails)")
    class ValidatePoliceCheckResponse {

        @Test
        @DisplayName("checkErrorReason returned result")
        void negativeCheckErrorReasonHasResult() {
            PersonDetailsDto personDetailsDto = PersonDetailsDto.builder().build();
            PoliceNationalComputerCheckResult expectedResult = new PoliceNationalComputerCheckResult(
                PoliceNationalComputerCheckResult.Status.INELIGIBLE, "Dummy message");

            when(policeNationalComputerCheckService.checkErrorReason(TestConstants.JUROR_NUMBER, personDetailsDto))
                .thenReturn(Optional.of(expectedResult));

            PoliceNationalComputerCheckResult result = policeNationalComputerCheckService
                .validatePoliceCheckResponse(TestConstants.JUROR_NUMBER, personDetailsDto);

            assertEquals(expectedResult, result, "Results should match");
        }

        @Test
        void negativePersonRulesCheckFailed() {
            PersonDto person = PersonDto.builder().build();
            PersonDetailsDto personDetailsDto = PersonDetailsDto.builder()
                .personDtos(List.of(
                    person
                ))
                .build();
            when(policeNationalComputerCheckService.checkErrorReason(TestConstants.JUROR_NUMBER, personDetailsDto))
                .thenReturn(Optional.empty());
            when(ruleService.fireRules(RuleSets.PERSON_RULE_SET, person))
                .thenReturn(new RuleValidationResult(false, "My Error Message"));


            PoliceNationalComputerCheckResult result = policeNationalComputerCheckService
                .validatePoliceCheckResponse(TestConstants.JUROR_NUMBER, personDetailsDto);

            assertEquals(PoliceNationalComputerCheckResult.Status.INELIGIBLE,
                result.getStatus(),
                "Status should be FAILED");
            assertEquals("My Error Message",
                result.getMessage(),
                "Message should be correct");
        }

        @Test
        @DisplayName("Disposal rules check failed")
        void negativeDisposalRulesCheckFailed() {
            List<DisposalDto> disposals = List.of(
                DisposalDto.builder().disposalCode("1").build(),
                DisposalDto.builder().disposalCode("2").build(),
                DisposalDto.builder().disposalCode("3").build()
            );
            PersonDto person = PersonDto.builder().build();
            person.setDisposals(disposals);
            PersonDetailsDto personDetailsDto = PersonDetailsDto.builder()
                .personDtos(List.of(
                    person
                ))
                .build();
            when(policeNationalComputerCheckService.checkErrorReason(TestConstants.JUROR_NUMBER, personDetailsDto))
                .thenReturn(Optional.empty());
            when(ruleService.fireRules(RuleSets.PERSON_RULE_SET, person))
                .thenReturn(RuleValidationResult.passed());


            when(ruleService.fireRules(eq(RuleSets.DISPOSAL_RULE_SET), anySet()))
                .thenReturn(new RuleValidationResult(false, "My Error disposals Message"));

            PoliceNationalComputerCheckResult result = policeNationalComputerCheckService
                .validatePoliceCheckResponse(TestConstants.JUROR_NUMBER, personDetailsDto);

            assertEquals(PoliceNationalComputerCheckResult.Status.INELIGIBLE,
                result.getStatus(),
                "Status should be FAILED");
            assertEquals("My Error disposals Message",
                result.getMessage(),
                "Message should be correct");

            ArgumentCaptor<Collection<DisposalDto>> disposalCaptor = ArgumentCaptor.forClass(Collection.class);

            verify(ruleService, times(1))
                .fireRules(eq(RuleSets.DISPOSAL_RULE_SET), disposalCaptor.capture());

            Collection<DisposalDto> disposalDtos = disposalCaptor.getValue();

            assertEquals(disposals.size(), disposalDtos.size(),
                "Disposals given to rule should be same as size provided");
            assertThat("Disposals should match", disposalDtos, hasItems(disposals.toArray(new DisposalDto[0])));

        }

        @Test
        @DisplayName("Blank Disposal codes removed from rule check")
        void positiveBlankDisposalCodeRemoved() {
            List<DisposalDto> disposals = List.of(
                DisposalDto.builder().disposalCode("1").build(),
                DisposalDto.builder().disposalCode("").build(),
                DisposalDto.builder().disposalCode(null).build()
            );
            PersonDto person = PersonDto.builder().build();
            person.setDisposals(disposals);
            PersonDetailsDto personDetailsDto = PersonDetailsDto.builder()
                .personDtos(List.of(person)).build();
            when(policeNationalComputerCheckService.checkErrorReason(TestConstants.JUROR_NUMBER, personDetailsDto))
                .thenReturn(Optional.empty());
            when(ruleService.fireRules(RuleSets.PERSON_RULE_SET, person))
                .thenReturn(RuleValidationResult.passed());


            when(ruleService.fireRules(eq(RuleSets.DISPOSAL_RULE_SET), anySet()))
                .thenReturn(new RuleValidationResult(false, "My Error disposals Message"));

            PoliceNationalComputerCheckResult result = policeNationalComputerCheckService
                .validatePoliceCheckResponse(TestConstants.JUROR_NUMBER, personDetailsDto);

            assertEquals(PoliceNationalComputerCheckResult.Status.INELIGIBLE,
                result.getStatus(),
                "Status should be FAILED");
            assertEquals("My Error disposals Message",
                result.getMessage(),
                "Message should be correct");

            ArgumentCaptor<Collection<DisposalDto>> disposalCaptor = ArgumentCaptor.forClass(Collection.class);

            verify(ruleService, times(1))
                .fireRules(eq(RuleSets.DISPOSAL_RULE_SET), disposalCaptor.capture());

            Collection<DisposalDto> disposalDtos = disposalCaptor.getValue();

            assertEquals(1, disposalDtos.size(),
                "Blank disposals should have been removed");
            assertEquals(disposals.get(0), disposalDtos.toArray()[0],
                "Only none bank disposal should be provided");
        }

        @Test
        @DisplayName("All checks passed")
        void positiveCheckPassed() {
            PersonDto person = PersonDto.builder().build();
            person.setDisposals(List.of(DisposalDto.builder().disposalCode("1").build()));
            PersonDetailsDto personDetailsDto = PersonDetailsDto.builder()
                .personDtos(List.of(person)).build();

            when(policeNationalComputerCheckService.checkErrorReason(TestConstants.JUROR_NUMBER, personDetailsDto))
                .thenReturn(Optional.empty());
            when(ruleService.fireRules(RuleSets.PERSON_RULE_SET, person))
                .thenReturn(RuleValidationResult.passed());

            when(ruleService.fireRules(eq(RuleSets.DISPOSAL_RULE_SET), anySet()))
                .thenReturn(RuleValidationResult.passed());


            PoliceNationalComputerCheckResult result = policeNationalComputerCheckService
                .validatePoliceCheckResponse(TestConstants.JUROR_NUMBER, personDetailsDto);

            assertEquals(PoliceNationalComputerCheckResult.Status.ELIGIBLE,
                result.getStatus(),
                "Status should be PASSED");
            assertNull(
                result.getMessage(),
                "No message returned when result is passed");
        }
    }


    @Nested
    @DisplayName("public void reportResults(JurorCheckBatch jurorCheckBatch)")
    class ReportResults {

        @Test
        @DisplayName("MetaData is null")
        void negativeNoMetaData() {
            policeNationalComputerCheckService
                .reportResults(new JurorCheckBatch(null, Collections.emptyList()));
            verifyNoInteractions(jobExecutionServiceClient);
        }

        @Test
        @DisplayName("JobKey is null")
        void negativeNoJobKey() {
            policeNationalComputerCheckService
                .reportResults(new JurorCheckBatch(
                    JurorCheckBatch.MetaData.builder().jobKey(null).taskId(TestConstants.TASK_ID).build(),
                    Collections.emptyList()));
            verifyNoInteractions(jobExecutionServiceClient);
        }

        @Test
        @DisplayName("TaskId is null")
        void negativeNoTaskId() {
            policeNationalComputerCheckService
                .reportResults(new JurorCheckBatch(
                    JurorCheckBatch.MetaData.builder().jobKey(TestConstants.JOB_KEY).taskId(null).build(),
                    Collections.emptyList()));
            verifyNoInteractions(jobExecutionServiceClient);

        }

        @Test
        @DisplayName("Result is null")
        void negativeResultIsNull() {
            policeNationalComputerCheckService
                .reportResults(new JurorCheckBatch(
                    JurorCheckBatch.MetaData.builder().jobKey(TestConstants.JOB_KEY).taskId(TestConstants.TASK_ID)
                        .build(),
                    Set.of(JurorCheckDetails.builder()
                        .jurorNumber(TestConstants.JUROR_NUMBER)
                        .result(null).build())));

            ArgumentCaptor<JobExecutionServiceClient.StatusUpdatePayload> captor =
                ArgumentCaptor.forClass(JobExecutionServiceClient.StatusUpdatePayload.class);

            verify(jobExecutionServiceClient, times(1))
                .call(captor.capture(), eq(TestConstants.JOB_KEY), eq(TestConstants.TASK_ID));

            JobExecutionServiceClient.StatusUpdatePayload payload = captor.getValue();

            assertEquals(JobExecutionServiceClient.StatusUpdatePayload.Status.FAILED_UNEXPECTED_EXCEPTION,
                payload.getStatus(),
                "Status should match");
            Map<String, String> metaData = payload.getMetaData();
            assertEquals("1", metaData.get("TOTAL_CHECKS_IN_BATCH"),
                "Meta data entry should match");
            assertEquals("1", metaData.get("TOTAL_NULL_RESULTS"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ELIGIBLE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_INELIGIBLE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_NAME_HAS_NUMERICS"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_CONNECTION_ERROR"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_OTHER_ERROR_CODE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_NO_ERROR_REASON"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_UNEXPECTED_EXCEPTION"),
                "Meta data entry should match");
            assertEquals("Juror check completed.", payload.getMessage(),
                "Message should match");
        }

        @Test
        @DisplayName("Connection error")
        void negativeConnectionError() {
            policeNationalComputerCheckService
                .reportResults(new JurorCheckBatch(
                    JurorCheckBatch.MetaData.builder().jobKey(TestConstants.JOB_KEY).taskId(TestConstants.TASK_ID)
                        .build(),
                    Set.of(
                        JurorCheckDetails.builder()
                            .jurorNumber(TestConstants.JUROR_NUMBER)
                            .result(new PoliceNationalComputerCheckResult(
                                PoliceNationalComputerCheckResult.Status.ERROR_RETRY_CONNECTION_ERROR)).build(),
                        JurorCheckDetails.builder()
                            .jurorNumber(TestConstants.JUROR_NUMBER + "1")
                            .result(PoliceNationalComputerCheckResult.passed())
                            .build())
                ));

            ArgumentCaptor<JobExecutionServiceClient.StatusUpdatePayload> captor =
                ArgumentCaptor.forClass(JobExecutionServiceClient.StatusUpdatePayload.class);

            verify(jobExecutionServiceClient, times(1))
                .call(captor.capture(), eq(TestConstants.JOB_KEY), eq(TestConstants.TASK_ID));

            JobExecutionServiceClient.StatusUpdatePayload payload = captor.getValue();

            assertEquals(JobExecutionServiceClient.StatusUpdatePayload.Status.INDETERMINATE,
                payload.getStatus(),
                "Status should match");
            Map<String, String> metaData = payload.getMetaData();
            assertEquals("2", metaData.get("TOTAL_CHECKS_IN_BATCH"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_NULL_RESULTS"),
                "Meta data entry should match");
            assertEquals("1", metaData.get("TOTAL_WITH_STATUS_ELIGIBLE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_INELIGIBLE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_NAME_HAS_NUMERICS"),
                "Meta data entry should match");
            assertEquals("1", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_CONNECTION_ERROR"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_OTHER_ERROR_CODE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_NO_ERROR_REASON"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_UNEXPECTED_EXCEPTION"),
                "Meta data entry should match");
            assertEquals("Juror check completed.", payload.getMessage(),
                "Message should match");
        }

        @Test
        @DisplayName("Result is Unexpected exception")
        void negativeResultIsUnexpectedException() {
            policeNationalComputerCheckService
                .reportResults(new JurorCheckBatch(
                    JurorCheckBatch.MetaData.builder().jobKey(TestConstants.JOB_KEY).taskId(TestConstants.TASK_ID)
                        .build(),
                    Set.of(JurorCheckDetails.builder()
                        .jurorNumber(TestConstants.JUROR_NUMBER)
                        .result(new PoliceNationalComputerCheckResult(
                            PoliceNationalComputerCheckResult.Status.ERROR_RETRY_UNEXPECTED_EXCEPTION, "MyMessage")
                        ).build())));

            ArgumentCaptor<JobExecutionServiceClient.StatusUpdatePayload> captor =
                ArgumentCaptor.forClass(JobExecutionServiceClient.StatusUpdatePayload.class);

            verify(jobExecutionServiceClient, times(1))
                .call(captor.capture(), eq(TestConstants.JOB_KEY), eq(TestConstants.TASK_ID));

            JobExecutionServiceClient.StatusUpdatePayload payload = captor.getValue();

            assertEquals(JobExecutionServiceClient.StatusUpdatePayload.Status.FAILED_UNEXPECTED_EXCEPTION,
                payload.getStatus(),
                "Status should match");
            Map<String, String> metaData = payload.getMetaData();
            assertEquals("1", metaData.get("TOTAL_CHECKS_IN_BATCH"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_NULL_RESULTS"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ELIGIBLE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_INELIGIBLE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_NAME_HAS_NUMERICS"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_CONNECTION_ERROR"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_OTHER_ERROR_CODE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_NO_ERROR_REASON"),
                "Meta data entry should match");
            assertEquals("1", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_UNEXPECTED_EXCEPTION"),
                "Meta data entry should match");
            assertEquals("Juror check completed.", payload.getMessage(),
                "Message should match");
        }

        @Test
        @DisplayName("Result has multiple failures")
        void negativeResultHasMultipleFailures() {
            policeNationalComputerCheckService
                .reportResults(new JurorCheckBatch(
                    JurorCheckBatch.MetaData.builder().jobKey(TestConstants.JOB_KEY).taskId(TestConstants.TASK_ID)
                        .build(),
                    List.of(
                        JurorCheckDetails.builder().jurorNumber(TestConstants.JUROR_NUMBER)
                            .result(PoliceNationalComputerCheckResult.passed()).build(),

                        JurorCheckDetails.builder().jurorNumber(TestConstants.JUROR_NUMBER)
                            .result(new PoliceNationalComputerCheckResult(
                                PoliceNationalComputerCheckResult.Status.INELIGIBLE, "MyMessage")).build(),

                        JurorCheckDetails.builder().jurorNumber(TestConstants.JUROR_NUMBER)
                            .result(new PoliceNationalComputerCheckResult(
                                PoliceNationalComputerCheckResult.Status.ERROR_RETRY_OTHER_ERROR_CODE, "MyMessage"))
                            .build(),

                        JurorCheckDetails.builder()
                            .jurorNumber(TestConstants.JUROR_NUMBER)
                            .result(null).build(),
                        JurorCheckDetails.builder()
                            .jurorNumber(TestConstants.JUROR_NUMBER + "2")
                            .result(new PoliceNationalComputerCheckResult(
                                PoliceNationalComputerCheckResult.Status.ERROR_RETRY_UNEXPECTED_EXCEPTION, "MyMessage")
                            ).build(),

                        JurorCheckDetails.builder()
                            .jurorNumber(TestConstants.JUROR_NUMBER + "3")
                            .result(new PoliceNationalComputerCheckResult(
                                PoliceNationalComputerCheckResult.Status.ERROR_RETRY_UNEXPECTED_EXCEPTION, "MyMessage2")
                            ).build())));

            ArgumentCaptor<JobExecutionServiceClient.StatusUpdatePayload> captor =
                ArgumentCaptor.forClass(JobExecutionServiceClient.StatusUpdatePayload.class);

            verify(jobExecutionServiceClient, times(1))
                .call(captor.capture(), eq(TestConstants.JOB_KEY), eq(TestConstants.TASK_ID));

            JobExecutionServiceClient.StatusUpdatePayload payload = captor.getValue();

            assertEquals(JobExecutionServiceClient.StatusUpdatePayload.Status.FAILED_UNEXPECTED_EXCEPTION,
                payload.getStatus(), "Status should match");


            Map<String, String> metaData = payload.getMetaData();
            assertEquals("6", metaData.get("TOTAL_CHECKS_IN_BATCH"),
                "Meta data entry should match");
            assertEquals("1", metaData.get("TOTAL_NULL_RESULTS"),
                "Meta data entry should match");
            assertEquals("1", metaData.get("TOTAL_WITH_STATUS_ELIGIBLE"),
                "Meta data entry should match");
            assertEquals("1", metaData.get("TOTAL_WITH_STATUS_INELIGIBLE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_NAME_HAS_NUMERICS"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_CONNECTION_ERROR"),
                "Meta data entry should match");
            assertEquals("1", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_OTHER_ERROR_CODE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_NO_ERROR_REASON"),
                "Meta data entry should match");
            assertEquals("2", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_UNEXPECTED_EXCEPTION"),
                "Meta data entry should match");
            assertEquals("Juror check completed.", payload.getMessage(),
                "Message should match");
        }

        @Test
        @DisplayName("Result has no failures")
        void positiveNoFailures() {
            policeNationalComputerCheckService
                .reportResults(new JurorCheckBatch(
                    JurorCheckBatch.MetaData.builder().jobKey(TestConstants.JOB_KEY).taskId(TestConstants.TASK_ID)
                        .build(),
                    List.of(JurorCheckDetails.builder().jurorNumber(TestConstants.JUROR_NUMBER)
                            .result(PoliceNationalComputerCheckResult.passed()).build(),
                        JurorCheckDetails.builder().jurorNumber(TestConstants.JUROR_NUMBER + "2")
                            .result(PoliceNationalComputerCheckResult.passed()).build())));

            ArgumentCaptor<JobExecutionServiceClient.StatusUpdatePayload> captor =
                ArgumentCaptor.forClass(JobExecutionServiceClient.StatusUpdatePayload.class);

            verify(jobExecutionServiceClient, times(1))
                .call(captor.capture(), eq(TestConstants.JOB_KEY), eq(TestConstants.TASK_ID));

            JobExecutionServiceClient.StatusUpdatePayload payload = captor.getValue();

            assertEquals(JobExecutionServiceClient.StatusUpdatePayload.Status.SUCCESS,
                payload.getStatus(), "Status should match");
            Map<String, String> metaData = payload.getMetaData();
            assertEquals("2", metaData.get("TOTAL_CHECKS_IN_BATCH"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_NULL_RESULTS"),
                "Meta data entry should match");
            assertEquals("2", metaData.get("TOTAL_WITH_STATUS_ELIGIBLE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_INELIGIBLE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_NAME_HAS_NUMERICS"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_CONNECTION_ERROR"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_OTHER_ERROR_CODE"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_NO_ERROR_REASON"),
                "Meta data entry should match");
            assertEquals("0", metaData.get("TOTAL_WITH_STATUS_ERROR_RETRY_UNEXPECTED_EXCEPTION"),
                "Meta data entry should match");
            assertEquals("Juror check completed.", payload.getMessage(),
                "Message should match");
        }

    }


    @Nested
    @DisplayName("public PoliceNationalComputerCheckResult performPoliceCheck"
        + "(final JurorCheckDetails jurorCheckDetails)")
    class PerformPoliceCheck {

        @Test
        @DisplayName("Person name contains numerics")
        void negativePersonNameContainsNumerics() {
            JurorCheckDetails jurorCheckDetails = JurorCheckDetails.builder()
                .jurorNumber(TestConstants.JUROR_NUMBER)
                .build();
            GetPersonDetails getPersonDetails = new GetPersonDetails();
            getPersonDetails.setSearchName("Ben10");

            when(getPersonDetailsMapper.mapJurorCheckRequestToGetPersonDetails(jurorCheckDetails))
                .thenReturn(getPersonDetails);


            PoliceNationalComputerCheckResult result =
                policeNationalComputerCheckService.performPoliceCheck(jurorCheckDetails);

            verify(getPersonDetailsMapper, times(1))
                .mapJurorCheckRequestToGetPersonDetails(jurorCheckDetails);

            assertEquals(PoliceNationalComputerCheckResult.Status.ERROR_RETRY_NAME_HAS_NUMERICS,
                result.getStatus(),
                "Status should be FAILED_NAME_HAS_NUMERICS");
            assertNull(
                result.getMessage(),
                "Message should be null when name has numerics");

            verify(jurorServiceClient, times(1)).call(
                jurorCheckDetails.getJurorNumber(),
                new JurorServiceClient.Payload(PoliceNationalComputerCheckResult.Status.ERROR_RETRY_NAME_HAS_NUMERICS)
            );
        }

        @Test
        @DisplayName("Unexpected exception")
        void negativeUnexpectedException() {
            JurorCheckDetails jurorCheckDetails = JurorCheckDetails.builder()
                .jurorNumber(TestConstants.JUROR_NUMBER)
                .build();

            RuntimeException cause = new RuntimeException("Some Exception");
            when(getPersonDetailsMapper.mapJurorCheckRequestToGetPersonDetails(jurorCheckDetails))
                .thenThrow(cause);


            PoliceNationalComputerCheckResult result =
                policeNationalComputerCheckService.performPoliceCheck(jurorCheckDetails);


            assertEquals(PoliceNationalComputerCheckResult.Status.ERROR_RETRY_UNEXPECTED_EXCEPTION,
                result.getStatus(),
                "Status should be FAILED_UNEXPECTED_EXCEPTION");
            assertNull(
                result.getMessage(),
                "Message should be null if exception is unexpected");

            verify(jurorServiceClient, times(1)).call(
                jurorCheckDetails.getJurorNumber(),
                new JurorServiceClient.Payload(
                    PoliceNationalComputerCheckResult.Status.ERROR_RETRY_UNEXPECTED_EXCEPTION)
            );
        }

        @Test
        @DisplayName("RemoteGateway exception")
        void negativeRemoteGatewayException() {
            JurorCheckDetails jurorCheckDetails = JurorCheckDetails.builder()
                .jurorNumber(TestConstants.JUROR_NUMBER)
                .build();

            RemoteGatewayException cause = new RemoteGatewayException("Some Exception");
            when(getPersonDetailsMapper.mapJurorCheckRequestToGetPersonDetails(jurorCheckDetails))
                .thenThrow(cause);


            PoliceNationalComputerCheckResult result =
                policeNationalComputerCheckService.performPoliceCheck(jurorCheckDetails);


            assertEquals(PoliceNationalComputerCheckResult.Status.ERROR_RETRY_CONNECTION_ERROR,
                result.getStatus(),
                "Status should be ERROR_RETRY_CONNECTION_ERROR");
            assertNull(
                result.getMessage(),
                "Message should be null if exception is unexpected");

            verify(jurorServiceClient, times(1)).call(
                jurorCheckDetails.getJurorNumber(),
                new JurorServiceClient.Payload(PoliceNationalComputerCheckResult.Status.ERROR_RETRY_CONNECTION_ERROR)
            );
        }

        @Test
        @DisplayName("Valid request")
        void positiveTypicalResponse() {
            JurorCheckDetails jurorCheckDetails = JurorCheckDetails.builder()
                .jurorNumber(TestConstants.JUROR_NUMBER)
                .build();
            GetPersonDetails getPersonDetails = new GetPersonDetails();
            getPersonDetails.setSearchName("Edwards/Ben");

            when(getPersonDetailsMapper.mapJurorCheckRequestToGetPersonDetails(jurorCheckDetails))
                .thenReturn(getPersonDetails);

            PersonDetailsDto personDetailsDto = new PersonDetailsDto();

            when(policeNationalComputerClient.call(getPersonDetails)).thenReturn(personDetailsDto);

            PoliceNationalComputerCheckResult expectedResponse = new PoliceNationalComputerCheckResult(
                PoliceNationalComputerCheckResult.Status.ELIGIBLE, "My Message");

            when(policeNationalComputerCheckService.validatePoliceCheckResponse(TestConstants.JUROR_NUMBER,
                personDetailsDto)).thenReturn(expectedResponse);

            PoliceNationalComputerCheckResult result =
                policeNationalComputerCheckService.performPoliceCheck(jurorCheckDetails);
            assertEquals(expectedResponse, result,
                "Return result should match that from validatePoliceCheckResponse");

            verify(jurorServiceClient, times(1)).call(
                jurorCheckDetails.getJurorNumber(),
                new JurorServiceClient.Payload(PoliceNationalComputerCheckResult.Status.ELIGIBLE)
            );
        }
    }
}
